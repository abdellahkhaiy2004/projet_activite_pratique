import { Injectable, computed, signal } from '@angular/core';

/** Counts running HTTP requests; `loading` is true while at least one is in flight. */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly pending = signal(0);
  readonly loading = computed(() => this.pending() > 0);

  show(): void {
    this.pending.update(n => n + 1);
  }

  hide(): void {
    this.pending.update(n => Math.max(0, n - 1));
  }
}
