import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_BASE = '/api/forum/reports';

export type ForumReportTarget = 'TOPIC' | 'MESSAGE';
export type ForumReportStatus = 'OPEN' | 'REVIEWED' | 'DISMISSED';

export interface ForumReport {
  id?: number;
  reporterUserId: number;
  targetType: ForumReportTarget;
  targetId: number;
  reason: string;
  status: ForumReportStatus;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class ForumReportService {
  constructor(private http: HttpClient) {}

  submit(
    reporterUserId: number,
    targetType: ForumReportTarget,
    targetId: number,
    reason: string
  ): Observable<ForumReport> {
    return this.http.post<ForumReport>(API_BASE, {
      reporterUserId,
      targetType,
      targetId,
      reason
    });
  }

  listOpen(): Observable<ForumReport[]> {
    return this.http.get<ForumReport[]>(`${API_BASE}/open`);
  }

  listAll(): Observable<ForumReport[]> {
    return this.http.get<ForumReport[]>(API_BASE);
  }

  updateStatus(id: number, status: ForumReportStatus): Observable<ForumReport> {
    return this.http.patch<ForumReport>(`${API_BASE}/${id}/status`, { status });
  }
}
