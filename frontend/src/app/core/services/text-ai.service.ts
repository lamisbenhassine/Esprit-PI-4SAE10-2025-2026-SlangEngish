import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of, switchMap, throwError, timer, timeout } from 'rxjs';
import { retry } from 'rxjs/operators';

interface SummarizeApiResponse {
  summary?: string;
}

interface TranslateApiResponse {
  translatedText?: string;
}

interface TranslateTopicApiResponse {
  trTitle?: string;
  trDescription?: string;
}

interface PolishEnglishApiResponse {
  correctedText?: string;
}

@Injectable({ providedIn: 'root' })
export class TextAiService {
  private static readonly TRANSLATE_TIMEOUT_MS = 12000;

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
   * Traduction vers une langue cible (codes ISO 639-1, liste côté serveur).
   * En cas d’erreur ou d’IA indisponible, renvoie le texte d’origine.
   */
  translate(text: string, targetLanguage: string): Observable<string> {
    const clean = (text || '').trim();
    const lang = (targetLanguage || 'en').trim().toLowerCase().slice(0, 2);
    if (!clean) {
      return of('');
    }
    return this.retryHttpOn429(
      this.http.post<TranslateApiResponse>('/api/forum/ai/translate', { text: clean, targetLanguage: lang }),
      0
    ).pipe(
        timeout(TextAiService.TRANSLATE_TIMEOUT_MS),
        switchMap(res => {
          const out = (res?.translatedText ?? '').trim();
          if (!out) {
            return throwError(
              () => new Error('Réponse de traduction vide (vérifiez le microservice forum et la clé IA).')
            );
          }
          return of(out);
        })
      );
  }

  /** Équivalent à {@link translate} avec la cible anglais (TTS / sélection). */
  translateToEnglish(text: string): Observable<string> {
    return this.translate(text, 'en');
  }

  /**
   * Traduction titre + description en un seul appel (moins de 429 que deux /translate).
   */
  /**
   * Orthographe / grammaire / style via Gemini (microservice forum).
   * En cas d’erreur réseau ou d’IA indisponible, renvoie une chaîne vide (le caller applique le fallback local).
   */
  polishEnglish(text: string): Observable<string> {
    const clean = (text || '').trim();
    if (!clean) {
      return of('');
    }
    return this.http.post<PolishEnglishApiResponse>('/api/forum/ai/polish-english', { text: clean }).pipe(
      map(res => (res?.correctedText ?? '').trim()),
      catchError(() => of(''))
    );
  }

  translateTopicPost(
    title: string,
    description: string,
    targetLanguage: string
  ): Observable<{ trTitle: string; trDescription: string }> {
    const cleanTitle = (title || '').trim();
    const cleanDesc = (description || '').trim();
    const lang = (targetLanguage || 'en').trim().toLowerCase().slice(0, 2);
    return this.retryHttpOn429(
      this.http.post<TranslateTopicApiResponse>('/api/forum/ai/translate-topic', {
        title: cleanTitle,
        description: cleanDesc,
        targetLanguage: lang
      }),
      0
    ).pipe(
      timeout(TextAiService.TRANSLATE_TIMEOUT_MS),
      switchMap(res => {
        const trTitle = (res?.trTitle ?? '').trim();
        const trDescription = (res?.trDescription ?? '').trim();
        if (!trTitle && !trDescription) {
          return throwError(
            () => new Error('Réponse de traduction vide (vérifiez le microservice forum).')
          );
        }
        return of({ trTitle, trDescription });
      }),
      catchError(() =>
        forkJoin({
          trTitle: cleanTitle ? this.translate(cleanTitle, lang).pipe(catchError(() => of(cleanTitle))) : of(''),
          trDescription: cleanDesc ? this.translate(cleanDesc, lang).pipe(catchError(() => of(cleanDesc))) : of('')
        })
      )
    );
  }

  /**
   * Nouvelle clé API ≠ nouveau quota : Google limite par projet / minute.
   * Retente automatiquement sur 429 avec délais croissants (en plus des retries serveur).
   */
  /**
   * @param maxRetries 0 = pas de retry client (évite d’empiler avec les retries serveur → timeouts proxy / ERR_EMPTY_RESPONSE).
   */
  private retryHttpOn429<T>(request$: Observable<T>, maxRetries = 2): Observable<T> {
    if (maxRetries <= 0) {
      return request$;
    }
    return request$.pipe(
      retry({
        count: maxRetries,
        delay: (error: unknown, retryCount: number) => {
          if (error instanceof HttpErrorResponse && error.status === 429) {
            const ms = Math.min(8000 * retryCount, 45_000);
            return timer(ms);
          }
          return throwError(() => error);
        }
      })
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

