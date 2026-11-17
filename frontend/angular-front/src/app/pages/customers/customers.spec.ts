import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GATEWAY_URL } from '../../config';
import { loadingInterceptor } from '../../interceptors/loading.interceptor';
import { Customers } from './customers';

describe('Customers page', () => {
  const url = `${GATEWAY_URL}/customer-service/customers`;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Customers],
      providers: [provideHttpClient(withInterceptors([loadingInterceptor])), provideHttpClientTesting()]
    });
    controller = TestBed.inject(HttpTestingController);
  });

  it('shows a spinner while loading, then the customers', async () => {
    const fixture = TestBed.createComponent(Customers);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.spinner-border')).toBeTruthy();

    controller.expectOne(url).flush([
      { id: 1, name: 'Mohammed', email: 'mohammed@gmail.com' },
      { id: 2, name: 'Iman', email: 'iman@gmail.com' }
    ]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(el.querySelector('.spinner-border')).toBeNull();
    expect(el.querySelectorAll('tbody tr').length).toBe(2);
    expect(el.textContent).toContain('Mohammed');
  });

  it('filters with the search box', async () => {
    const fixture = TestBed.createComponent(Customers);
    fixture.detectChanges();
    controller.expectOne(url).flush([
      { id: 1, name: 'Mohammed', email: 'm@x.com' },
      { id: 2, name: 'Iman', email: 'i@x.com' }
    ]);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input[type=search]') as HTMLInputElement;
    input.value = 'iman';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(1);
  });

  it('shows an error message when the request fails', () => {
    const fixture = TestBed.createComponent(Customers);
    fixture.detectChanges();
    controller.expectOne(url).flush('x', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.alert-danger')).toBeTruthy();
  });
});
