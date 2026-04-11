import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';

interface SummarizeApiResponse {
  summary?: string;
}

interface TranslateApiResponse {
  translatedText?: string;
}

@Injectable({ providedIn: 'root' })
export class TextAiService {
  constructor(private http: HttpClient) {}

  /**
   * Preferred path: backend Gemini (/api/forum/ai/summarize).
   * Fallback: local extractive summary in frontend.
   */
  summarizeText(text: string): Observable<{ summary: string; source: 'ai' | 'fallback' }> {
    const clean = (text || '').trim();
    if (!clean) {
      return of({ summary: '', source: 'fallback' });
    }
    return this.http.post<SummarizeApiResponse>('/api/forum/ai/summarize', { text: clean }).pipe(
      map(res => {
        let summary = (res?.summary || '').trim();
        if (!summary) {
          return { summary: this.createLocalSummary(clean), source: 'fallback' as const };
        }
        summary = this.compactSummaryOutput(summary);
        return { summary, source: 'ai' as const };
      }),
      catchError(() => of({ summary: this.createLocalSummary(clean), source: 'fallback' as const }))
    );
  }

  /**
   * Translation to English via backend Gemini (same key as summarize).
   * On error or missing AI, returns the original text for TTS fallback.
   */
  translateToEnglish(text: string): Observable<string> {
    const clean = (text || '').trim();
    if (!clean) {
      return of('');
    }
    return this.http.post<TranslateApiResponse>('/api/forum/ai/translate-to-english', { text: clean }).pipe(
      map(res => (res?.translatedText || clean).trim()),
      catchError(() => of(clean))
    );
  }

  private createLocalSummary(text: string): string {
    const cleaned = text.replace(/\s+/g, ' ').trim();
    const sentences = cleaned
      .split(/(?<=[.!?])\s+/)
      .map(s => s.trim())
      .filter(Boolean);
    if (!sentences.length) {
      return '';
    }
    if (sentences.length <= 3) {
      return sentences.map(s => `• ${this.shortenPhrase(s, 95)}`).join('\n');
    }

    const words = cleaned
      .toLowerCase()
      .replace(/[^a-z0-9\s]/g, ' ')
      .split(/\s+/)
      .filter(w => w.length > 3);
    const frequency: Record<string, number> = {};
    for (const w of words) {
      frequency[w] = (frequency[w] || 0) + 1;
    }

    const scored = sentences
      .map((sentence, idx) => {
        const score = sentence
          .toLowerCase()
          .replace(/[^a-z0-9\s]/g, ' ')
          .split(/\s+/)
          .reduce((sum, token) => sum + (frequency[token] || 0), 0);
        return { sentence, idx, score };
      })
      .sort((a, b) => b.score - a.score);

    const n = Math.min(3, scored.length);
    const picked = scored.slice(0, n).sort((a, b) => a.idx - b.idx);
    return picked.map(p => `• ${this.shortenPhrase(p.sentence, 95)}`).join('\n');
  }

  /** Formate et limite le résumé IA en quelques phrases courtes. */
  private compactSummaryOutput(summary: string): string {
    const raw = summary.trim();
    const lines = raw.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
    const hasBullets = lines.some(l => /^[-*•]\s/.test(l));
    if (hasBullets && lines.length) {
      return lines
        .map(l => l.replace(/^[-*•]\s*/, '').trim())
        .filter(Boolean)
        .map(l => this.shortenPhrase(l, 95))
        .slice(0, 5)
        .map(s => `• ${s}`)
        .join('\n');
    }
    const oneLine = raw.replace(/\s+/g, ' ');
    const sents = oneLine.split(/(?<=[.!?])\s+/).map(s => s.trim()).filter(Boolean);
    const chunks = (sents.length ? sents : [oneLine]).slice(0, 4).map(s => this.shortenPhrase(s, 85));
    return chunks.map(s => `• ${s}`).join('\n');
  }

  private shortenPhrase(s: string, maxLen: number): string {
    const t = s.trim();
    if (t.length <= maxLen) {
      return t;
    }
    const cut = t.slice(0, maxLen - 1).trim();
    const lastSpace = cut.lastIndexOf(' ');
    const base = lastSpace > 20 ? cut.slice(0, lastSpace) : cut;
    return `${base}…`;
  }
}

