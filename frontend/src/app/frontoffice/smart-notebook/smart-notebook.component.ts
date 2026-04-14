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
      // Build the full "current best" transcript.
      // With interimResults=true, the browser will re-emit partial transcripts many times;
      // we must REPLACE the live buffer, not append, otherwise words repeat.
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
    // Bubbles are a separate workspace from the main notebook text.
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
    // If a bubble is selected: correct ONLY that bubble.
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
      error: () => this.snack.open('Grammar failed (try Ollama running + model pulled)', 'Close', { duration: 4000 })
    });
  }

  runSummarize(): void {
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
      try {
        this.recognition.stop();
      } catch {
        /* ignore */
      }
      this.micActive = false;
      return;
    }
    if (this.micActive && this.micRoute === 'coach') {
      try {
        this.recognition.stop();
      } catch {
        /* ignore */
      }
      this.micActive = false;
    }
    this.micRoute = 'dictate';
    this.dictateStartText = (this.contentDraft ?? '').trim();
    this.micActive = true;
    try {
      this.recognition.start();
    } catch {
      this.micActive = false;
      this.snack.open('Could not start microphone', 'Close', { duration: 3000 });
    }
  }

  /** Coach mic: listens into a separate buffer; stopping runs grammar + coach automatically. */
  toggleCoachMic(host: HTMLTextAreaElement): void {
    if (!this.recognition) {
      this.snack.open('Voice not supported in this browser', 'Close', { duration: 3000 });
      return;
    }
    if (this.micActive && this.micRoute === 'coach') {
      try {
        this.recognition.stop();
      } catch {
        /* ignore */
      }
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
      try {
        this.recognition.stop();
      } catch {
        /* ignore */
      }
      this.micActive = false;
    }
    this.micRoute = 'coach';
    this.coachHeard = '';
    this.coachStartText = '';
    this.coachFeedback = null;
    this.coachGrammarResult = null;
    this.coachHeardWhenAnalyzed = '';
    this.micActive = true;
    try {
      this.recognition.start();
    } catch {
      this.micActive = false;
      this.snack.open('Could not start coach microphone', 'Close', { duration: 3000 });
    }
  }

  resolveCoachTarget(host: HTMLTextAreaElement): string {
    const goal = (this.coachGoalText ?? '').trim();
    if (goal) {
      return goal;
    }
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
    if (this.speaking) {
      this.stopSpeaking();
    }
    this.speakText(t);
  }

  /** Show grammar panel when API reported fixes or corrected text differs from what you said. */
  coachGrammarShowOnScreen(gr: GrammarResult | null | undefined): boolean {
    if (!gr?.correctedText?.trim()) {
      return false;
    }
    if ((gr.issuesFixed ?? 0) > 0) {
      return true;
    }
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

  /**
   * Grammar (heard text) + pronunciation coach (target vs heard) in parallel.
   * Optionally triggered when coach mic stops — then voice is short (intro + essentials only).
   */
  private runCoachPipeline(host: HTMLTextAreaElement, opts: { autoFromMicStop: boolean }): void {
    const heard = (this.coachHeard ?? '').trim();
    if (!heard) {
      if (!opts.autoFromMicStop) {
        this.snack.open('Use Coach mic first to capture what you said.', 'Close', { duration: 3500 });
      }
      return;
    }
    if (opts.autoFromMicStop && heard.length < this.coachMinHeard) {
      return;
    }
    const target = this.resolveCoachTarget(host);
    this.coachLoading = true;
    this.coachFeedback = null;
    this.coachGrammarResult = null;

    const grammar$ = this.api.grammar(heard).pipe(catchError(() => of(null)));
    const coach$ = this.api.pronunciationCoach(target, heard).pipe(catchError(() => of(null)));

    forkJoin({ grammar: grammar$, coach: coach$ }).subscribe({
      next: ({ grammar, coach }) => {
        this.coachGrammarResult = grammar;
        this.coachFeedback = coach;
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
        this.snack.open('Coach request failed. Is Ollama + gateway + notebook running?', 'Close', { duration: 4500 });
      }
    });
  }

  speakIdealSentence(): void {
    const s = (this.coachFeedback?.idealSentence ?? '').trim();
    if (!s) {
      this.snack.open('No ideal sentence from coach yet.', 'Close', { duration: 2500 });
      return;
    }
    if (this.speaking) {
      this.stopSpeaking();
    }
    this.speakText(s);
  }

  /** Replay the same short voice clip (twice + spelling per fix). */
  replayCoachVoice(): void {
    if (!this.coachFeedback && !this.coachGrammarResult) {
      this.snack.open('Stop the coach mic or tap Analyze first.', 'Close', { duration: 3000 });
      return;
    }
    const heard = (this.coachHeardWhenAnalyzed || this.coachHeard || '').trim();
    this.speakCoachScriptShort(this.coachFeedback, this.coachGrammarResult, heard);
  }

  /**
   * Short voice only: intro, then each key phrase is said twice and followed by a spelling guide.
   * Does not read the full summary or all tips (those stay on screen).
   */
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

  private buildCoachVoicePhrasesShort(
    cf: PronunciationCoachResult | null,
    grammar: GrammarResult | null,
    heard: string
  ): string[] {
    const parts: string[] = ["Hey, I'm your speaking coach."];

    this.appendGrammarVoiceParts(parts, grammar, heard);

    if (cf?.rawCoachText && !cf.overallSummary && !cf.items?.length) {
      const raw = cf.rawCoachText.trim();
      if (raw.length > 0 && raw.length <= 120) {
        parts.push('Coach note — listen carefully.');
        this.pushSayTwiceThenSpell(parts, raw);
      } else {
        const clip = raw.slice(0, 280);
        parts.push(clip.length < raw.length ? clip + ' Details are on screen.' : clip);
      }
      return parts.map((p) => p.trim()).filter((p) => p.length > 0);
    }

    if (cf?.items?.length) {
      const it0 = cf.items[0];
      const c0 = it0.correction?.trim();
      if (c0) {
        parts.push('Pronunciation — listen to the fix.');
        this.pushSayTwiceThenSpell(parts, c0);
      }
      if (cf.items.length > 1 && cf.items[1].correction?.trim()) {
        parts.push('One more fix.');
        this.pushSayTwiceThenSpell(parts, cf.items[1].correction!.trim());
      }
    } else if (cf?.idealSentence?.trim()) {
      parts.push('Line to practice.');
      this.pushSayTwiceThenSpell(parts, cf.idealSentence.trim());
    }

    const hadGrammar = this.grammarPhraseForVoice(grammar, heard) !== null;
    if (parts.length === 1 && !cf?.items?.length && !cf?.idealSentence?.trim() && !hadGrammar) {
      parts.push('Looks good — keep practicing. More detail is on your screen if you need it.');
    }

    return parts.map((p) => p.trim()).filter((p) => p.length > 0);
  }

  /** Corrected grammar phrase to voice, or null if unchanged / missing. */
  private grammarPhraseForVoice(grammar: GrammarResult | null, heard: string): string | null {
    if (!grammar?.correctedText?.trim()) {
      return null;
    }
    const a = heard.trim().replace(/\s+/g, ' ');
    const b = grammar.correctedText.trim().replace(/\s+/g, ' ');
    if (a === b) {
      return null;
    }
    let fix = grammar.correctedText.trim();
    if (fix.length > 140) {
      fix = fix.slice(0, 137).replace(/\s+\S*$/, '') + '…';
    }
    return fix;
  }

  private appendGrammarVoiceParts(parts: string[], grammar: GrammarResult | null, heard: string): void {
    const fix = this.grammarPhraseForVoice(grammar, heard);
    if (!fix) {
      return;
    }
    parts.push('Grammar — here is the correct phrase.');
    this.pushSayTwiceThenSpell(parts, fix);
  }

  /** Say `phrase` twice (separate clips), then a spelling walkthrough when possible. */
  private pushSayTwiceThenSpell(parts: string[], phrase: string): void {
    const p = phrase.trim();
    if (!p) {
      return;
    }
    parts.push(`Listen: ${p}.`);
    parts.push(`Again: ${p}.`);
    const spell = this.spellGuideFromPhrase(p);
    if (spell) {
      parts.push(spell);
    }
  }

  /**
   * Single word: letter-by-letter. Short phrase: up to five content words spelled out.
   */
  private spellGuideFromPhrase(phrase: string): string | null {
    const normalized = phrase.trim().replace(/\s+/g, ' ');
    if (!normalized) {
      return null;
    }

    const tokens = normalized
      .split(/\s/)
      .map((tok) => tok.replace(/^[^a-zA-Z]+|[^a-zA-Z]+$/g, ''))
      .filter((w) => w.length > 0);

    if (!tokens.length) {
      return null;
    }

    if (tokens.length === 1) {
      const hint = this.spellAloudHint(tokens[0]);
      return hint || null;
    }

    const skip = new Set([
      'a',
      'an',
      'the',
      'is',
      'are',
      'was',
      'were',
      'to',
      'of',
      'and',
      'or',
      'it',
      'in',
      'on',
      'at',
      'as',
      'be',
      'by',
      'so',
      'we',
      'he',
      'she',
      'they',
      'you',
      'i',
      'my',
      'me',
      'if',
      'but'
    ]);
    let toSpell = tokens.filter((w) => w.length >= 3 && !skip.has(w.toLowerCase())).slice(0, 5);
    if (!toSpell.length) {
      toSpell = tokens.slice(0, 3);
    }

    const spellChunks = toSpell.map((w) => this.wordSpellChunk(w)).filter((s): s is string => !!s);
    if (!spellChunks.length) {
      return null;
    }
    return `Spell with me: ${spellChunks.join(' Next: ')}`;
  }

  private wordSpellChunk(w: string): string {
    const core = w.replace(/-/g, '');
    if (core.length < 2 || core.length > 16 || !/^[a-zA-Z]+$/.test(core)) {
      return '';
    }
    const letters = core.toUpperCase().split('').join(', ');
    return `${w}: ${letters}`;
  }

  /** Letter-by-letter for a single-word correction so the learner can spell it. */
  private spellAloudHint(correction: string | null | undefined): string {
    if (!correction) {
      return '';
    }
    const t = correction.trim().replace(/['']/g, '');
    if (!t || /\s/.test(t)) {
      return '';
    }
    if (t.length < 3 || t.length > 16) {
      return '';
    }
    const core = t.replace(/-/g, '');
    if (!/^[a-zA-Z]+$/.test(core)) {
      return '';
    }
    const letters = core
      .toUpperCase()
      .split('')
      .join(', ');
    return `Spell it with me: ${letters}.`;
  }

  /** Speak several phrases one after another (one "conversation" with the coach). */
  private speakUtteranceQueue(phrases: string[], index = 0): void {
    if (!this.ttsAvailable()) {
      return;
    }
    if (index === 0) {
      window.speechSynthesis.cancel();
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
    u.rate = 0.9;
    u.pitch = 1;
    u.onend = () => {
      this.zone.run(() => this.speakUtteranceQueue(phrases, next + 1));
    };
    u.onerror = () => {
      this.zone.run(() => {
        this.speaking = false;
      });
    };
    if (next === 0) {
      this.speaking = true;
    }
    window.speechSynthesis.speak(u);
  }

  /** Split AI summary into readable paragraphs for the template. */
  summaryParagraphs(text: string | null | undefined): string[] {
    if (!text?.trim()) {
      return [];
    }
    const t = text.trim();
    if (/\n/.test(t)) {
      return t.split(/\n+/).map((s) => s.trim()).filter((s) => s.length > 0);
    }
    if (t.length > 220) {
      return t.split(/(?<=[.!?])\s+/).map((s) => s.trim()).filter((s) => s.length > 0);
    }
    return [t];
  }

  private ttsAvailable(): boolean {
    return isPlatformBrowser(this.platformId) && typeof window !== 'undefined' && !!window.speechSynthesis;
  }

  /** Read title + full note (browser TTS, English). */
  speakPage(): void {
    if (!this.ttsAvailable()) {
      this.snack.open('Read-aloud needs a browser with speech synthesis (e.g. Chrome).', 'Close', { duration: 4000 });
      return;
    }
    const title = (this.titleDraft ?? '').trim();
    const body = (this.contentDraft ?? '').trim();
    const parts: string[] = [];
    if (title) {
      parts.push(title);
    }
    if (body) {
      parts.push(body);
    }
    const text = parts.join('. ');
    if (!text) {
      this.snack.open('Write something to listen.', 'Close', { duration: 2500 });
      return;
    }
    if (this.speaking) {
      this.stopSpeaking();
    }
    this.speakText(text);
  }

  /** Read highlighted text in the note editor. */
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
    if (this.speaking) {
      this.stopSpeaking();
    }
    this.speakText(text);
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
    if (this.speaking) {
      this.stopSpeaking();
    } else {
      this.speakPage();
    }
  }

  private speakText(text: string): void {
    if (!this.ttsAvailable()) {
      return;
    }
    const max = 32000;
    const chunk = text.length > max ? text.slice(0, max) : text;
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(chunk);
    u.lang = 'en-US';
    u.rate = 1;
    u.pitch = 1;
    u.onend = () => {
      this.zone.run(() => {
        this.speaking = false;
      });
    };
    u.onerror = () => {
      this.zone.run(() => {
        this.speaking = false;
        this.snack.open('Read-aloud was interrupted.', 'Close', { duration: 2500 });
      });
    };
    this.speaking = true;
    window.speechSynthesis.speak(u);
  }

  insertSummaryIntoNote(): void {
    if (!this.summaryResult?.summary) return;
    this.contentDraft = (this.contentDraft + '\n\n--- Summary ---\n' + this.summaryResult.summary).trim();
  }

  // ----- Bubble editor helpers -----

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
    if (this.bubbleSelectedIndex == null) {
      this.snack.open('Select a bubble first.', 'Close', { duration: 2500 });
      return;
    }
    const src = (this.bubbles[this.bubbleSelectedIndex] ?? '').trim();
    if (!src) return;
    this.api.grammar(src).subscribe({
      next: (r: GrammarResult) => {
        const next = [...this.bubbles];
        next[this.bubbleSelectedIndex!] = r.correctedText?.trim() || src;
        this.bubbles = next;
        const via = r.source === 'ollama' ? 'Ollama (local)' : r.source === 'languagetool' ? 'LanguageTool' : 'Grammar';
        this.snack.open(`${via}: bubble corrected`, 'OK', { duration: 2500 });
      },
      error: () => this.snack.open('Grammar failed (try Ollama running + model pulled)', 'Close', { duration: 4000 })
    });
  }

  private readSelectionOrParagraph(host: HTMLTextAreaElement): string {
    const lo = Math.min(host.selectionStart, host.selectionEnd);
    const hi = Math.max(host.selectionStart, host.selectionEnd);
    const raw = host.value ?? '';
    const selection = raw.slice(lo, hi).trim();
    if (selection) return selection;
    // If no selection, bubble the current paragraph.
    const p = this.currentParagraphBounds(raw, lo);
    return raw.slice(p.start, p.end).trim();
  }

  private removeSelectionOrParagraphFromEditor(host: HTMLTextAreaElement): void {
    const raw = host.value ?? '';
    const lo = Math.min(host.selectionStart, host.selectionEnd);
    const hi = Math.max(host.selectionStart, host.selectionEnd);
    let start = lo;
    let end = hi;
    if (start === end) {
      const p = this.currentParagraphBounds(raw, lo);
      start = p.start;
      end = p.end;
    }
    const next = (raw.slice(0, start) + raw.slice(end)).replace(/\n{3,}/g, '\n\n').trim();
    this.contentDraft = next;
    // update textarea immediately so selection/cursor behaves
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
}
