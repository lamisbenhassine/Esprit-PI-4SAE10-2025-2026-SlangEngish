import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GESTION_COURS_API_BASE } from '../../api-config';

export interface Progression {
  id: number;
  userId: number;
  lastPage: number;
  progressPercentage: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProgressionService {
  private readonly apiUrl = `${GESTION_COURS_API_BASE}/progression`;

  constructor(private http: HttpClient) {}

  updateChapterProgress(payload: {
    userId: number;
    chapterId: number;
    lastPage: number;
    progressPercentage: number;
  }): Observable<Progression> {
    const params = new HttpParams()
      .set('userId', payload.userId)
      .set('chapterId', payload.chapterId)
      .set('lastPage', payload.lastPage)
      .set('progressPercentage', payload.progressPercentage);

    return this.http.post<Progression>(`${this.apiUrl}/chapter`, null, { params });
  }

  getChapterProgress(userId: number, chapterId: number): Observable<Progression | null> {
    const params = new HttpParams()
      .set('userId', String(userId))
      .set('chapterId', String(chapterId));

    return this.http.get<Progression | null>(`${this.apiUrl}/chapter`, { params });
  }

  getCourseProgress(userId: number, courseId: number): Observable<number> {
    const params = new HttpParams()
      .set('userId', String(userId))
      .set('courseId', String(courseId));

    return this.http.get<number>(`${this.apiUrl}/course`, { params });
  }
}
