import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../services/auth.service';
import {
  CreateNotebookGameEntryRequest,
  CreateNotebookGameRequest,
  NotebookApiService,
  NotebookGameDetail,
  NotebookGameSummary
} from '../../core/services/notebook-api.service';

@Component({
  selector: 'app-notebook-games',
  templateUrl: './notebook-games.component.html',
  styleUrls: ['./notebook-games.component.css']
})
export class NotebookGamesComponent implements OnInit {
  teacherId: number | null = null;

  loading = false;
  games: NotebookGameSummary[] = [];

  // Create form
  title = '';
  description = '';
  published = true;
  entries: Array<{ clue: string; answer: string; teacherHint: string }> = [
    { clue: 'A very informal word for friend', answer: 'buddy', teacherHint: 'It starts with B.' }
  ];

  creating = false;
  createdPreview: NotebookGameDetail | null = null;

  constructor(private api: NotebookApiService, private auth: AuthService, private snack: MatSnackBar) {}

  ngOnInit(): void {
    const u = this.auth.getCurrentUser();
    this.teacherId = u?.id ?? null;
    if (!this.teacherId) {
      this.snack.open('Sign in as a teacher to manage notebook games.', 'Close', { duration: 4000 });
      return;
    }
    this.refresh();
  }

  refresh(): void {
    if (!this.teacherId) return;
    this.loading = true;
    this.api.listTeacherGames(this.teacherId).subscribe({
      next: (list) => {
        this.games = list ?? [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.games = [];
        this.snack.open('Could not load games. Is gateway + notebook running?', 'Close', { duration: 3500 });
      }
    });
  }

  addEntry(): void {
    this.entries = [...this.entries, { clue: '', answer: '', teacherHint: '' }];
  }

  removeEntry(i: number): void {
    this.entries = this.entries.filter((_, idx) => idx !== i);
  }

  entryOrder(i: number): number {
    return i + 1;
  }

  private buildPayload(): CreateNotebookGameRequest {
    if (!this.teacherId) throw new Error('Missing teacher id');
    const title = (this.title ?? '').trim();
    const desc = (this.description ?? '').trim();

    const entries: CreateNotebookGameEntryRequest[] = (this.entries ?? [])
      .map((e, idx) => ({
        order: idx + 1,
        clue: (e.clue ?? '').trim(),
        answer: (e.answer ?? '').trim(),
        teacherHint: (e.teacherHint ?? '').trim() || null
      }))
      .filter((e) => e.clue.length > 0 || e.answer.length > 0);

    return {
      teacherId: this.teacherId,
      title,
      description: desc || null,
      type: 'CROSSWORD',
      published: this.published,
      entries
    };
  }

  create(): void {
    if (!this.teacherId) return;
    const payload = this.buildPayload();
    if (!payload.title.trim()) {
      this.snack.open('Title is required.', 'Close', { duration: 2500 });
      return;
    }
    if (!payload.entries.length) {
      this.snack.open('Add at least 1 clue + answer.', 'Close', { duration: 2500 });
      return;
    }
    if (payload.entries.some((e) => !e.clue.trim() || !e.answer.trim())) {
      this.snack.open('Each entry must have a clue and an answer.', 'Close', { duration: 3000 });
      return;
    }

    this.creating = true;
    this.createdPreview = null;
    this.api.createGame(payload).subscribe({
      next: (created) => {
        this.creating = false;
        this.createdPreview = created;
        this.snack.open('Game created!', 'OK', { duration: 2500 });
        this.refresh();
        this.title = '';
        this.description = '';
        this.published = true;
        this.entries = [{ clue: '', answer: '', teacherHint: '' }];
      },
      error: (err) => {
        this.creating = false;
        const msg = err?.error?.message || 'Create failed.';
        this.snack.open(msg, 'Close', { duration: 4000 });
      }
    });
  }
}

