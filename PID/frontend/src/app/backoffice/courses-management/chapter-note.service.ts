import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GESTION_COURS_API_BASE } from '../../api-config';
import { ChapterNote, NoteTag } from '../../models/chapter-note.model';

export interface ChapterNotePayload {
  title: string;
  content: string;
  tag: NoteTag;
}

@Injectable({
  providedIn: 'root'
})
export class ChapterNoteService {
  private readonly apiUrl = `${GESTION_COURS_API_BASE}/chapter-note`;

  constructor(private http: HttpClient) {}

  listByChapter(userId: number, chapterId: number, search?: string): Observable<ChapterNote[]> {
    let params = new HttpParams().set('userId', String(userId));
    if (search != null && search.trim() !== '') {
      params = params.set('search', search.trim());
    }
    return this.http.get<ChapterNote[]>(`${this.apiUrl}/by-chapter/${chapterId}`, { params });
  }

  create(userId: number, chapterId: number, body: ChapterNotePayload): Observable<ChapterNote> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.post<ChapterNote>(`${this.apiUrl}/add/${chapterId}`, body, { params });
  }

  update(userId: number, noteId: number, body: ChapterNotePayload): Observable<ChapterNote> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.put<ChapterNote>(`${this.apiUrl}/update/${noteId}`, body, { params });
  }

  delete(userId: number, noteId: number): Observable<void> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.delete<void>(`${this.apiUrl}/delete/${noteId}`, { params });
  }
}
