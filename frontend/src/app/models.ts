export interface Account {
  id?: number;
  customerId: string;
  currency: string;
  balance?: number;
  status?: string;
}

export interface Transaction {
  id?: number;
  accountId: number;
  amount: number;
  currency: string;
  merchantCategory?: string;
  country?: string;
  type?: string;
  createdAt?: string;
}

export interface ChatMessage {
  conversationId: string;
  message: string;
}

export interface ChatResponse {
  response: string;
}
