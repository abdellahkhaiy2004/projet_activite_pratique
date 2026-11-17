import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GATEWAY_URL } from '../../config';
import { loadingInterceptor } from '../../interceptors/loading.interceptor';
import { Accounts } from './accounts';

describe('Accounts page', () => {
  const url = `${GATEWAY_URL}/ebank-service/accounts`;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Accounts],
      providers: [provideHttpClient(withInterceptors([loadingInterceptor])), provideHttpClientTesting()]
    });
    controller = TestBed.inject(HttpTestingController);
  });

  it('shows balance, type, customer id and customer details', async () => {
    const fixture = TestBed.createComponent(Accounts);
    fixture.detectChanges();
    controller.expectOne(url).flush([{
      id: 'abcdef12-0000', createdAt: '2026-11-17T11:24:41', balance: 1234.5, type: 'SAVING_ACCOUNT',
      customerId: 1, customer: { id: 1, name: 'Mohammed', email: 'mohammed@gmail.com' }
    }]);
    fixture.detectChanges();
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('abcdef12');
    expect(text).toContain('Saving');
    expect(text).toContain('$1,234.50');
    expect(text).toContain('Mohammed');
  });

  it('flags the fallback customer as not available', () => {
    const fixture = TestBed.createComponent(Accounts);
    fixture.detectChanges();
    controller.expectOne(url).flush([{
      id: 'id-1', createdAt: '2026-11-17T11:24:41', balance: 10, type: 'CURRENT_ACCOUNT',
      customerId: 2, customer: { id: 2, name: 'Not available', email: 'Not available' }
    }]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.text-warning')?.textContent)
      .toContain('Not available');
  });

  it('shows an error message when the request fails', () => {
    const fixture = TestBed.createComponent(Accounts);
    fixture.detectChanges();
    controller.expectOne(url).flush('x', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.alert-danger')).toBeTruthy();
  });
});
