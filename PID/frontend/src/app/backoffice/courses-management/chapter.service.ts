import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Chapter } from '../../models/chapter.model';
import { GESTION_COURS_API_BASE } from '../../api-config';

@Injectable({
  providedIn: 'root'
})
export class ChapterService {
  private readonly apiUrl = `${GESTION_COURS_API_BASE}/chapter`;

  constructor(private http: HttpClient) {}

  getByCourse(courseId: number): Observable<Chapter[]> {
    return this.http.get<Chapter[]>(`${this.apiUrl}/by-course/${courseId}`);
  }

  getById(id: number): Observable<Chapter> {
    return this.http.get<Chapter>(`${this.apiUrl}/get/${id}`);
  }

  addChapter(courseId: number, chapter: Chapter): Observable<Chapter> {
    return this.http.post<Chapter>(`${this.apiUrl}/add/${courseId}`, chapter);
  }

  updateChapter(id: number, chapter: Chapter): Observable<Chapter> {
    return this.http.put<Chapter>(`${this.apiUrl}/update/${id}`, chapter);
  }

  deleteChapter(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/delete/${id}`);
  }
}

