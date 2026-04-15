import {
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  ViewChild
} from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TextAiService } from '../../core/services/text-ai.service';
import {
  TRANSLATION_TARGET_LANGUAGES,
  TranslationTarget
} from '../../core/data/translation-target-languages';

@Component({
  selector: 'app-forum-selection-assist',
  templateUrl: './forum-selection-assist.component.html',
  styleUrls: ['./forum-selection-assist.component.css']
})
export class ForumSelectionAssistComponent implements OnDestroy {
  @ViewChild('hostRef', { static: true }) hostRef!: ElementRef<HTMLElement>;

  barVisible = false;
  barTop = 0;
  barLeft = 0;

  selectedText = '';
  translating = false;

  summarizing = false;
  summaryOpen = false;
  generatedSummary = '';
  summarySource: 'ai' | 'fallback' | '' = '';

  readonly longTextThreshold = 220;

  /** Langue cible pour traduction + lecture vocale de la sélection. */
  ttsTargetLang = 'en';
  readonly readLanguages: TranslationTarget[] = TRANSLATION_TARGET_LANGUAGES;

  private selectedUtterance?: SpeechSynthesisUtterance;
  private availableVoices: SpeechSynthesisVoice[] = [];
  private readonly isBrowser = typeof window !== 'undefined';

  constructor(
    private textAi: TextAiService,
    private snackBar: MatSnackBar
  ) {
    this.initSpeechVoices();
  }

  ngOnDestroy(): void {
    this.stopReading();
  }

  @HostListener('window:scroll')
  onScroll(): void {
    if (this.barVisible) {
      this.closeBar();
    }
  }

  @HostListener('document:mousedown', ['$event'])
  onDocMouseDown(ev: MouseEvent): void {
    const t = ev.target as HTMLElement | null;
    if (!t || !this.hostRef?.nativeElement) {
      return;
    }
    if (t.closest('.fsa-toolbar') || t.closest('.fsa-summary')) {
      return;
    }
    if (this.hostRef.nativeElement.contains(t)) {
      return;
    }
    this.closeBar();
  }

  onHostMouseUp(): void {
    if (!this.isBrowser) {
      return;
    }
    setTimeout(() => this.syncSelectionFromDom(), 0);
  }

  onHostKeyUp(): void {
    if (!this.isBrowser) {
      return;
    }
    setTimeout(() => this.syncSelectionFromDom(), 0);
  }

  private syncSelectionFromDom(): void {
    const sel = window.getSelection();
    const text = sel?.toString().trim() || '';
    if (!text || text.length < 2) {
      this.closeBar();
      return;
    }
    const anchor = sel?.anchorNode;
    if (!anchor || !this.hostRef.nativeElement.contains(anchor)) {
      this.closeBar();
      return;
    }
    this.selectedText = text;
    this.generatedSummary = '';
    this.summarySource = '';
    this.summaryOpen = false;
    this.positionBar(sel);
    this.barVisible = true;
  }

  private positionBar(sel: Selection): void {
    let rect: DOMRect | null = null;
    try {
      if (sel.rangeCount > 0) {
        rect = sel.getRangeAt(0).getBoundingClientRect();
      }
    } catch {
      rect = null;
    }
    if (!rect || (rect.width === 0 && rect.height === 0)) {
      this.barTop = 80;
      this.barLeft = 24;
      return;
    }
    const pad = 8;
    const toolbarH = 44;
    // position: fixed → coordonnées viewport
    const top = rect.top - toolbarH - pad;
    const left = rect.left + rect.width / 2;
    this.barTop = Math.max(8, top);
    this.barLeft = Math.max(8, left);
  }

  canSummarize(): boolean {
    return this.selectedText.length >= this.longTextThreshold;
  }

  readAloud(ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    if (!this.selectedText) {
      return;
    }
    if (!this.isBrowser || !('speechSynthesis' in window)) {
      this.snackBar.open('Synthèse vocale non disponible sur ce navigateur.', 'OK', { duration: 3500 });
      return;
    }
    this.translating = true;
    this.textAi.translate(this.selectedText, this.ttsTargetLang).subscribe({
      next: translated => {
        this.translating = false;
        this.speakTranslated(translated || this.selectedText, this.ttsTargetLang);
      },
      error: () => {
        this.translating = false;
        this.speakTranslated(this.selectedText, this.ttsTargetLang);
      }
    });
  }

  summarize(ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    if (!this.canSummarize()) {
      return;
    }
    this.summaryOpen = true;
    this.summarizing = true;
    this.generatedSummary = '';
    this.summarySource = '';
    this.textAi.summarizeText(this.selectedText).subscribe({
      next: result => {
        this.generatedSummary = result.summary;
        this.summarySource = result.source;
        this.summarizing = false;
      },
      error: () => {
        this.summarizing = false;
      }
    });
  }

  stopReading(): void {
    if (!this.isBrowser) {
      return;
    }
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    this.selectedUtterance = undefined;
  }

  closeBar(): void {
    this.barVisible = false;
    this.selectedText = '';
    this.summaryOpen = false;
  }

  closeSummary(): void {
    this.summaryOpen = false;
  }

  private initSpeechVoices(): void {
    if (!this.isBrowser || !('speechSynthesis' in window)) {
      return;
    }
    const synth = window.speechSynthesis;
    const refresh = () => (this.availableVoices = synth.getVoices());
    refresh();
    synth.onvoiceschanged = refresh;
  }

  private pickPreferredVoiceForLang(lang: string): SpeechSynthesisVoice | undefined {
    const prefix = (lang || 'en').toLowerCase().slice(0, 2);
    const candidates = this.availableVoices.filter(v => (v.lang || '').toLowerCase().startsWith(prefix));
    if (!candidates.length) {
      return undefined;
    }
    if (prefix === 'en') {
      const feminineHint = ['female', 'woman', 'zira', 'aria', 'samantha', 'victoria', 'karen'];
      const femaleVoice = candidates.find(v => {
        const n = (v.name || '').toLowerCase();
        return feminineHint.some(h => n.includes(h));
      });
      return femaleVoice || candidates[0];
    }
    return candidates[0];
  }

  private defaultBcp47ForLang(iso2: string): string {
    const map: Record<string, string> = {
      en: 'en-US',
      fr: 'fr-FR',
      ar: 'ar-SA',
      es: 'es-ES',
      de: 'de-DE',
      it: 'it-IT',
      pt: 'pt-PT',
      tr: 'tr-TR',
      nl: 'nl-NL',
      pl: 'pl-PL',
      ru: 'ru-RU',
      ja: 'ja-JP',
      zh: 'zh-CN',
      hi: 'hi-IN',
      vi: 'vi-VN',
      ko: 'ko-KR',
      sv: 'sv-SE',
      da: 'da-DK',
      no: 'nb-NO',
      fi: 'fi-FI'
    };
    return map[iso2.toLowerCase()] || `${iso2}-${iso2.toUpperCase()}`;
  }

  private speakTranslated(text: string, langCode: string): void {
    this.stopReading();
    const iso = (langCode || 'en').toLowerCase().slice(0, 2);
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = this.defaultBcp47ForLang(iso);
    utterance.rate = 0.92;
    utterance.pitch = iso === 'en' ? 1.08 : 1.0;
    const preferredVoice = this.pickPreferredVoiceForLang(iso);
    if (preferredVoice) {
      utterance.voice = preferredVoice;
      utterance.lang = preferredVoice.lang || utterance.lang;
    }
    this.selectedUtterance = utterance;
    window.speechSynthesis.speak(utterance);
  }
}
