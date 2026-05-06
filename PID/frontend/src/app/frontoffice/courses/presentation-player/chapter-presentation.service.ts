import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of, throwError } from 'rxjs';
import { API_CHAPTERS_BASE } from '../../../api-config';

export interface PresentationSlide {
  title: string;
  bullets: string[];
}

export interface ChapterPresentationDto {
  chapterId: number;
  slides: PresentationSlide[];
  updatedAt: string;
  fromCache: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ChapterPresentationService {
  private readonly base = API_CHAPTERS_BASE;

  constructor(private readonly http: HttpClient) {}

  getPresentation(chapterId: number): Observable<ChapterPresentationDto | null> {
    return this.http.get<ChapterPresentationDto>(`${this.base}/${chapterId}/presentation`).pipe(
      catchError((err) => {
        if (err.status === 404) {
          return of(null);
        }
        return throwError(() => err);
      })
    );
  }

  generatePresentation(
    chapterId: number,
    file: File,
    regenerate: boolean
  ): Observable<ChapterPresentationDto> {
    const fd = new FormData();
    fd.append('file', file, file.name);
    const params = new HttpParams().set('regenerate', String(regenerate));
    return this.http.post<ChapterPresentationDto>(`${this.base}/${chapterId}/generate-presentation`, fd, {
      params
    });
  }
}

