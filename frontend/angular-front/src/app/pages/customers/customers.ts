import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Customer } from '../../models/customer.model';
import { CustomerService } from '../../services/customer.service';
import { LoadingService } from '../../services/loading.service';

@Component({
  selector: 'app-customers',
  imports: [FormsModule],
  templateUrl: './customers.html'
})
export class Customers {
  private readonly customerService = inject(CustomerService);
  protected readonly loadingService = inject(LoadingService);

  protected readonly customers = signal<Customer[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly search = signal('');

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    return this.customers().filter(c =>
      !term || `${c.id} ${c.name} ${c.email}`.toLowerCase().includes(term));
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.error.set(null);
    this.customerService.getCustomers().subscribe({
      next: customers => this.customers.set(customers),
      error: () => this.error.set('Could not load customers. Are the gateway and customer-service running?')
    });
  }
}
