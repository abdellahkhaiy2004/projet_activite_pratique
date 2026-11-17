import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { GATEWAY_URL } from '../config';
import { BankAccount } from '../models/bank-account.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly url = `${GATEWAY_URL}/ebank-service/accounts`;

  getAccounts(): Observable<BankAccount[]> {
    return this.http.get<BankAccount[]>(this.url);
  }
}
