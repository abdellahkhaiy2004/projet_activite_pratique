import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GATEWAY_URL } from '../../config';
import { Bot } from './bot';

describe('Bot page', () => {
  const url = `${GATEWAY_URL}/ebank-bot/chat`;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Bot],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    controller = TestBed.inject(HttpTestingController);
  });

  async function ask(fixture: any, question: string) {
    await fixture.whenStable(); // ngModel registers its controls asynchronously
    const el = fixture.nativeElement as HTMLElement;
    const input = el.querySelector('input[name=query]') as HTMLInputElement;
    input.value = question;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('shows the streamed answer growing and ends with the text shown once', async () => {
    const fixture = TestBed.createComponent(Bot);
    fixture.detectChanges();
    await ask(fixture, 'list customers');

    const req = controller.expectOne(r => r.url === `${url}/stream`);
    req.event({ type: HttpEventType.DownloadProgress, loaded: 1, partialText: 'data:You have\n\n' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('You have');

    req.event({ type: HttpEventType.DownloadProgress, loaded: 2, partialText: 'data:You have\n\ndata: 3 customers\n\n' });
    req.flush('data:You have\n\ndata: 3 customers\n\n');
    fixture.detectChanges();
    await fixture.whenStable();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('You have 3 customers');
    expect(text.split('You have 3 customers').length - 1).toBe(1);
  });

  it('uses the normal endpoint when streaming is switched off', async () => {
    const fixture = TestBed.createComponent(Bot);
    fixture.detectChanges();
    await fixture.whenStable(); // let ngModel write its initial value first
    const toggle = fixture.nativeElement.querySelector('#streamSwitch') as HTMLInputElement;
    toggle.click();
    fixture.detectChanges();
    await ask(fixture, 'hello');

    controller.expectOne(r => r.url === url && r.params.get('query') === 'hello').flush('Hi there');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Hi there');
  });

  it('shows an error bubble and re-enables the form when the request fails', async () => {
    const fixture = TestBed.createComponent(Bot);
    fixture.detectChanges();
    await ask(fixture, 'hello');
    controller.expectOne(r => r.url === `${url}/stream`).flush('x', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    await fixture.whenStable(); // [disabled] on an ngModel input is applied asynchronously
    expect(fixture.nativeElement.textContent).toContain('not available');
    expect((fixture.nativeElement.querySelector('input[name=query]') as HTMLInputElement).disabled).toBe(false);
  });
});
