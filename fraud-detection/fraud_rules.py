def score_transaction(amount, merchant_category, country, account_balance):
    risk = 0.0
    if amount > 500:
        risk += 0.4
    if merchant_category.lower() in {"travel", "cash", "gambling"}:
        risk += 0.3
    if country not in {"FR", "US", "MA"}:
        risk += 0.2
    if account_balance < amount:
        risk += 0.1
    return min(risk, 1.0)


def is_suspicious(amount, merchant_category, country, account_balance):
    return score_transaction(amount, merchant_category, country, account_balance) >= 0.6
