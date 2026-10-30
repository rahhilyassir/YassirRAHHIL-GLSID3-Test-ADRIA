import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Transaction } from '../../models';
import { BankApiService } from '../../services/bank-api.service';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.css',
})
export class TransactionsComponent implements OnInit {
  transactions: Transaction[] = [];
  successMessage = '';
  errorMessage = '';
  selectedAccountId: number | null = null;

  form = {
    accountId: 1,
    amount: 50,
    currency: 'EUR',
    merchantCategory: 'SHOPPING',
    country: 'FR',
  };

  constructor(private bankApi: BankApiService) {}

  ngOnInit(): void {
    this.loadTransactions();
  }

  loadTransactions(accountId?: number): void {
    const id = accountId ?? this.form.accountId ?? this.selectedAccountId;
    this.bankApi.getTransactions(id ?? undefined).subscribe({
      next: (data) => {
        this.transactions = data;
      },
      error: () => {
        this.errorMessage = 'Unable to load transactions.';
      },
    });
  }

  submit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    const payload = {
      accountId: this.form.accountId,
      amount: this.form.amount,
      currency: this.form.currency,
      merchantCategory: this.form.merchantCategory,
      country: this.form.country,
    };

    this.bankApi.createTransaction(payload).subscribe({
      next: (transaction) => {
        this.successMessage = `Transaction ${transaction.id ?? ''} created successfully.`;
        this.selectedAccountId = this.form.accountId;
        this.loadTransactions(this.form.accountId);
      },
      error: (err) => {
        this.errorMessage = this.formatError(err);
      },
    });
  }

  private formatError(err: any): string {
    if (typeof err?.error === 'string') {
      return err.error;
    }

    if (err?.error && typeof err.error === 'object') {
      const messages = Object.values(err.error).flatMap((entry) =>
        typeof entry === 'string' ? [entry] : Array.isArray(entry) ? entry : [JSON.stringify(entry)],
      );
      return messages.join(' ');
    }

    return 'The transaction request failed on the server.';
  }
}
