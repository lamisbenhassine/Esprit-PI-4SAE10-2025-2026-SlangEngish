import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map } from 'rxjs/operators';
import { EN_COMMON_TYPOS } from '../core/data/english-typo-map';
import { TextAiService } from './text-ai.service';

/** Extrait `{ error: string }` ou une chaîne brute des réponses API forum. */
export function apiErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof HttpErrorResponse) {
    const b = err.error;
    if (b && typeof b === 'object' && b !== null && 'error' in b) {
      const m = (b as { error: unknown }).error;
      if (typeof m === 'string' && m.trim()) {
        return m.trim();
      }
    }
    if (typeof b === 'string' && b.trim()) {
      return b.trim();
    }
  }
  return fallback;
}

/** Aligné sur le filtrage serveur (ContentModerationService) — garde-fou avant envoi. */
const PROFANITY =
  /\b(?:fuck|fucking|fucked|fucker|fucks|shit|bullshit|bitch|bastard|asshole|wtf|wth|stfu|lmfao|damn|crap|piss|pissed|cunt|slut|whore|dick|cock|putain|merde|connard|salope|enculé|encule)\b/i;

@Injectable({ providedIn: 'root' })
export class ComposeAssistService {
  readonly profanityHint =
    'Ce message ne peut pas être envoyé : vocabulaire inapproprié détecté.';

  constructor(private readonly textAi: TextAiService) {}

  hasProfanity(text: string | null | undefined): boolean {
    return !!(text && PROFANITY.test(text));
  }

  validateForSend(text: string | null | undefined): { ok: true } | { ok: false; message: string } {
    if (this.hasProfanity(text || '')) {
      return { ok: false, message: this.profanityHint };
    }
    return { ok: true };
  }

  /**
   * Gemini (forum) si disponible, sinon corrections locales (fautes courantes + slang).
   */
  smartPolish$(raw: string): Observable<{ text: string; source: 'ai' | 'local' }> {
    const v = this.validateForSend(raw);
    if (!v.ok) {
      return throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            statusText: 'Bad Request',
            error: { error: v.message }
          })
      );
    }
    const local = this.applyEnglishCasualFixes(raw);
    return this.textAi.polishEnglish(raw).pipe(
      map(ai => {
        const t = (ai || '').trim();
        if (t.length > 0) {
          return { text: t, source: 'ai' as const };
        }
        return { text: local, source: 'local' as const };
      })
    );
  }

  private applyTypoDictionary(text: string): string {
    let s = text;
    for (const [wrong, right] of Object.entries(EN_COMMON_TYPOS) as Array<[string, string]>) {
      const escaped = wrong.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const re = new RegExp(`\\b${escaped}\\b`, 'gi');
      s = s.replace(re, right);
    }
    return s;
  }

  /**
   * Dataset de fautes fréquentes + anglais familier + capitalisation simple.
   * Utilisé en secours si l’IA n’est pas disponible.
   */
  applyEnglishCasualFixes(raw: string): string {
    let s = this.applyTypoDictionary(raw);
    s = s.replace(/\bi\s+wana\b/gi, 'I want to');
    s = s.replace(/\bi\s+wan\s+na\b/gi, 'I want to');
    s = s.replace(/\bwanna\b/gi, 'want to');
    s = s.replace(/\bgonna\b/gi, 'going to');
    s = s.replace(/\bgotta\b/gi, 'have to');
    s = s.replace(/\bkinda\b/gi, 'kind of');
    s = s.replace(/\bsometh\b/gi, 'something');
    s = s.replace(/\banythin\b/gi, 'anything');
    s = s.replace(/\bnothin\b/gi, 'nothing');
    s = s.replace(/\bplz\b/gi, 'please');
    s = s.replace(/\bthx\b/gi, 'thanks');
    s = s.replace(/\bpls\b/gi, 'please');
    s = s.replace(/\bim\b/gi, "I'm");
    s = s.replace(/\bive\b/gi, "I've");
    s = s.replace(/\bdont\b/gi, "don't");
    s = s.replace(/\bcant\b/gi, "can't");
    s = s.replace(/\bwont\b/gi, "won't");
    s = s.replace(/\bdidnt\b/gi, "didn't");
    s = s.replace(/\bisnt\b/gi, "isn't");
    s = s.replace(/\bwasnt\b/gi, "wasn't");
    s = s.replace(/\bsomethin\b/gi, 'something');
    const t = s.trim();
    if (t.length > 0 && /^[a-z]/.test(t)) {
      return t.charAt(0).toUpperCase() + t.slice(1);
    }
    return s;
  }
}
