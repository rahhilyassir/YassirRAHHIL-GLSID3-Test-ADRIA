import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Account } from '../../models';
import { BankApiService } from '../../services/bank-api.service';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './accounts.component.html',
  styleUrl: './accounts.component.css',
})
export class AccountsComponent implements OnInit {
  accounts: Account[] = [];
  successMessage = '';
  errorMessage = '';

  form = {
    customerId: '',
    currency: 'EUR',
    balance: 0,
  };

  constructor(private bankApi: BankApiService) {}

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.bankApi.getAccounts().subscribe({
      next: (data) => {
        this.accounts = data;
      },
      error: () => {
        this.errorMessage = 'Unable to load accounts from the backend.';
      },
    });
  }

  submit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    const payload = {
      customerId: this.form.customerId,
      currency: this.form.currency,
      balance: this.form.balance,
    };

    this.bankApi.createAccount(payload).subscribe({
      next: (created) => {
        this.successMessage = `Account ${created.id ?? ''} created successfully.`;
        this.form = { customerId: '', currency: 'EUR', balance: 0 };
        this.loadAccounts();
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
      const values = Object.values(err.error).flatMap((entry) =>
        typeof entry === 'string' ? [entry] : Array.isArray(entry) ? entry : [JSON.stringify(entry)],
      );
      return values.join(' ');
    }

    return 'The server rejected the request.';
  }
}
