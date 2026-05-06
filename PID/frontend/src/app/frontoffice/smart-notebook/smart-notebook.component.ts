import { isPlatformBrowser } from '@angular/common';
import { Component, Inject, NgZone, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { CurrentUserService } from '../../core/services/current-user.service';
import {
  DictionaryResult,
  GrammarResult,
  NotebookApiService,
  NotebookDashboard,
  NotebookGameDetail,
  NotebookGameHintResponse,
  NotebookGameProgressRow,
  NotebookGameSubmitAnswerResponse,
  NotebookGameSummary,
  NotebookNote,
  PronunciationCoachResult,
  SummaryResult
} from '../../core/services/notebook-api.service';

@Component({
  selector: 'app-smart-notebook',
  templateUrl: './smart-notebook.component.html',
  styleUrls: ['./smart-notebook.component.css']
})
export class SmartNotebookComponent implements OnInit, OnDestroy {
  readonly Math = Math;
  tabIndex = 0;
  userId = 1;
  notes: NotebookNote[] = [];
  selected: NotebookNote | null = null;
  titleDraft = '';
  contentDraft = '';
  /** Bubble editor: each bubble is a word/line/paragraph the user adds. */
  bubbles: string[] = [];
  bubbleSelectedIndex: number | null = null;
  loading = false;
  dictWord = '';
  dictResult: DictionaryResult | null = null;
  summaryResult: SummaryResult | null = null;
  dashboard: NotebookDashboard | null = null;
  sharedNotes: NotebookNote[] = [];
  sharedLoading = false;
  sharedQuery = '';
  sharedSort: 'recent' | 'score' = 'recent';
  sharedExpandedShareId: string | null = null;
  /** Mic on for speech-to-text into the note */
  micActive = false;
  /** 'dictate' = type into note; 'coach' = separate buffer for AI coach */
  micRoute: 'dictate' | 'coach' = 'dictate';
  coachHeard = '';
  coachGoalText = '';
  coachFeedback: PronunciationCoachResult | null = null;
  /** Grammar pass on the same coach transcript (Ollama / LanguageTool). */
  coachGrammarResult: GrammarResult | null = null;
  /** Heard text snapshot used for last analysis (for replay + grammar compare). */
  coachHeardWhenAnalyzed = '';
  coachLoading = false;
  /** After analysis, speak a short voice reply (no long tip lists). */
  coachSpeakFeedback = true;
  /** Min characters in coach transcript before auto-run when you stop the mic. */
  private readonly coachMinHeard = 12;
  /** Browser text-to-speech (SpeechSynthesis) is active */
  speaking = false;
  private recognition: { start: () => void; stop: () => void; lang: string; continuous: boolean; interimResults: boolean; onresult: ((ev: unknown) => void) | null; onerror: (() => void) | null; onend: (() => void) | null } | null = null;
  /** Snapshot of note text when dictate mic starts (prevents duplicated interim appends). */
  private dictateStartText = '';
  /** Snapshot of coach buffer when coach mic starts. */
  private coachStartText = '';

  // ----- Games -----
  games: NotebookGameSummary[] = [];
  gamesLoading = false;
  selectedGame: NotebookGameDetail | null = null;
  gameProgress: Record<number, NotebookGameProgressRow> = {};
  gameHintByEntryId: Record<number, NotebookGameHintResponse> = {};
  gameAnswerDraft: Record<number, string> = {};
  /** Crossword grid (row:col -> char). */
  cwCells: Record<string, string> = {};
  cwSelectedEntryId: number | null = null;
  gameWorkingEntryId: number | null = null;
  gamesNewCount = 0;
  private readonly gamesSeenKey = 'notebook.games.lastSeenAt';

  constructor(
    private api: NotebookApiService,
    private snack: MatSnackBar,
    private auth: AuthService,
    private currentUser: CurrentUserService,
    private zone: NgZone,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit(): void {
    const u = this.auth.getCurrentUser();
    if (u?.id != null) {
      this.currentUser.setUserId(u.id);
      this.userId = u.id;
    } else {
      this.userId = this.currentUser.getUserId();
    }
    this.loadNotes();
    this.loadDashboard();
    this.loadSharedNotes();
    this.loadGames();
    if (isPlatformBrowser(this.platformId)) {
      this.initSpeech();
    }
  }

  ngOnDestroy(): void {
    try {
      this.recognition?.stop();
    } catch {
      /* ignore */
    }
    this.stopSpeaking();
  }

  private initSpeech(): void {
    if (typeof window === 'undefined') return;
    const w = window as unknown as { webkitSpeechRecognition?: new () => unknown; SpeechRecognition?: new () => unknown };
    const Ctor = w.SpeechRecognition || w.webkitSpeechRecognition;
    if (!Ctor) return;
    this.recognition = new Ctor() as typeof this.recognition;
    if (!this.recognition) return;
    this.recognition.lang = 'en-US';
    this.recognition.continuous = true;
    this.recognition.interimResults = true;
    this.recognition.onresult = (ev: unknown) => {
      const e = ev as { resultIndex: number; results: { length: number; [i: number]: { [0]: { transcript: string } } } };
      let transcript = '';
      for (let i = 0; i < e.results.length; i++) {
        transcript += e.results[i][0].transcript;
      }
      transcript = transcript.trim().replace(/\s+/g, ' ');
      this.zone.run(() => {
        if (this.micRoute === 'dictate') {
          const base = (this.dictateStartText ?? '').trim();
          this.contentDraft = (base ? `${base} ${transcript}` : transcript).trim();
        } else {
          const base = (this.coachStartText ?? '').trim();
          this.coachHeard = (base ? `${base} ${transcript}` : transcript).trim();
        }
      });
    };
    this.recognition.onerror = () => {
      this.zone.run(() => {
        this.micActive = false;
        this.snack.open('Voice input error. Try Chrome and allow microphone.', 'Close', { duration: 4000 });
      });
    };
    this.recognition.onend = () => {
      this.zone.run(() => {
        this.micActive = false;
      });
    };
  }

  loadNotes(): void {
    this.loading = true;
    this.api.listNotes(this.userId).subscribe({
      next: (list) => {
        this.notes = list;
        this.loading = false;
        if (!this.selected && list.length) this.selectNote(list[0]);
      },
      error: () => {
        this.loading = false;
        this.snack.open('Could not load notes. Is gateway + notebook running?', 'Close', { duration: 4000 });
      }
    });
  }

  loadSharedNotes(): void {
    this.sharedLoading = true;
    this.api.listSharedNotes(this.userId).subscribe({
      next: (list) => {
        this.sharedNotes = list ?? [];
        this.sharedLoading = false;
      },
      error: () => {
        this.sharedLoading = false;
        this.sharedNotes = [];
      }
    });
  }

  get sharedNotesView(): NotebookNote[] {
    const q = (this.sharedQuery ?? '').trim().toLowerCase();
    let list = (this.sharedNotes ?? []).slice();
    if (q) {
      list = list.filter((n) => {
        const t = `${n.title ?? ''} ${n.content ?? ''}`.toLowerCase();
        return t.includes(q);
      });
    }
    if (this.sharedSort === 'score') {
      list.sort((a, b) => (b.shareScore ?? 0) - (a.shareScore ?? 0));
    } else {
      list.sort((a, b) => {
        const ta = new Date(a.updatedAt ?? a.createdAt ?? 0).getTime();
        const tb = new Date(b.updatedAt ?? b.createdAt ?? 0).getTime();
        return tb - ta;
      });
    }
    return list;
  }

  loadDashboard(): void {
    this.api.dashboard(this.userId).subscribe({
      next: (d) => (this.dashboard = d),
      error: () => {}
    });
  }

  selectNote(n: NotebookNote): void {
    this.selected = n;
    this.titleDraft = n.title;
    this.contentDraft = n.content ?? '';
    this.bubbles = [];
    this.bubbleSelectedIndex = null;
    this.dictResult = null;
    this.summaryResult = null;
    this.clearCoachUi();
  }

  newNote(): void {
    this.selected = null;
    this.titleDraft = 'New note';
    this.contentDraft = '';
    this.bubbles = [];
    this.bubbleSelectedIndex = null;
    this.dictResult = null;
    this.summaryResult = null;
    this.clearCoachUi();
  }

  private clearCoachUi(): void {
    this.coachHeard = '';
    this.coachFeedback = null;
    this.coachGrammarResult = null;
    this.coachHeardWhenAnalyzed = '';
  }

  saveNote(): void {
    if (this.selected?.id != null) {
      this.api.updateNote(this.userId, this.selected.id, this.titleDraft, this.contentDraft).subscribe({
        next: (n) => {
          this.snack.open('Saved', 'OK', { duration: 2000 });
          this.selected = n;
          this.loadNotes();
          this.loadDashboard();
          this.loadSharedNotes();
        },
        error: () => this.snack.open('Save failed', 'Close', { duration: 3000 })
      });
    } else {
      this.api.createNote(this.userId, this.titleDraft, this.contentDraft).subscribe({
        next: (n) => {
          this.snack.open('Created', 'OK', { duration: 2000 });
          this.selected = n;
          this.loadNotes();
          this.loadDashboard();
          this.loadSharedNotes();
        },
        error: () => this.snack.open('Create failed', 'Close', { duration: 3000 })
      });
    }
  }

  deleteNote(): void {
    if (!this.selected?.id) return;
    this.api.deleteNote(this.userId, this.selected.id).subscribe({
      next: () => {
        this.snack.open('Deleted', 'OK', { duration: 2000 });
        this.newNote();
        this.loadNotes();
        this.loadDashboard();
        this.loadSharedNotes();
      },
      error: () => this.snack.open('Delete failed', 'Close', { duration: 3000 })
    });
  }

  shareSelectedNote(): void {
    if (!this.selected?.id) {
      this.snack.open('Select a saved note first.', 'Close', { duration: 2500 });
      return;
    }
    this.api.shareNote(this.userId, this.selected.id).subscribe({
      next: (n) => {
        this.selected = n;
        this.snack.open('Shared to students', 'OK', { duration: 2500 });
        this.loadNotes();
        this.loadSharedNotes();
      },
      error: () => this.snack.open('Share failed', 'Close', { duration: 3000 })
    });
  }

  unshareSelectedNote(): void {
    if (!this.selected?.id) return;
    this.api.unshareNote(this.userId, this.selected.id).subscribe({
      next: (n) => {
        this.selected = n;
        this.snack.open('Unshared', 'OK', { duration: 2500 });
        this.loadNotes();
        this.loadSharedNotes();
      },
      error: () => this.snack.open('Unshare failed', 'Close', { duration: 3000 })
    });
  }

  importSharedNote(n: NotebookNote): void {
    const sid = (n.shareId ?? '').trim();
    if (!sid) {
      this.snack.open('This shared note has no shareId.', 'Close', { duration: 3000 });
      return;
    }
    this.api.importSharedNote(this.userId, sid).subscribe({
      next: (created) => {
        this.snack.open('Added to your notebook', 'OK', { duration: 2500 });
        this.selectNote(created);
        this.loadNotes();
        this.loadDashboard();
      },
      error: () => this.snack.open('Import failed', 'Close', { duration: 3000 })
    });
  }

  toggleSharedPreview(n: NotebookNote): void {
    const sid = (n.shareId ?? '').trim();
    if (!sid) return;
    this.sharedExpandedShareId = this.sharedExpandedShareId === sid ? null : sid;
  }

  runGrammar(): void {
    if (this.bubbleSelectedIndex != null) {
      this.runGrammarOnSelectedBubble();
      return;
    }
    if (!this.contentDraft.trim()) return;
    this.api.grammar(this.contentDraft).subscribe({
      next: (r: GrammarResult) => {
        this.contentDraft = r.correctedText;
        const via = r.source === 'ollama' ? 'Ollama (local)' : r.source === 'languagetool' ? 'LanguageTool' : 'Grammar';
        this.snack.open(`${via}: ${r.issuesFixed} change(s)`, 'OK', { duration: 3000 });
      },
      error: () => this.snack.open('Grammar failed', 'Close', { duration: 4000 })
    });
  }

  runSummarize(): void {
    if (this.bubbleSelectedIndex != null) {
      this.runSummarizeOnSelectedBubble();
      return;
    }
    if (!this.contentDraft.trim()) return;
    this.api.summarize(this.contentDraft).subscribe({
      next: (r: SummaryResult) => (this.summaryResult = r),
      error: () => this.snack.open('Summary failed', 'Close', { duration: 3000 })
    });
  }

  runDictionary(): void {
    if (!this.dictWord.trim()) return;
    this.api.dictionary(this.dictWord.trim(), this.contentDraft.slice(0, 200)).subscribe({
      next: (r) => (this.dictResult = r),
      error: () => this.snack.open('Dictionary lookup failed', 'Close', { duration: 3000 })
    });
  }

  toggleDictate(): void {
    if (!this.recognition) {
      this.snack.open('Voice not supported in this browser', 'Close', { duration: 3000 });
      return;
    }
    if (this.micActive && this.micRoute === 'dictate') {
      try { this.recognition.stop(); } catch { /* ignore */ }
      this.micActive = false;
      return;
    }
    if (this.micActive && this.micRoute === 'coach') {
      try { this.recognition.stop(); } catch { /* ignore */ }
      this.micActive = false;
    }
    this.micRoute = 'dictate';
    this.dictateStartText = (this.contentDraft ?? '').trim();
    this.micActive = true;
    try { this.recognition.start(); } catch {
      this.micActive = false;
      this.snack.open('Could not start microphone', 'Close', { duration: 3000 });
    }
  }

  toggleCoachMic(host: HTMLTextAreaElement): void {
    if (!this.recognition) {
      this.snack.open('Voice not supported in this browser', 'Close', { duration: 3000 });
      return;
    }
    if (this.micActive && this.micRoute === 'coach') {
      try { this.recognition.stop(); } catch { /* ignore */ }
      this.micActive = false;
      setTimeout(() => {
        const h = (this.coachHeard ?? '').trim();
        if (h.length >= this.coachMinHeard) {
          this.runCoachPipeline(host, { autoFromMicStop: true });
        }
      }, 450);
      return;
    }
    if (this.micActive && this.micRoute === 'dictate') {
      try { this.recognition.stop(); } catch { /* ignore */ }
      this.micActive = false;
    }
    this.micRoute = 'coach';
    this.coachHeard = '';
    this.coachStartText = '';
    this.coachFeedback = null;
    this.coachGrammarResult = null;
    this.coachHeardWhenAnalyzed = '';
    this.micActive = true;
    try { this.recognition.start(); } catch {
      this.micActive = false;
      this.snack.open('Could not start coach microphone', 'Close', { duration: 3000 });
    }
  }

  resolveCoachTarget(host: HTMLTextAreaElement): string {
    const goal = (this.coachGoalText ?? '').trim();
    if (goal) return goal;
    const lo = Math.min(host.selectionStart, host.selectionEnd);
    const hi = Math.max(host.selectionStart, host.selectionEnd);
    const sel = host.value.slice(lo, hi).trim();
    return sel;
  }

  speakCoachTarget(host: HTMLTextAreaElement): void {
    const t = this.resolveCoachTarget(host);
    if (!t) {
      this.snack.open('Type a practice line above or select text in your note.', 'Close', { duration: 3500 });
      return;
    }
    if (this.speaking) this.stopSpeaking();
    this.speakText(t);
  }

  coachGrammarShowOnScreen(gr: GrammarResult | null | undefined): boolean {
    if (!gr?.correctedText?.trim()) return false;
    if ((gr.issuesFixed ?? 0) > 0) return true;
    const heard = (this.coachHeardWhenAnalyzed || this.coachHeard || '').trim().replace(/\s+/g, ' ');
    const corr = gr.correctedText.trim().replace(/\s+/g, ' ');
    return heard.length > 0 && heard !== corr;
  }

  runCoachAnalysis(host: HTMLTextAreaElement): void {
    const heard = (this.coachHeard ?? '').trim();
    if (!heard) {
      this.snack.open('Use Coach mic first to capture what you said.', 'Close', { duration: 3500 });
      return;
    }
    this.runCoachPipeline(host, { autoFromMicStop: false });
  }

  private runCoachPipeline(host: HTMLTextAreaElement, opts: { autoFromMicStop: boolean }): void {
    const heard = (this.coachHeard ?? '').trim();
    if (!heard) {
      if (!opts.autoFromMicStop) {
        this.snack.open('Use Coach mic first to capture what you said.', 'Close', { duration: 3500 });
      }
      return;
    }
    if (opts.autoFromMicStop && heard.length < this.coachMinHeard) return;
    const target = this.resolveCoachTarget(host);
    this.coachLoading = true;
    this.coachFeedback = null;
    this.coachGrammarResult = null;

    const grammar$ = this.api.grammar(heard).pipe(catchError(() => of(null)));
    const coach$ = this.api.pronunciationCoach(target, heard).pipe(catchError(() => of(null)));

    forkJoin({ grammar: grammar$, coach: coach$ }).subscribe({
      next: ({ grammar, coach }) => {
        this.coachGrammarResult = grammar;
        this.coachFeedback = this.normalizeCoachResponse(coach);
        this.coachHeardWhenAnalyzed = heard;
        this.coachLoading = false;
        if (!coach && !grammar) {
          this.snack.open('Could not reach coach or grammar. Check gateway + notebook.', 'Close', { duration: 4500 });
          return;
        }
        if (this.coachSpeakFeedback) {
          setTimeout(() => this.speakCoachScriptShort(coach, grammar, heard), 200);
        }
      },
      error: () => {
        this.coachLoading = false;
        this.snack.open('Coach request failed.', 'Close', { duration: 4500 });
      }
    });
  }

  /**
   * Backend may return `rawCoachText` containing JSON-like content (sometimes missing outer braces).
   * If so, parse and hydrate fields so the UI shows structured cards instead of raw JSON.
   */
  private normalizeCoachResponse(coach: PronunciationCoachResult | null): PronunciationCoachResult | null {
    if (!coach) return null;
    const raw = (coach.rawCoachText ?? '').trim();
    if (!raw) return coach;

    const parsed = this.tryParseCoachRawJson(raw);
    if (!parsed) return coach;

    const hadParseErrorSummary = (coach.overallSummary ?? '').toLowerCase().includes('could not parse');

    return {
      overallSummary: (parsed.overallSummary ?? (hadParseErrorSummary ? '' : coach.overallSummary) ?? '').toString(),
      idealSentence: parsed.idealSentence ?? coach.idealSentence ?? null,
      items: parsed.items ?? coach.items ?? [],
      overallTips: parsed.overallTips ?? coach.overallTips ?? [],
      rawCoachText: coach.rawCoachText ?? null
    };
  }

  private tryParseCoachRawJson(raw: string): Partial<PronunciationCoachResult> | null {
    try {
      let t = raw.trim();
      // Strip markdown code fences if present
      t = t.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/i, '').trim();
      // If the backend returns a fragment starting with a quoted key, wrap it
      if (t.startsWith('"') && !t.startsWith('{"')) {
        t = `{${t}}`;
      }
      // If it looks like an object fragment without braces, wrap it
      if (!t.startsWith('{') && (t.includes('"idealSentence"') || t.includes('"items"') || t.includes('"overallTips"'))) {
        t = `{${t}}`;
      }
      // Remove trailing commas before } or ]
      t = t.replace(/,\s*([}\]])/g, '$1');

      const obj = JSON.parse(t) as Record<string, unknown>;

      const idealSentence =
        typeof obj['idealSentence'] === 'string' ? (obj['idealSentence'] as string) : null;
      const overallSummary =
        typeof obj['overallSummary'] === 'string' ? (obj['overallSummary'] as string) : null;

      const itemsRaw = Array.isArray(obj['items']) ? (obj['items'] as unknown[]) : null;
      const items =
        itemsRaw?.map((it) => {
          const x = (it ?? {}) as Record<string, unknown>;
          return {
            issue: typeof x['issue'] === 'string' ? (x['issue'] as string) : '',
            correction: typeof x['correction'] === 'string' ? (x['correction'] as string) : '',
            tip: typeof x['tip'] === 'string' ? (x['tip'] as string) : ''
          };
        }).filter((x) => x.issue || x.correction || x.tip) ?? null;

      const tipsRaw = Array.isArray(obj['overallTips']) ? (obj['overallTips'] as unknown[]) : null;
      const overallTips =
        tipsRaw?.map((x) => (typeof x === 'string' ? x : '')).filter((s) => s.trim().length > 0) ?? null;

      // If we couldn't extract anything useful, ignore.
      if (!idealSentence && !overallSummary && !items?.length && !overallTips?.length) return null;

      return {
        idealSentence: idealSentence ?? undefined,
        overallSummary: overallSummary ?? undefined,
        items: items ?? undefined,
        overallTips: overallTips ?? undefined
      };
    } catch {
      return null;
    }
  }

  replayCoachVoice(): void {
    if (!this.coachFeedback && !this.coachGrammarResult) {
      this.snack.open('Stop the coach mic or tap Analyze first.', 'Close', { duration: 3000 });
      return;
    }
    const heard = (this.coachHeardWhenAnalyzed || this.coachHeard || '').trim();
    this.speakCoachScriptShort(this.coachFeedback, this.coachGrammarResult, heard);
  }

  speakIdealSentence(): void {
    const s = (this.coachFeedback?.idealSentence ?? '').trim();
    if (!s) {
      this.snack.open('No ideal sentence from coach yet.', 'Close', { duration: 2500 });
      return;
    }
    if (this.speaking) this.stopSpeaking();
    this.speakText(s);
  }

  private speakCoachScriptShort(
    cf: PronunciationCoachResult | null,
    grammar: GrammarResult | null,
    heard: string
  ): void {
    if (!this.ttsAvailable()) {
      this.snack.open('Voice coach needs speech synthesis (try Chrome).', 'Close', { duration: 4000 });
      return;
    }
    const phrases = this.buildCoachVoicePhrasesShort(cf, grammar, heard);
    if (!phrases.length) {
      this.snack.open('Nothing to say aloud — check the text on screen.', 'Close', { duration: 2500 });
      return;
    }
    this.stopSpeaking();
    this.speakUtteranceQueue(phrases);
  }

  /** Speak several phrases one after another (one short "conversation" with the coach). */
  private speakUtteranceQueue(phrases: string[], index = 0): void {
    if (!this.ttsAvailable()) {
      return;
    }
    if (index === 0) {
      window.speechSynthesis.cancel();
      this.zone.run(() => {
        this.speaking = true;
      });
    }
    let next = index;
    while (next < phrases.length && !phrases[next]?.trim()) {
      next++;
    }
    if (next >= phrases.length) {
      this.zone.run(() => {
        this.speaking = false;
      });
      return;
    }
    const text = phrases[next].trim();
    const u = new SpeechSynthesisUtterance(text);
    u.lang = 'en-US';
    u.rate = 0.95;
    u.pitch = 1;
    u.onend = () => {
      this.zone.run(() => this.speakUtteranceQueue(phrases, next + 1));
    };
    u.onerror = () => {
      this.zone.run(() => {
        this.speaking = false;
      });
    };
    window.speechSynthesis.speak(u);
  }

  private buildCoachVoicePhrasesShort(
    cf: PronunciationCoachResult | null,
    grammar: GrammarResult | null,
    heard: string
  ): string[] {
    const parts: string[] = ["Hey, I'm your speaking coach."];
    const fix = this.grammarPhraseForVoice(grammar, heard);
    if (fix) {
      parts.push('Grammar — here is the correct phrase.');
      parts.push(`Listen: ${fix}.`);
      parts.push(`Again: ${fix}.`);
    }
    if (cf?.items?.length) {
      const c0 = cf.items[0]?.correction?.trim();
      if (c0) {
        parts.push('Pronunciation — listen to the fix.');
        parts.push(`Listen: ${c0}.`);
        parts.push(`Again: ${c0}.`);
      }
    } else if (cf?.idealSentence?.trim()) {
      parts.push('Line to practice.');
      parts.push(`Listen: ${cf.idealSentence.trim()}.`);
      parts.push(`Again: ${cf.idealSentence.trim()}.`);
    }
    return parts.map((p) => p.trim()).filter((p) => p.length > 0);
  }

  private grammarPhraseForVoice(grammar: GrammarResult | null, heard: string): string | null {
    if (!grammar?.correctedText?.trim()) return null;
    const a = heard.trim().replace(/\s+/g, ' ');
    const b = grammar.correctedText.trim().replace(/\s+/g, ' ');
    if (a === b) return null;
    let fix = grammar.correctedText.trim();
    if (fix.length > 140) fix = fix.slice(0, 137).replace(/\s+\S*$/, '') + '…';
    return fix;
  }

  summaryParagraphs(text: string | null | undefined): string[] {
    if (!text?.trim()) return [];
    const t = text.trim();
    if (/\n/.test(t)) return t.split(/\n+/).map((s) => s.trim()).filter((s) => s.length > 0);
    if (t.length > 220) return t.split(/(?<=[.!?])\s+/).map((s) => s.trim()).filter((s) => s.length > 0);
    return [t];
  }

  private ttsAvailable(): boolean {
    return isPlatformBrowser(this.platformId) && typeof window !== 'undefined' && !!window.speechSynthesis;
  }

  stopSpeaking(): void {
    if (!this.ttsAvailable()) {
      this.speaking = false;
      return;
    }
    window.speechSynthesis.cancel();
    this.speaking = false;
  }

  toggleReadAloud(): void {
    if (this.speaking) this.stopSpeaking();
    else this.speakPage();
  }

  speakPage(): void {
    if (!this.ttsAvailable()) {
      this.snack.open('Read-aloud needs speech synthesis (e.g. Chrome).', 'Close', { duration: 4000 });
      return;
    }
    const title = (this.titleDraft ?? '').trim();
    const body = (this.contentDraft ?? '').trim();
    const parts: string[] = [];
    if (title) parts.push(title);
    if (body) parts.push(body);
    const text = parts.join('. ');
    if (!text) {
      this.snack.open('Write something to listen.', 'Close', { duration: 2500 });
      return;
    }
    if (this.speaking) this.stopSpeaking();
    this.speakText(text);
  }

  speakSelection(host: HTMLTextAreaElement): void {
    if (!this.ttsAvailable()) {
      this.snack.open('Read-aloud not supported in this environment.', 'Close', { duration: 3000 });
      return;
    }
    const lo = Math.min(host.selectionStart, host.selectionEnd);
    const hi = Math.max(host.selectionStart, host.selectionEnd);
    const text = host.value.slice(lo, hi).trim();
    if (!text) {
      this.snack.open('Select text in the note first, then tap Selection.', 'Close', { duration: 3500 });
      return;
    }
    if (this.speaking) this.stopSpeaking();
    this.speakText(text);
  }

  private speakText(text: string): void {
    if (!this.ttsAvailable()) return;
    const max = 32000;
    const chunk = text.length > max ? text.slice(0, max) : text;
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(chunk);
    u.lang = 'en-US';
    u.rate = 1;
    u.pitch = 1;
    u.onend = () => this.zone.run(() => (this.speaking = false));
    u.onerror = () => this.zone.run(() => (this.speaking = false));
    this.speaking = true;
    window.speechSynthesis.speak(u);
  }

  insertSummaryIntoNote(): void {
    if (!this.summaryResult?.summary) return;
    this.contentDraft = (this.contentDraft + '\n\n--- Summary ---\n' + this.summaryResult.summary).trim();
  }

  bubbleAccent(i: number): string {
    const hues = [223, 198, 265, 155, 28];
    const h = hues[i % hues.length];
    return `hsl(${h} 85% 45%)`;
  }

  selectBubble(idx: number): void {
    this.bubbleSelectedIndex = idx;
  }

  makeBubbleFromSelection(host: HTMLTextAreaElement): void {
    const sel = this.readSelectionOrParagraph(host);
    if (!sel) {
      this.snack.open('Select a word or paragraph first (or place cursor inside a paragraph).', 'Close', { duration: 3500 });
      return;
    }
    const normalized = sel.trim().replace(/\s+/g, ' ');
    if (!normalized) return;
    this.bubbles = [...this.bubbles, normalized];
    this.bubbleSelectedIndex = this.bubbles.length - 1;
    this.removeSelectionOrParagraphFromEditor(host);
  }

  removeSelectedBubble(): void {
    if (this.bubbleSelectedIndex == null) return;
    const idx = this.bubbleSelectedIndex;
    this.bubbles = this.bubbles.filter((_, i) => i !== idx);
    this.bubbleSelectedIndex = this.bubbles.length ? Math.min(idx, this.bubbles.length - 1) : null;
  }

  private runGrammarOnSelectedBubble(): void {
    if (this.bubbleSelectedIndex == null) return;
    const src = (this.bubbles[this.bubbleSelectedIndex] ?? '').trim();
    if (!src) return;
    this.api.grammar(src).subscribe({
      next: (r: GrammarResult) => {
        const next = [...this.bubbles];
        next[this.bubbleSelectedIndex!] = r.correctedText?.trim() || src;
        this.bubbles = next;
        this.snack.open('Grammar: bubble corrected', 'OK', { duration: 2500 });
      },
      error: () => this.snack.open('Grammar failed', 'Close', { duration: 4000 })
    });
  }

  private runSummarizeOnSelectedBubble(): void {
    if (this.bubbleSelectedIndex == null) return;
    const src = (this.bubbles[this.bubbleSelectedIndex] ?? '').trim();
    if (!src) return;
    this.api.summarize(src).subscribe({
      next: (r: SummaryResult) => {
        this.summaryResult = r;
        const s = (r?.summary ?? '').trim();
        if (!s) return;
        const next = [...this.bubbles];
        next[this.bubbleSelectedIndex!] = s;
        this.bubbles = next;
        this.snack.open('Summary: bubble summarized', 'OK', { duration: 2500 });
      },
      error: () => this.snack.open('Summary failed', 'Close', { duration: 3000 })
    });
  }

  private readSelectionOrParagraph(host: HTMLTextAreaElement): string {
    const lo = Math.min(host.selectionStart, host.selectionEnd);
    const hi = Math.max(host.selectionStart, host.selectionEnd);
    const raw = host.value ?? '';
    const selection = raw.slice(lo, hi).trim();
    if (selection) return selection;

    // If the user didn't select anything, prefer the single word at cursor.
    // This ensures "bubble + Grammar" corrects only the wrong word (not the whole paragraph).
    const w = this.wordBoundsAt(raw, lo);
    if (w) {
      const word = raw.slice(w.start, w.end).trim();
      if (word) return word;
    }

    const p = this.currentParagraphBounds(raw, lo);
    return raw.slice(p.start, p.end).trim();
  }

  /**
   * Returns word bounds around a cursor position for bubble extraction.
   * Used so that when no selection exists, Grammar corrects only that word.
   */
  private wordBoundsAt(text: string, pos: number): { start: number; end: number } | null {
    const t = text ?? '';
    if (!t.length) return null;
    const len = t.length;

    const isWordChar = (ch: string): boolean => /[A-Za-z0-9'-]/.test(ch) || ch === '’';

    // Choose a "center" character: cursor may sit at end of a word.
    const center =
      pos < len && isWordChar(t.charAt(pos))
        ? pos
        : pos > 0 && isWordChar(t.charAt(pos - 1))
          ? pos - 1
          : -1;

    if (center < 0) return null;

    let start = center;
    while (start > 0 && isWordChar(t.charAt(start - 1))) start--;

    let end = center + 1; // exclusive
    while (end < len && isWordChar(t.charAt(end))) end++;

    const word = t.slice(start, end).trim();
    if (!word) return null;
    return { start, end };
  }

  private removeSelectionOrParagraphFromEditor(host: HTMLTextAreaElement): void {
    const raw = host.value ?? '';
    const lo = Math.min(host.selectionStart, host.selectionEnd);
    const hi = Math.max(host.selectionStart, host.selectionEnd);
    let start = lo;
    let end = hi;
    if (start === end) {
      // Same rule as extraction: if nothing is selected, remove only the word at cursor.
      const w = this.wordBoundsAt(raw, lo);
      if (w) {
        start = w.start;
        end = w.end;
      } else {
        const p = this.currentParagraphBounds(raw, lo);
        start = p.start;
        end = p.end;
      }
    }
    const next = (raw.slice(0, start) + raw.slice(end)).replace(/\n{3,}/g, '\n\n').trim();
    this.contentDraft = next;
    host.value = next;
  }

  private currentParagraphBounds(text: string, pos: number): { start: number; end: number } {
    const t = text ?? '';
    const p = Math.max(0, Math.min(pos, t.length));
    const before = t.lastIndexOf('\n\n', p);
    const after = t.indexOf('\n\n', p);
    const start = before === -1 ? 0 : before + 2;
    const end = after === -1 ? t.length : after;
    return { start, end };
  }

  // ----- Games UI -----
  private safeNowMs(): number {
    return Date.now();
  }

  private lastGamesSeenAt(): number {
    if (!isPlatformBrowser(this.platformId)) return 0;
    try {
      const v = localStorage.getItem(this.gamesSeenKey);
      const n = v ? Number(v) : 0;
      return Number.isFinite(n) ? n : 0;
    } catch {
      return 0;
    }
  }

  markGamesSeenNow(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      localStorage.setItem(this.gamesSeenKey, String(this.safeNowMs()));
    } catch {
      /* ignore */
    }
    this.gamesNewCount = 0;
  }

  loadGames(): void {
    this.gamesLoading = true;
    const seen = this.lastGamesSeenAt();
    this.api.listPublishedGames().subscribe({
      next: (list) => {
        this.games = (list ?? []).slice();
        const seenMs = seen || 0;
        this.gamesNewCount =
          isPlatformBrowser(this.platformId) && seenMs > 0
            ? this.games.filter((g) => new Date(g.createdAt ?? 0).getTime() > seenMs).length
            : this.games.length > 0 && seenMs === 0
              ? Math.min(3, this.games.length)
              : 0;
        this.gamesLoading = false;
      },
      error: () => {
        this.gamesLoading = false;
        this.games = [];
      }
    });
  }

  selectGame(g: NotebookGameSummary): void {
    this.selectedGame = null;
    this.gameProgress = {};
    this.gameHintByEntryId = {};
    this.gameAnswerDraft = {};
    this.cwCells = {};
    this.cwSelectedEntryId = null;
    this.gameWorkingEntryId = null;
    this.api.gameDetail(g.id).subscribe({
      next: (detail) => {
        this.selectedGame = detail;
        this.loadGameProgress(detail.id);
        const first = (detail.entries ?? []).find((e) => (e.row ?? null) != null && (e.col ?? null) != null && !!e.dir);
        if (first?.id != null) {
          this.cwSelectedEntryId = first.id;
          setTimeout(() => this.focusFirstEmptyCell(first.id), 0);
        }
        this.markGamesSeenNow();
      },
      error: () => this.snack.open('Could not open game.', 'Close', { duration: 3500 })
    });
  }

  backToGamesList(): void {
    this.selectedGame = null;
    this.gameHintByEntryId = {};
    this.gameAnswerDraft = {};
    this.cwCells = {};
    this.cwSelectedEntryId = null;
    this.gameWorkingEntryId = null;
  }

  private loadGameProgress(gameId: number): void {
    this.api.gameProgress(gameId, this.userId).subscribe({
      next: (rows) => {
        const map: Record<number, NotebookGameProgressRow> = {};
        (rows ?? []).forEach((r) => (map[r.entryId] = r));
        this.gameProgress = map;
      },
      error: () => (this.gameProgress = {})
    });
  }

  entrySolved(entryId: number): boolean {
    return !!this.gameProgress?.[entryId]?.solved;
  }

  submitEntryAnswer(entryId: number): void {
    if (!this.selectedGame) return;
    const ans = this.cwEntryAnswer(entryId) || (this.gameAnswerDraft?.[entryId] ?? '').trim();
    if (!ans) return;
    this.gameWorkingEntryId = entryId;
    this.api.gameSubmitAnswer(this.selectedGame.id, entryId, this.userId, ans).subscribe({
      next: (r: NotebookGameSubmitAnswerResponse) => {
        this.gameProgress = {
          ...(this.gameProgress ?? {}),
          [entryId]: {
            entryId,
            solved: r.solved,
            attempts: r.attempts,
            hintLevel: r.hintLevel,
            lastAnswer: ans
          }
        };
        this.gameWorkingEntryId = null;
        this.snack.open(r.correct ? 'Correct!' : 'Not yet — try again.', 'Close', { duration: 2500 });
      },
      error: () => {
        this.gameWorkingEntryId = null;
        this.snack.open('Answer failed. Try again.', 'Close', { duration: 2500 });
      }
    });
  }

  requestHint(entryId: number): void {
    if (!this.selectedGame) return;
    this.gameWorkingEntryId = entryId;
    this.api.gameHint(this.selectedGame.id, entryId, this.userId).subscribe({
      next: (h) => {
        this.gameHintByEntryId = { ...(this.gameHintByEntryId ?? {}), [entryId]: h };
        const prev = this.gameProgress?.[entryId];
        this.gameProgress = {
          ...(this.gameProgress ?? {}),
          [entryId]: {
            entryId,
            solved: prev?.solved ?? false,
            attempts: prev?.attempts ?? 0,
            hintLevel: h.level ?? (prev?.hintLevel ?? 0),
            lastAnswer: prev?.lastAnswer ?? null
          }
        };
        this.gameWorkingEntryId = null;
      },
      error: () => {
        this.gameWorkingEntryId = null;
        this.snack.open('Hint failed.', 'Close', { duration: 2500 });
      }
    });
  }

  gameSolvedCount(): number {
    const game = this.selectedGame;
    if (!game?.entries?.length) return 0;
    return game.entries.filter((e) => this.entrySolved(e.id)).length;
  }

  gameTotalCount(): number {
    return this.selectedGame?.entries?.length ?? 0;
  }

  cwRows(): number {
    return Number(this.selectedGame?.gridRows ?? 0) || 0;
  }

  cwCols(): number {
    return Number(this.selectedGame?.gridCols ?? 0) || 0;
  }

  cwPlayable(r: number, c: number): boolean {
    const rows = this.selectedGame?.cellMaskRows ?? [];
    const line = rows?.[r] ?? '';
    return (line?.[c] ?? '0') === '1';
  }

  cwKey(r: number, c: number): string {
    return `${r}:${c}`;
  }

  cwDomId(r: number, c: number): string {
    return `cw-${r}-${c}`;
  }

  cwValue(r: number, c: number): string {
    return (this.cwCells?.[this.cwKey(r, c)] ?? '').toString();
  }

  cwNumberAt(r: number, c: number): number | null {
    const entries = this.selectedGame?.entries ?? [];
    const hit = entries.find((e) => Number(e.row) === r && Number(e.col) === c);
    const n = hit?.number ?? null;
    return n != null ? Number(n) : null;
  }

  cwSetValue(r: number, c: number, v: string): void {
    const ch = (v ?? '').toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 1);
    const key = this.cwKey(r, c);
    this.cwCells = { ...(this.cwCells ?? {}), [key]: ch };
  }

  cwEntriesAcross(): NotebookGameDetail['entries'] {
    return (this.selectedGame?.entries ?? []).filter((e) => (e.dir ?? '').toString().toUpperCase() === 'ACROSS');
  }

  cwEntriesDown(): NotebookGameDetail['entries'] {
    return (this.selectedGame?.entries ?? []).filter((e) => (e.dir ?? '').toString().toUpperCase() === 'DOWN');
  }

  selectCwEntry(entryId: number): void {
    this.cwSelectedEntryId = entryId;
    setTimeout(() => this.focusFirstEmptyCell(entryId), 0);
  }

  cwEntryCoords(entryId: number): Array<{ r: number; c: number }> {
    const e = (this.selectedGame?.entries ?? []).find((x) => x.id === entryId);
    if (!e) return [];
    const r0 = Number(e.row ?? -1);
    const c0 = Number(e.col ?? -1);
    const L = Number(e.answerLength ?? 0);
    const dir = (e.dir ?? '').toString().toUpperCase();
    if (r0 < 0 || c0 < 0 || !L || (dir !== 'ACROSS' && dir !== 'DOWN')) return [];
    const out: Array<{ r: number; c: number }> = [];
    for (let i = 0; i < L; i++) {
      out.push({ r: r0 + (dir === 'DOWN' ? i : 0), c: c0 + (dir === 'ACROSS' ? i : 0) });
    }
    return out;
  }

  cwIsActiveCell(r: number, c: number): boolean {
    const id = this.cwSelectedEntryId;
    if (!id) return false;
    const coords = this.cwEntryCoords(id);
    return coords.some((p) => p.r === r && p.c === c);
  }

  focusFirstEmptyCell(entryId: number): void {
    const coords = this.cwEntryCoords(entryId);
    if (!coords.length) return;
    const firstEmpty = coords.find((p) => !this.cwValue(p.r, p.c));
    const target = firstEmpty ?? coords[0];
    const id = this.cwDomId(target.r, target.c);
    (document.getElementById(id) as HTMLInputElement | null)?.focus();
  }

  cwEntryAnswer(entryId: number): string {
    const coords = this.cwEntryCoords(entryId);
    if (!coords.length) return '';
    return coords.map((p) => this.cwValue(p.r, p.c) || '').join('').trim();
  }

  onCwCellInput(r: number, c: number, ev: Event): void {
    const el = ev.target as HTMLInputElement;
    const raw = (el.value ?? '').toString();
    this.cwSetValue(r, c, raw.slice(-1));
    const entryId = this.cwSelectedEntryId;
    if (!entryId) return;
    const coords = this.cwEntryCoords(entryId);
    const idx = coords.findIndex((p) => p.r === r && p.c === c);
    if (idx >= 0 && idx < coords.length - 1) {
      const next = coords[idx + 1];
      setTimeout(() => (document.getElementById(this.cwDomId(next.r, next.c)) as HTMLInputElement | null)?.focus(), 0);
    }
  }

  onCwCellKeyDown(r: number, c: number, ev: KeyboardEvent): void {
    const entryId = this.cwSelectedEntryId;
    if (!entryId) return;
    const coords = this.cwEntryCoords(entryId);
    const idx = coords.findIndex((p) => p.r === r && p.c === c);
    if (idx < 0) return;
    if (ev.key === 'Enter') {
      ev.preventDefault();
      this.submitEntryAnswer(entryId);
      return;
    }
    if (ev.key === 'Backspace') {
      if (!this.cwValue(r, c) && idx > 0) {
        const prev = coords[idx - 1];
        setTimeout(() => (document.getElementById(this.cwDomId(prev.r, prev.c)) as HTMLInputElement | null)?.focus(), 0);
      }
      return;
    }
  }

  onCwCellClick(r: number, c: number): void {
    if (this.cwIsActiveCell(r, c)) return;
    const entries = this.selectedGame?.entries ?? [];
    const hit = entries.find((e) => {
      const coords = this.cwEntryCoords(e.id);
      return coords.some((p) => p.r === r && p.c === c);
    });
    if (hit?.id != null) {
      this.cwSelectedEntryId = hit.id;
    }
  }
}

