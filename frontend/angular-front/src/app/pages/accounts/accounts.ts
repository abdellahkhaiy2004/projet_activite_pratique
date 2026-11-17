import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BankAccount } from '../../models/bank-account.model';
import { AccountService } from '../../services/account.service';
import { LoadingService } from '../../services/loading.service';

@Component({
  selector: 'app-accounts',
  imports: [FormsModule, CurrencyPipe, DatePipe],
  templateUrl: './accounts.html'
})
export class Accounts {
  private readonly accountService = inject(AccountService);
  protected readonly loadingService = inject(LoadingService);

  protected readonly accounts = signal<BankAccount[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly search = signal('');

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    return this.accounts().filter(a =>
      !term || `${a.id} ${a.type} ${a.customerId} ${a.customer?.name ?? ''} ${a.customer?.email ?? ''}`
        .toLowerCase().includes(term));
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.error.set(null);
    this.accountService.getAccounts().subscribe({
      next: accounts => this.accounts.set(accounts),
      error: () => this.error.set('Could not load accounts. Are the gateway and ebank-service running?')
    });
  }

  protected typeLabel(type: string): string {
    return type === 'SAVING_ACCOUNT' ? 'Saving' : 'Current';
  }

  protected customerAvailable(account: BankAccount): boolean {
    return !!account.customer && account.customer.name !== 'Not available';
  }
}
