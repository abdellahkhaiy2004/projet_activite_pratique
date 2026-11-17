import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LoadingService } from '../services/loading.service';
import { loadingInterceptor } from './loading.interceptor';

describe('loadingInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let loading: LoadingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([loadingInterceptor])), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    loading = TestBed.inject(LoadingService);
  });

  it('is on while a request is in flight and off after success', () => {
    http.get('/x').subscribe();
    expect(loading.loading()).toBe(true);
    controller.expectOne('/x').flush({});
    expect(loading.loading()).toBe(false);
  });

  it('is turned off after an error too', () => {
    http.get('/x').subscribe({ error: () => undefined });
    expect(loading.loading()).toBe(true);
    controller.expectOne('/x').flush('boom', { status: 500, statusText: 'Server Error' });
    expect(loading.loading()).toBe(false);
  });

  it('stays on until every parallel request has finished', () => {
    http.get('/a').subscribe();
    http.get('/b').subscribe();
    controller.expectOne('/a').flush({});
    expect(loading.loading()).toBe(true);
    controller.expectOne('/b').flush({});
    expect(loading.loading()).toBe(false);
  });
});
