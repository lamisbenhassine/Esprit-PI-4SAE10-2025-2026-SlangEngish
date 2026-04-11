import {
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  ViewChild
} from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TextAiService } from '../../core/services/text-ai.service';

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
    this.textAi.translateToEnglish(this.selectedText).subscribe({
      next: translated => {
        this.translating = false;
        this.speakEnglish(translated || this.selectedText);
      },
      error: () => {
        this.translating = false;
        this.speakEnglish(this.selectedText);
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

  private pickPreferredEnglishFemaleVoice(): SpeechSynthesisVoice | undefined {
    const englishVoices = this.availableVoices.filter(v =>
      (v.lang || '').toLowerCase().startsWith('en')
    );
    if (!englishVoices.length) {
      return undefined;
    }
    const feminineHint = ['female', 'woman', 'zira', 'aria', 'samantha', 'victoria', 'karen'];
    const femaleVoice = englishVoices.find(v => {
      const n = (v.name || '').toLowerCase();
      return feminineHint.some(h => n.includes(h));
    });
    return femaleVoice || englishVoices[0];
  }

  private speakEnglish(text: string): void {
    this.stopReading();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'en-US';
    utterance.rate = 0.92;
    utterance.pitch = 1.1;
    const preferredVoice = this.pickPreferredEnglishFemaleVoice();
    if (preferredVoice) {
      utterance.voice = preferredVoice;
      utterance.lang = preferredVoice.lang || utterance.lang;
    }
    this.selectedUtterance = utterance;
    window.speechSynthesis.speak(utterance);
  }
}
