import { Component, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatMessage } from '../../models/chat-message.model';
import { ChatService } from '../../services/chat.service';

@Component({
  selector: 'app-bot',
  imports: [FormsModule],
  templateUrl: './bot.html'
})
export class Bot {
  private readonly chatService = inject(ChatService);
  private readonly history = viewChild<ElementRef<HTMLElement>>('history');

  protected readonly messages = signal<ChatMessage[]>([]);
  protected readonly query = signal('');
  protected readonly streaming = signal(true);
  protected readonly busy = signal(false);

  protected readonly examples = [
    'List all customers',
    'List all bank accounts',
    'Show the accounts of customer 1'
  ];

  constructor() {
    // keep the newest text visible while the answer grows
    effect(() => {
      this.messages();
      const el = this.history()?.nativeElement;
      if (el) {
        queueMicrotask(() => (el.scrollTop = el.scrollHeight));
      }
    });
  }

  protected send(text: string = this.query()): void {
    const question = text.trim();
    if (!question || this.busy()) {
      return;
    }
    this.query.set('');
    this.busy.set(true);
    this.messages.update(list => [...list, { role: 'user', text: question }, { role: 'bot', text: '' }]);

    const answer$ = this.streaming() ? this.chatService.stream(question) : this.chatService.ask(question);
    answer$.subscribe({
      // each emission is the whole answer so far: replace the bot message, never append
      next: answer => this.updateBotMessage({ text: answer }),
      error: () => {
        this.updateBotMessage({ text: 'The assistant is not available right now. Please try again.', error: true });
        this.busy.set(false);
      },
      complete: () => this.busy.set(false)
    });
  }

  private updateBotMessage(changes: Partial<ChatMessage>): void {
    this.messages.update(list => {
      const copy = [...list];
      copy[copy.length - 1] = { ...copy[copy.length - 1], ...changes };
      return copy;
    });
  }
}
