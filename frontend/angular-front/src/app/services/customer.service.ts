import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { GATEWAY_URL } from '../config';
import { Customer } from '../models/customer.model';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly url = `${GATEWAY_URL}/customer-service/customers`;

  getCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.url);
  }
}
