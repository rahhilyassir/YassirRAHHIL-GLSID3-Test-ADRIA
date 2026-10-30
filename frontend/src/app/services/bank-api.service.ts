import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Account, ChatMessage, ChatResponse, Transaction } from '../models';

@Injectable({ providedIn: 'root' })
export class BankApiService {
  private readonly baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.baseUrl}/api/accounts`);
  }

  createAccount(account: Partial<Account>): Observable<Account> {
    return this.http.post<Account>(`${this.baseUrl}/api/accounts`, account);
  }

  getTransactions(accountId?: number): Observable<Transaction[]> {
    const url = accountId
      ? `${this.baseUrl}/api/transactions?accountId=${accountId}`
      : `${this.baseUrl}/api/transactions`;

    return this.http.get<Transaction[]>(url);
  }

  createTransaction(payload: Record<string, unknown>): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/api/transactions`, payload);
  }

  sendChatMessage(payload: ChatMessage): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.baseUrl}/api/chat`, payload);
  }
}
