import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Recording, RecordingHighlight, RecordingAnalysisStatus } from '../../models/recording.model';
import { GESTION_COURS_API_BASE } from '../../api-config';

interface AnalysisStatusResponse {
  status: RecordingAnalysisStatus;
}

@Injectable({
  providedIn: 'root'
})
export class RecordingService {

  private readonly apiUrl = `${GESTION_COURS_API_BASE}/recording`;
  // GESTION_COURS_API_BASE already ends with /api
  private readonly analysisApiUrl = `${GESTION_COURS_API_BASE}/recordings`;

  constructor(private http: HttpClient) {}

  getAvailable(): Observable<Recording[]> {
    return this.http.get<Recording[]>(`${this.apiUrl}/available`);
  }

  getHighlights(recordingId: number): Observable<RecordingHighlight[]> {
    return this.http.get<RecordingHighlight[]>(`${this.analysisApiUrl}/${recordingId}/highlights`);
  }

  getAnalysisStatus(recordingId: number): Observable<AnalysisStatusResponse> {
    return this.http.get<AnalysisStatusResponse>(`${this.analysisApiUrl}/${recordingId}/analysis-status`);
  }

  analyzeRecording(recordingId: number): Observable<{ status: string }> {
    return this.http.post<{ status: string }>(`${this.analysisApiUrl}/${recordingId}/analyze`, {});
  }
}

