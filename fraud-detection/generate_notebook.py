import os
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns

from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    average_precision_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
)
from sklearn.model_selection import StratifiedKFold, cross_val_score, train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

import nbformat
from nbclient import NotebookClient

base_dir = Path(__file__).resolve().parent
os.chdir(base_dir)
csv_path = base_dir / "bank_transactions.csv"
if not csv_path.exists():
    rng = np.random.default_rng(42)
    n = 5000
    countries = ["FR", "DE", "BE", "ES", "IT"]
    merchant_categories = ["GROCERY", "TRANSPORT", "TRAVEL", "ECOMMERCE", "RESTAURANT"]
    df = pd.DataFrame(
        {
            "transaction_id": [f"TX{idx:06d}" for idx in range(1, n + 1)],
            "account_id": [f"ACC{rng.integers(1000, 9999):04d}" for _ in range(n)],
            "amount": np.round(rng.lognormal(mean=3.5, sigma=0.9, size=n), 2),
            "hour": rng.integers(0, 24, size=n),
            "country": rng.choice(countries, size=n),
            "merchant_category": rng.choice(merchant_categories, size=n),
            "transactions_last_24h": rng.integers(0, 25, size=n),
            "average_amount_last_7d": np.round(rng.lognormal(mean=3.3, sigma=0.7, size=n), 2),
        }
    )
    df["is_fraud"] = np.where(
        (df["amount"] > 700) & (df["hour"].between(0, 5)) & (df["country"].isin(["FR", "DE"])),
        1,
        0,
    )
    df["is_fraud"] = np.where(rng.random(n) < 0.015, 1, df["is_fraud"])
    df["is_fraud"] = np.clip(df["is_fraud"], 0, 1)
    df.to_csv(csv_path, index=False)

# Notebook skeleton
nb = nbformat.v4.new_notebook()

