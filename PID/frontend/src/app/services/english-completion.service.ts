import { HttpClient } from '@angular/common/http';
import { Injectable, inject, isDevMode } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, shareReplay } from 'rxjs/operators';

/** Si le chargement du JSON échoue (SSR hors navigateur, fichier absent, etc.). */
const EMBEDDED_WORDS: readonly string[] = [
  'about',
  'after',
  'again',
  'against',
  'all',
  'also',
  'and',
  'another',
  'any',
  'are',
  'ask',
  'because',
  'been',
  'before',
  'being',
  'best',
  'better',
  'between',
  'both',
  'but',
  'can',
  'come',
  'could',
  'day',
  'did',
  'different',
  'each',
  'even',
  'every',
  'example',
  'feel',
  'find',
  'first',
  'for',
  'from',
  'get',
  'give',
  'going',
  'good',
  'great',
  'had',
  'has',
  'have',
  'help',
  'here',
  'high',
  'how',
  'important',
  'into',
  'just',
  'know',
  'language',
  'last',
  'learn',
  'learning',
  'like',
  'little',
  'long',
  'look',
  'made',
  'make',
  'many',
  'may',
  'more',
  'most',
  'much',
  'must',
  'need',
  'never',
  'new',
  'next',
  'not',
  'now',
  'off',
  'often',
  'one',
  'only',
  'other',
  'our',
  'out',
  'over',
  'own',
  'people',
  'place',
  'please',
  'practice',
  'question',
  'read',
  'really',
  'right',
  'same',
  'school',
  'see',
  'sentence',
  'should',
  'since',
  'some',
  'something',
  'sometimes',
  'speak',
  'student',
  'study',
  'such',
  'take',
  'teacher',
  'tell',
  'than',
  'thank',
  'thanks',
  'that',
  'the',
  'their',
  'them',
  'then',
  'there',
  'these',
  'they',
  'thing',
  'think',
  'this',
  'time',
  'today',
  'together',
  'too',
  'try',
  'tutor',
  'under',
  'understand',
  'until',
  'use',
  'used',
  'very',
  'want',
  'was',
  'way',
  'well',
  'were',
  'what',
  'when',
  'where',
  'which',
  'while',
  'who',
  'why',
  'will',
  'with',
  'without',
  'word',
  'work',
  'world',
  'would',
  'write',
  'year',
  'you',
  'your',
  'yourself'
].sort();

@Injectable({ providedIn: 'root' })
export class EnglishCompletionService {
  private readonly http = inject(HttpClient);
  private readonly words$ = this.http.get<string[]>('/data/en-common-words.json').pipe(
    map(list => (Array.isArray(list) ? list.map(w => String(w).toLowerCase()).filter(Boolean) : []).sort()),
    catchError(() => {
      if (isDevMode()) {
        console.warn('[EnglishCompletionService] Using embedded word list (fetch /data/en-common-words.json failed).');
      }
      return of([...EMBEDDED_WORDS]);
    }),
    shareReplay(1)
  );

  /** Suggestions pour le fragment courant (préfixe + proches si peu de résultats). */
  completionsFor(prefix: string, limit = 8): Observable<string[]> {
    const p = (prefix || '').toLowerCase().replace(/[^a-z']/g, '');
    if (p.length < 1) {
      return of([]);
    }
    return this.words$.pipe(
      map(words => this.pickCompletions(words, p, limit)),
      map(list => [...new Set(list)])
    );
  }

  private pickCompletions(words: readonly string[], p: string, limit: number): string[] {
    const out: string[] = [];
    for (const w of words) {
      if (w.length < 2 || w === p) {
        continue;
      }
      if (w.startsWith(p)) {
        out.push(w);
        if (out.length >= limit) {
          return out;
        }
      }
    }
    if (out.length >= 3 || p.length < 4) {
      return out.slice(0, limit);
    }
    for (const w of words) {
      if (w === p || out.includes(w)) {
        continue;
      }
      if (w.length >= 3 && w.length <= 24 && this.levenshtein(w, p) <= 1) {
        out.push(w);
        if (out.length >= limit) {
          break;
        }
      }
    }
    return out.slice(0, limit);
  }

  private levenshtein(a: string, b: string): number {
    if (a === b) {
      return 0;
    }
    const m = a.length;
    const n = b.length;
    if (!m) {
      return n;
    }
    if (!n) {
      return m;
    }
    const row = new Array<number>(n + 1);
    for (let j = 0; j <= n; j++) {
      row[j] = j;
    }
    for (let i = 1; i <= m; i++) {
      let prev = row[0];
      row[0] = i;
      for (let j = 1; j <= n; j++) {
        const tmp = row[j];
        const cost = a.charAt(i - 1) === b.charAt(j - 1) ? 0 : 1;
        row[j] = Math.min(row[j] + 1, row[j - 1] + 1, prev + cost);
        prev = tmp;
      }
    }
    return row[n];
  }
}
