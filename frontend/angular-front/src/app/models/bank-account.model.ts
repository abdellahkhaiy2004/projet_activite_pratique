import { Customer } from './customer.model';

export type AccountType = 'CURRENT_ACCOUNT' | 'SAVING_ACCOUNT';

export interface BankAccount {
  id: string;
  createdAt: string;
  balance: number;
  type: AccountType;
  customerId: number;
  /** Filled by ebank-service from customer-service (name is "Not available" when it is down). */
  customer?: Customer | null;
}
