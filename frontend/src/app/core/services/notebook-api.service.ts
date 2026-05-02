import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NOTEBOOK_API_URL } from '../../api.config';

export interface NotebookNote {
  id?: number;
  userId: number;
  title: string;
  content: string;
  shareScore?: number;
  shared?: boolean;
  shareId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface GrammarResult {
  correctedText: string;
  issuesFixed: number;
  /** 'ollama' | 'languagetool' */
  source?: string | null;
}

export interface DictionaryResult {
  word: string;
  meanings: string[];
  phonetic?: string | null;
  aiExplanation?: string | null;
}

export interface SummaryResult {
  summary: string;
  originalWordCount: number;
  summaryWordCount: number;
  /** 'ollama' | 'extractive' */
  source?: string | null;
}

export interface WordCountRow {
  word: string;
  count: number;
}

export interface NotebookDashboard {
  bestNoteToShare: NotebookNote | null;
  mostUsedWords: WordCountRow[];
}

export interface PronunciationCoachItem {
  issue: string;
  correction: string;
  tip: string;
}

export interface PronunciationCoachResult {
  overallSummary: string;
  idealSentence?: string | null;
  items: PronunciationCoachItem[];
  overallTips: string[];
  rawCoachText?: string | null;
}

export interface NotebookGameSummary {
  id: number;
  title: string;
  description?: string | null;
  type: string;
  published: boolean;
  entryCount: number;
  createdAt?: string;
  updatedAt?: string;
  teacherId: number;
}

export interface NotebookGameEntryView {
  id: number;
  order: number;
  clue: string;
  answerLength: number;
  row?: number | null;
  col?: number | null;
  dir?: 'ACROSS' | 'DOWN' | string | null;
  number?: number | null;
}

export interface NotebookGameDetail {
  id: number;
  title: string;
  description?: string | null;
  type: string;
  createdAt?: string;
  teacherId: number;
  gridRows?: number | null;
  gridCols?: number | null;
  /** Each string is length gridCols: '1' = playable, '0' = black. */
  cellMaskRows?: string[] | null;
  entries: NotebookGameEntryView[];
}

export interface NotebookGameProgressRow {
  entryId: number;
  solved: boolean;
  attempts: number;
  hintLevel: number;
  lastAnswer?: string | null;
}

export interface NotebookGameHintResponse {
  level: number;
  hint: string;
}

export interface NotebookGameSubmitAnswerResponse {
  correct: boolean;
  solved: boolean;
  attempts: number;
  hintLevel: number;
}

export interface CreateNotebookGameEntryRequest {
  order: number;
  clue: string;
  answer: string;
  teacherHint?: string | null;
}

export interface CreateNotebookGameRequest {
  teacherId: number;
  title: string;
  description?: string | null;
  type?: string | null; // "CROSSWORD"
  published?: boolean | null;
  entries: CreateNotebookGameEntryRequest[];
}

@Injectable({ providedIn: 'root' })
export class NotebookApiService {
  private readonly base = NOTEBOOK_API_URL;

  constructor(private http: HttpClient) {}

  listNotes(userId: number): Observable<NotebookNote[]> {
    return this.http.get<NotebookNote[]>(`${this.base}/notes`, { params: { userId: String(userId) } });
  }

  createNote(userId: number, title: string, content: string): Observable<NotebookNote> {
    return this.http.post<NotebookNote>(`${this.base}/notes`, { userId, title, content });
  }

  updateNote(userId: number, id: number, title: string, content: string): Observable<NotebookNote> {
    return this.http.put<NotebookNote>(`${this.base}/notes/${id}`, { userId, title, content });
  }

  deleteNote(userId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/notes/${id}`, { params: { userId: String(userId) } });
  }

  shareNote(userId: number, id: number): Observable<NotebookNote> {
    return this.http.post<NotebookNote>(`${this.base}/notes/${id}/share`, null, { params: { userId: String(userId) } });
  }

  unshareNote(userId: number, id: number): Observable<NotebookNote> {
    return this.http.delete<NotebookNote>(`${this.base}/notes/${id}/share`, { params: { userId: String(userId) } });
  }

  listSharedNotes(userId: number): Observable<NotebookNote[]> {
    return this.http.get<NotebookNote[]>(`${this.base}/shared-notes`, { params: { userId: String(userId) } });
  }

  importSharedNote(userId: number, shareId: string): Observable<NotebookNote> {
    return this.http.post<NotebookNote>(`${this.base}/shared-notes/${encodeURIComponent(shareId)}/import`, null, {
      params: { userId: String(userId) }
    });
  }

  dashboard(userId: number): Observable<NotebookDashboard> {
    return this.http.get<NotebookDashboard>(`${this.base}/dashboard`, { params: { userId: String(userId) } });
  }

  grammar(text: string): Observable<GrammarResult> {
    return this.http.post<GrammarResult>(`${this.base}/ai/grammar`, { text });
  }

  dictionary(word: string, context?: string): Observable<DictionaryResult> {
    return this.http.post<DictionaryResult>(`${this.base}/ai/dictionary`, { word, context });
  }

  summarize(text: string): Observable<SummaryResult> {
    return this.http.post<SummaryResult>(`${this.base}/ai/summarize`, { text });
  }

  pronunciationCoach(targetText: string, heardText: string): Observable<PronunciationCoachResult> {
    return this.http.post<PronunciationCoachResult>(`${this.base}/ai/pronunciation-coach`, {
      targetText,
      heardText
    });
  }

  // ----- Games -----

  listPublishedGames(createdAfterEpochMs?: number): Observable<NotebookGameSummary[]> {
    const params: Record<string, string> = {};
    if (createdAfterEpochMs != null) {
      params['createdAfter'] = String(createdAfterEpochMs);
    }
    return this.http.get<NotebookGameSummary[]>(`${this.base}/games`, { params });
  }

  gameDetail(gameId: number): Observable<NotebookGameDetail> {
    return this.http.get<NotebookGameDetail>(`${this.base}/games/${gameId}`);
  }

  gameProgress(gameId: number, userId: number): Observable<NotebookGameProgressRow[]> {
    return this.http.get<NotebookGameProgressRow[]>(`${this.base}/games/${gameId}/progress`, {
      params: { userId: String(userId) }
    });
  }

  gameHint(gameId: number, entryId: number, userId: number): Observable<NotebookGameHintResponse> {
    return this.http.post<NotebookGameHintResponse>(`${this.base}/games/${gameId}/entries/${entryId}/hint`, null, {
      params: { userId: String(userId) }
    });
  }

  gameSubmitAnswer(
    gameId: number,
    entryId: number,
    userId: number,
    answer: string
  ): Observable<NotebookGameSubmitAnswerResponse> {
    return this.http.post<NotebookGameSubmitAnswerResponse>(`${this.base}/games/${gameId}/entries/${entryId}/answer`, {
      userId,
      answer
    });
  }

  listTeacherGames(teacherId: number): Observable<NotebookGameSummary[]> {
    return this.http.get<NotebookGameSummary[]>(`${this.base}/games/teacher`, {
      params: { teacherId: String(teacherId) }
    });
  }

  createGame(req: CreateNotebookGameRequest): Observable<NotebookGameDetail> {
    return this.http.post<NotebookGameDetail>(`${this.base}/games`, req);
  }
}
