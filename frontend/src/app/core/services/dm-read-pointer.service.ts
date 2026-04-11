import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

const STORAGE_KEY = 'slang_dm_read_v1';

/**
 * Pointeurs de lecture locale (par utilisateur + conversation) pour les badges « non lu ».
 */
@Injectable({ providedIn: 'root' })
export class DmReadPointerService {
  constructor(@Inject(PLATFORM_ID) private platformId: object) {}

  getReadUpTo(viewerId: number, conversationId: number): number {
    if (!isPlatformBrowser(this.platformId)) {
      return 0;
    }
    const v = this.parse()[String(viewerId)]?.[String(conversationId)];
    return typeof v === 'number' && !Number.isNaN(v) ? v : 0;
  }

  markConversationRead(viewerId: number, conversationId: number, maxMessageId: number): void {
    if (!isPlatformBrowser(this.platformId) || !Number.isFinite(maxMessageId)) {
      return;
    }
    const data = this.parse();
    const vk = String(viewerId);
    const ck = String(conversationId);
    if (!data[vk]) {
      data[vk] = {};
    }
    const prev = data[vk][ck] ?? 0;
    if (maxMessageId > prev) {
      data[vk][ck] = maxMessageId;
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
      } catch {
        /* ignore quota */
      }
    }
  }

  private parse(): Record<string, Record<string, number>> {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return {};
      }
      const o = JSON.parse(raw) as Record<string, Record<string, number>>;
      return o && typeof o === 'object' ? o : {};
    } catch {
      return {};
    }
  }
}
