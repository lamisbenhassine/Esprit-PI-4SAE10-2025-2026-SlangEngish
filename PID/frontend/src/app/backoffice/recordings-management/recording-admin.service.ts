import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Recording, RecordingStatus } from '../../models/recording.model';
import { GESTION_COURS_API_BASE } from '../../api-config';

export interface RecordingUpdatePayload {
  title: string;
  streamLink: string | null;
  recordingLink: string | null;
  recordedAt: string;
  status: RecordingStatus;
}

export interface RecordingCreatePayload {
  title: string;
  recordingLink: string | null;
  status?: RecordingStatus | null;
}

@Injectable({
  providedIn: 'root'
})
export class RecordingAdminService {

  private readonly apiUrl = `${GESTION_COURS_API_BASE}/recording`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Recording[]> {
    return this.http.get<Recording[]>(`${this.apiUrl}/all`);
  }

  create(payload: RecordingCreatePayload): Observable<Recording> {
    const body = {
      title: payload.title,
      streamLink: null,
      recordingLink: payload.recordingLink,
      recordedAt: null,
      status: payload.status ?? null
    };
    return this.http.post<Recording>(`${this.apiUrl}/create`, body);
  }

  update(id: number, payload: RecordingUpdatePayload): Observable<Recording> {
    return this.http.put<Recording>(`${this.apiUrl}/update/${id}`, payload);
  }

  uploadFile(id: number, file: File): Observable<Recording> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Recording>(`${this.apiUrl}/upload/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/delete/${id}`);
  }
}

