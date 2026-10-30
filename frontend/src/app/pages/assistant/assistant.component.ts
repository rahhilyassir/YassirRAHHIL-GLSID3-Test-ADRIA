import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BankApiService } from '../../services/bank-api.service';

@Component({
  selector: 'app-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assistant.component.html',
  styleUrl: './assistant.component.css',
})
export class AssistantComponent {
  readonly conversationId = 'bank-web-user';
  message = '';
  loading = false;
  errorMessage = '';
  chatMessages: { sender: 'user' | 'assistant'; text: string }[] = [
    { sender: 'assistant', text: 'How can I help with your bank operations today?' },
  ];

  constructor(private bankApi: BankApiService) {}

  sendMessage(): void {
    const text = this.message.trim();
    if (!text) {
      return;
    }

    this.chatMessages.push({ sender: 'user', text });
    this.loading = true;
    this.errorMessage = '';

    this.bankApi.sendChatMessage({ conversationId: this.conversationId, message: text }).subscribe({
      next: (response) => {
        this.chatMessages.push({ sender: 'assistant', text: response.response || 'No response returned.' });
        this.message = '';
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = 'Assistant request failed. Check that the chatbot service is running.';
        if (typeof err?.error === 'string') {
          this.errorMessage = err.error;
        }
      },
    });
  }
}
