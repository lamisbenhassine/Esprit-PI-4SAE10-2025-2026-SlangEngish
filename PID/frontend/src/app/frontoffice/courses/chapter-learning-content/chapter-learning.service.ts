import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of, throwError } from 'rxjs';
import { API_CHAPTERS_BASE } from '../../../api-config';

export interface FlashcardItem {
  front: string;
  back: string;
}

export interface ChapterLearningResponse {
  chapterId: number;
  summaryEnglish: string;
  flashcards: FlashcardItem[];
  updatedAt: string;
  fromCache: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ChapterLearningService {
  private readonly base = API_CHAPTERS_BASE;

  constructor(private readonly http: HttpClient) {}

  getCached(chapterId: number): Observable<ChapterLearningResponse | null> {
    return this.http.get<ChapterLearningResponse>(`${this.base}/${chapterId}/learning-content`).pipe(
      catchError((err) => {
        if (err.status === 404) {
          return of(null);
        }
        return throwError(() => err);
      })
    );
  }

  generateContent(chapterId: number, regenerate: boolean): Observable<ChapterLearningResponse> {
    const params = new HttpParams().set('regenerate', String(regenerate));
    return this.http.post<ChapterLearningResponse>(
      `${this.base}/${chapterId}/generate-content`,
      {},
      { params }
    );
  }
}

