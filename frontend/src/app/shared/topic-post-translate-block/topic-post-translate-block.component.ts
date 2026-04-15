import { Component, Input } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TextAiService } from '../../core/services/text-ai.service';
import {
  TRANSLATION_TARGET_LANGUAGES,
  TranslationTarget
} from '../../core/data/translation-target-languages';

interface CachedPair {
  trTitle: string;
  trDesc: string;
}

/** Bloc titre + extrait : liste « Langue cible » + bouton Traduire (API forum). */
@Component({
  selector: 'app-topic-post-translate-block',
  templateUrl: './topic-post-translate-block.component.html',
  styleUrls: ['./topic-post-translate-block.component.css']
})
export class TopicPostTranslateBlockComponent {
  @Input() variant: 'card' | 'hero' = 'card';

  @Input() title = '';
  @Input() description = '';
  @Input() previewMaxLength = 160;

  readonly targetLanguages: TranslationTarget[] = TRANSLATION_TARGET_LANGUAGES;

  /** Langue choisie dans le sélecteur (ISO 639-1). */
  pickedLang = 'fr';

  showTranslated = false;
  busy = false;
  /** Langue affichée (code ISO). */
  activeLang: string | null = null;
  private readonly cache = new Map<string, CachedPair>();

  constructor(
    private textAi: TextAiService,
    private snackBar: MatSnackBar
  ) {}

  get activeLangLabel(): string {
    if (!this.activeLang) {
      return '';
    }
    return this.targetLanguages.find(l => l.code === this.activeLang)?.label || this.activeLang.toUpperCase();
  }

  get displayTitle(): string {
    if (!this.showTranslated || !this.activeLang) {
      return (this.title || '').trim();
    }
    const c = this.cache.get(this.activeLang);
    const t = c?.trTitle ?? this.title;
    return (t || '').trim();
  }

  get displayDescription(): string {
    if (!this.showTranslated || !this.activeLang) {
      const s = (this.description || '').trim();
      return this.truncate(s);
    }
    const c = this.cache.get(this.activeLang);
    const raw = (c?.trDesc ?? this.description ?? '').trim();
    return this.truncate(raw);
  }

  private truncate(s: string): string {
    if (this.variant === 'hero' || !this.previewMaxLength || this.previewMaxLength <= 0) {
      return s;
    }
    if (s.length <= this.previewMaxLength) {
      return s;
    }
    return `${s.slice(0, this.previewMaxLength)}…`;
  }

  get canTranslate(): boolean {
    return !!(this.title?.trim() || this.description?.trim());
  }

  showOriginal(ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    this.showTranslated = false;
  }

  onTranslateClick(ev: Event): void {
    this.runTranslate(this.pickedLang, ev);
  }

  private translateErrorMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      const body = err.error as { error?: string } | string | null;
      const serverMsg =
        body && typeof body === 'object' && typeof body.error === 'string'
          ? body.error.trim()
          : typeof body === 'string'
            ? body.trim().slice(0, 200)
            : '';

      if (err.status === 0) {
        return (
          'Forum injoignable (réseau). Démarrez le microservice forum sur le port 8040. ' +
          'Avec ng serve (4200) : proxy déjà actif. Avec serve:ssr (4000) : le serveur Node relaie aussi /api/forum → 8040.'
        );
      }
      if (err.status === 429) {
        return (
          serverMsg ||
          'Quota ou limite Google (429). Attendez 1–2 minutes, ou consultez votre forfait sur Google AI Studio.'
        );
      }
      if (err.status === 503) {
        if (serverMsg.includes('not configured') || serverMsg.includes('FORUM_AI_API_KEY')) {
          return 'IA forum : configurez FORUM_AI_API_KEY (ou application-local.properties) sur le microservice forum.';
        }
        if (serverMsg.includes('429')) {
          return 'Limite de débit Google atteinte. Réessayez dans une minute.';
        }
        return serverMsg || 'IA forum temporairement indisponible (503).';
      }
      if (serverMsg) {
        return serverMsg;
      }
    }
    if (err instanceof Error && err.message) {
      return err.message;
    }
    return 'Traduction indisponible. Vérifiez le microservice forum et la clé IA.';
  }

  runTranslate(langCode: string, ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    if (!this.canTranslate) {
      return;
    }
    const title = (this.title || '').trim();
    const desc = (this.description || '').trim();
    const cached = this.cache.get(langCode);
    if (cached) {
      this.activeLang = langCode;
      this.showTranslated = true;
      return;
    }
    this.busy = true;
    this.textAi.translateTopicPost(title, desc, langCode).subscribe({
      next: ({ trTitle, trDescription }) => {
        this.cache.set(langCode, {
          trTitle,
          trDesc: trDescription
        });
        this.activeLang = langCode;
        this.showTranslated = true;
        this.busy = false;
      },
      error: (err: unknown) => {
        this.busy = false;
        const msg = this.translateErrorMessage(err);
        this.snackBar.open(msg, 'OK', { duration: 6500 });
      }
    });
  }
}
