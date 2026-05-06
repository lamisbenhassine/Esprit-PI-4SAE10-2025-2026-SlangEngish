import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API = '/api/forum/tutor-assist';

export interface TutorAssistSlot {
  start: string;
  end: string;
  tutorId?: number | null;
  tutorName?: string | null;
}

export interface TutorAssistSlotsRequest {
  topicId: number;
  studentId: number;
  studentName?: string;
  level?: string;
  problemType?: string;
  timezone?: string;
  durationMin?: number;
  windowStart?: string;
  windowEnd?: string;
}

export interface TutorAssistSlotsResponse {
  slots: TutorAssistSlot[];
}

export interface TutorAssistBookRequest {
  topicId: number;
  studentId: number;
  studentName?: string;
  tutorId?: number | null;
  tutorName?: string | null;
  start: string;
  end: string;
  timezone?: string;
  problemType?: string;
  meetingMode?: string;
}

export interface TutorAssistBookResponse {
  success: boolean;
  appointmentId?: string | null;
  meetLink?: string | null;
}

export interface TutorAssistVapiConfig {
  publicKey: string;
  assistantId: string;
}

@Injectable({ providedIn: 'root' })
export class TutorAssistService {
  constructor(private http: HttpClient) {}

  getSlots(req: TutorAssistSlotsRequest): Observable<TutorAssistSlotsResponse> {
    return this.http.post<TutorAssistSlotsResponse>(`${API}/slots`, req);
  }

  book(req: TutorAssistBookRequest): Observable<TutorAssistBookResponse> {
    return this.http.post<TutorAssistBookResponse>(`${API}/book`, req);
  }

  getVapiConfig(): Observable<TutorAssistVapiConfig> {
    return this.http.get<TutorAssistVapiConfig>(`${API}/vapi-config`);
  }
}