cells = [
    ("markdown", "## 1. Chargement des données\n\nOn charge le fichier CSV et on vérifie rapidement les colonnes et les premières lignes. Cela permet de confirmer le schéma des données avant toute modélisation."),
    ("code", "from pathlib import Path\nimport pandas as pd\n\npath = Path(\"bank_transactions.csv\")\ndf = pd.read_csv(path)\nprint(f\"Forme du dataset : {df.shape}\")\nprint(df.head())\nprint(\"\\nColonnes :\", list(df.columns))\n"),
    ("markdown", "## 2. Proportion des fraudes et valeurs manquantes\n\nNous mesurons d'abord la rareté de la fraude et la qualité des colonnes, car les jeux de données de fraude sont généralement très déséquilibrés."),
    ("code", "fraud_rate = df[\"is_fraud\"].mean()\nmissing = df.isna().sum().sort_values(ascending=False)\nprint(f\"Proportion de fraudes : {fraud_rate:.2%}\")\nprint(\"\\nValeurs manquantes par colonne :\")\nprint(missing[missing > 0].to_string())\nif (missing == 0).all():\n    print(\"\\nAucune valeur manquante détectée.\")\n"),
    ("markdown", "## 3. Séparation train/test stratifiée avant tout prétraitement\n\nLe découpage est fait avant le prétraitement pour éviter toute fuite d'information provenant du jeu de test et pour préserver la proportion de fraude dans chaque sous-ensemble."),
    ("code", "from sklearn.model_selection import train_test_split\n\nX = df.drop(columns=[\"is_fraud\"])\ny = df[\"is_fraud\"]\n\nX_train, X_test, y_train, y_test = train_test_split(\n    X, y,\n    test_size=0.2,\n    random_state=42,\n    stratify=y\n)\n\nprint(f\"Train shape : {X_train.shape}\")\nprint(f\"Test shape : {X_test.shape}\")\nprint(f\"Fraude train : {y_train.mean():.2%}\")\nprint(f\"Fraude test : {y_test.mean():.2%}\")\n"),
    ("markdown", "## 4. Suppression des identifiants non utiles\n\nLes identifiants de transaction et de compte ne sont pas des variables explicatives pertinentes pour détecter la fraude ; on les retire avant la construction des features."),
    ("code", "drop_cols = [\"transaction_id\", \"account_id\"]\nX_train = X_train.drop(columns=drop_cols)\nX_test = X_test.drop(columns=drop_cols)\nprint(X_train.head())\nprint(\"\\nColonnes finales :\", list(X_train.columns))\n"),
    ("markdown", "## 5. Pipeline de prétraitement scikit-learn\n\nLe pipeline applique un prétraitement adapté à chaque type de variable : médiane + standardisation pour les numériques, mode + one-hot encoding pour les catégorielles, en ajustant uniquement sur le train."),
    ("code", "from sklearn.compose import ColumnTransformer\nfrom sklearn.impute import SimpleImputer\nfrom sklearn.linear_model import LogisticRegression\nfrom sklearn.pipeline import Pipeline\nfrom sklearn.preprocessing import OneHotEncoder, StandardScaler\n\nnum_cols = X_train.select_dtypes(include=[\"number\"]).columns.tolist()\ncat_cols = X_train.select_dtypes(exclude=[\"number\"]).columns.tolist()\n\nnum_transformer = Pipeline(steps=[\n    (\"imputer\", SimpleImputer(strategy=\"median\")),\n    (\"scaler\", StandardScaler())\n])\n\ncat_transformer = Pipeline(steps=[\n    (\"imputer\", SimpleImputer(strategy=\"most_frequent\")),\n    (\"onehot\", OneHotEncoder(handle_unknown=\"ignore\"))\n])\n\npreprocessor = ColumnTransformer(\n    transformers=[\n        (\"numeric\", num_transformer, num_cols),\n        (\"categorical\", cat_transformer, cat_cols),\n    ]\n)\n\nbase_model = LogisticRegression(class_weight=\"balanced\", max_iter=1000)\nmodel_pipeline = Pipeline(steps=[(\"preprocessor\", preprocessor), (\"model\", base_model)])\nmodel_pipeline.fit(X_train, y_train)\n\nX_train_processed = model_pipeline.named_steps[\"preprocessor\"].transform(X_train)\nprint(f\"Forme après prétraitement du train : {X_train_processed.shape}\")\nprint(f\"Nombre de variables transformées : {X_train_processed.shape[1]}\")\n"),
    ("markdown", "## 6. Modèle et comparaison sur le train en validation croisée\n\nOn compare rapidement un modèle linéaire et un arbre aléatoire sur le train avec validation croisée stratifiée et score average_precision, puis on garde le meilleur modèle."),
    ("code", "from sklearn.ensemble import RandomForestClassifier\nfrom sklearn.linear_model import LogisticRegression\nfrom sklearn.model_selection import StratifiedKFold, cross_val_score\n\ncv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)\nmodels = {\n    \"LogisticRegression\": LogisticRegression(class_weight=\"balanced\", max_iter=1000),\n    \"RandomForest\": RandomForestClassifier(class_weight=\"balanced\", random_state=42)\n}\n\nresults = {}\nfor name, model in models.items():\n    clf = Pipeline([\n        (\"preprocessor\", preprocessor),\n        (\"model\", model),\n    ])\n    scores = cross_val_score(clf, X_train, y_train, cv=cv, scoring=\"average_precision\")\n    results[name] = scores.mean()\n    print(f\"{name} : average_precision moyen = {scores.mean():.4f} (écart-type = {scores.std():.4f})\")\n\nbest_model_name = max(results, key=results.get)\nbest_model = models[best_model_name]\nprint(f\"\\nMeilleur modèle retenu : {best_model_name}\")\n\nbest_pipeline = Pipeline([\n    (\"preprocessor\", preprocessor),\n    (\"model\", best_model),\n])\nbest_pipeline.fit(X_train, y_train)\n"),
    ("markdown", "## 7. Évaluation finale sur le jeu de test\n\nOn évalue une seule fois sur le test, sans recalcul de seuil sur ce jeu, en gardant le seuil par défaut de 0.5 pour un benchmark honnête et reproductible."),
    ("code", "import matplotlib.pyplot as plt\nimport seaborn as sns\nfrom sklearn.metrics import average_precision_score, classification_report, confusion_matrix, f1_score, precision_score, recall_score\n\nfinal_pred = best_pipeline.predict(X_test)\nfinal_proba = best_pipeline.predict_proba(X_test)[:, 1]\n\nprecision = precision_score(y_test, final_pred, pos_label=1)\nrecall = recall_score(y_test, final_pred, pos_label=1)\nf1 = f1_score(y_test, final_pred, pos_label=1)\nap = average_precision_score(y_test, final_proba)\ncm = confusion_matrix(y_test, final_pred, labels=[0, 1])\n\nprint(f\"Précision (classe 1) : {precision:.4f}\")\nprint(f\"Rappel (classe 1) : {recall:.4f}\")\nprint(f\"F1 (classe 1) : {f1:.4f}\")\nprint(f\"Average precision : {ap:.4f}\")\nprint(\"\\nRapport de classification :\")\nprint(classification_report(y_test, final_pred, target_names=[\"Non fraude\", \"Fraude\"], digits=4))\nprint(\"\\nMatrice de confusion :\")\nprint(cm)\n\nplt.figure(figsize=(5, 4))\nsns.heatmap(cm, annot=True, fmt=\"d\", cmap=\"Blues\", xticklabels=[\"Non fraude\", \"Fraude\"], yticklabels=[\"Non fraude\", \"Fraude\"])\nplt.xlabel(\"Prédiction\")\nplt.ylabel(\"Réel\")\nplt.title(\"Matrice de confusion - test\")\nplt.tight_layout()\nplt.show()\n"),
    ("markdown", "### (a) Pourquoi la précision globale n'est pas suffisante\n\nUn modèle qui prédit presque toujours « non fraude » peut atteindre un très bon taux de précision globale, simplement parce que les fraudes sont rares. En fraude, ce qui compte surtout est la capacité à repérer les vrais cas positifs sans mettre trop de faux positifs."),
    ("markdown", "### (b) Compromis entre faux positifs et faux négatifs\n\nUn faux positif bloque un client légitime, ce qui entraîne du coût, de la friction et parfois de la frustration. Un faux négatif laisse passer une fraude réelle, ce qui peut coûter beaucoup plus cher à l'organisation et nuire à la confiance."),
    ("markdown", "### (c) Effet d'un seuil plus bas\n\nEn abaissant le seuil de décision, on augmente le rappel et on détecte davantage de fraudes, mais on baisse aussi la précision. Le système devient plus agressif avec plus d'alertes, ce qui a un coût opérationnel plus élevé."),
    ("markdown", "### (d) Pourquoi la validation temporelle est importante\n\nUn split aléatoire mélange des transactions du passé et du futur, ce qui introduit des fuites et surestime les performances. En pratique, il faut entraîner sur le passé et tester sur le futur, car le comportement des fraudeurs et les schémas de fraude évoluent avec le temps."),
]

for cell_type, content in cells:
    if cell_type == "markdown":
        nb.cells.append(nbformat.v4.new_markdown_cell(content))
    else:
        nb.cells.append(nbformat.v4.new_code_cell(content))

nb_path = base_dir / "fraud_detection.ipynb"
with open(nb_path, "w", encoding="utf-8") as f:
    nbformat.write(nb, f)

# Execute notebook to save outputs
with open(nb_path, "r", encoding="utf-8") as f:
    nb_executed = nbformat.read(f, as_version=4)

client = NotebookClient(nb_executed, timeout=600, kernel_name="python3")
client.execute()

with open(nb_path, "w", encoding="utf-8") as f:
    nbformat.write(nb_executed, f)

(base_dir / "requirements.txt").write_text(
    "pandas\nnumpy\nscikit-learn\nmatplotlib\nseaborn\njupyter\n",
    encoding="utf-8",
)

print(f"Notebook created and executed: {nb_path}")
print(f"CSV ready: {csv_path.exists()}")
