import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GATEWAY_URL } from '../config';
import { ChatService, parseSse } from './chat.service';

describe('parseSse', () => {
  it('joins complete events and keeps the spaces of each chunk', () => {
    expect(parseSse('data:Hello\n\ndata: from\n\ndata: eBank\n\n')).toBe('Hello from eBank');
  });

  it('ignores a last event that is still incomplete while streaming', () => {
    expect(parseSse('data:Hello\n\ndata: fr')).toBe('Hello');
  });

  it('keeps the last event when the response is complete', () => {
    expect(parseSse('data:Hello\n\ndata: world', true)).toBe('Hello world');
  });

  it('rebuilds line breaks from multi-line events', () => {
    expect(parseSse('data:one\ndata:two\n\n')).toBe('one\ntwo');
  });
});

describe('ChatService', () => {
  let service: ChatService;
  let controller: HttpTestingController;
  const url = `${GATEWAY_URL}/ebank-bot/chat`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ChatService);
    controller = TestBed.inject(HttpTestingController);
  });

  it('ask() returns the plain text answer', () => {
    let answer = '';
    service.ask('hi').subscribe(a => (answer = a));
    const req = controller.expectOne(r => r.url === url && r.params.get('query') === 'hi');
    req.flush('Hello!');
    expect(answer).toBe('Hello!');
  });

  it('stream() emits a growing answer from progress events, then the same final text once', () => {
    const emitted: string[] = [];
    service.stream('hi').subscribe(a => emitted.push(a));
    const req = controller.expectOne(r => r.url === `${url}/stream` && r.params.get('query') === 'hi');

    req.event({ type: HttpEventType.DownloadProgress, loaded: 12, partialText: 'data:Hello\n\n' });
    req.event({ type: HttpEventType.DownloadProgress, loaded: 24, partialText: 'data:Hello\n\ndata: world\n\n' });
    req.flush('data:Hello\n\ndata: world\n\n');

    expect(emitted).toEqual(['Hello', 'Hello world']);
  });
});
