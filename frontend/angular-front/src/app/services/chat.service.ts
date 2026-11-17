import { HttpClient, HttpDownloadProgressEvent, HttpEventType } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, distinctUntilChanged, filter, map } from 'rxjs';
import { GATEWAY_URL } from '../config';

/**
 * Turns the text received so far from a Server-Sent Events response into the answer text.
 * Each event looks like "data:<chunk>\n\n"; the chunk is taken as is (spaces are meaningful).
 * While the stream is running, a last event without its blank-line terminator is still incomplete.
 */
export function parseSse(raw: string, complete = false): string {
  const events = raw.split(/\r?\n\r?\n/);
  const last = events.pop() ?? '';
  if (complete && last.length > 0) {
    events.push(last);
  }
  return events
    .map(event => event.split(/\r?\n/)
      .filter(line => line.startsWith('data:'))
      .map(line => line.slice(5))
      .join('\n'))
    .join('');
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly url = `${GATEWAY_URL}/ebank-bot/chat`;

  /** Normal chat: the complete answer in one response. */
  ask(query: string): Observable<string> {
    return this.http.get(this.url, { params: { query }, responseType: 'text' });
  }

  /**
   * Streamed chat: emits the whole answer received so far each time more text arrives.
   * Progress events carry the partial text; the final response carries the same complete text,
   * so the consumer replaces (never appends to) its message and no text is shown twice.
   */
  stream(query: string): Observable<string> {
    return this.http.get(`${this.url}/stream`, {
      params: { query },
      headers: { Accept: 'text/event-stream' },
      observe: 'events',
      reportProgress: true,
      responseType: 'text'
    }).pipe(
      filter(event => event.type === HttpEventType.DownloadProgress || event.type === HttpEventType.Response),
      map(event => event.type === HttpEventType.Response
        ? parseSse(event.body ?? '', true)
        : parseSse((event as HttpDownloadProgressEvent).partialText ?? '')),
      distinctUntilChanged()
    );
  }
}
