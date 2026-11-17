import { Routes } from '@angular/router';
import { Accounts } from './pages/accounts/accounts';
import { Bot } from './pages/bot/bot';
import { Customers } from './pages/customers/customers';

export const routes: Routes = [
  { path: '', redirectTo: 'accounts', pathMatch: 'full' },
  { path: 'accounts', component: Accounts },
  { path: 'customers', component: Customers },
  { path: 'bot', component: Bot },
  { path: '**', redirectTo: 'accounts' }
];
