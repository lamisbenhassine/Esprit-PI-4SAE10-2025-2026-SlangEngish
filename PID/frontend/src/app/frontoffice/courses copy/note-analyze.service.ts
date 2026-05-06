import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_NOTES_BASE } from '../../api-config';

export interface NoteAnalysisResponse {
  feedbackSummary: string;
  suggestedTitle: string | null;
  grammarCorrectedHtml: string;
  restructuredHtml: string;
  originalTitle: string;
  originalHtml: string;
}

@Injectable({
  providedIn: 'root'
})
export class NoteAnalyzeService {
  constructor(private http: HttpClient) {}

  analyze(userId: number, noteId: number): Observable<NoteAnalysisResponse> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.post<NoteAnalysisResponse>(`${API_NOTES_BASE}/${noteId}/analyze`, {}, { params });
  }
}
