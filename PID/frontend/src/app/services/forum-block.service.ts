import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_BASE = '/api/forum/blocks';

@Injectable({ providedIn: 'root' })
export class ForumBlockService {
  constructor(private http: HttpClient) {}

  block(blockerUserId: number, blockedUserId: number): Observable<unknown> {
    return this.http.post(API_BASE, { blockerUserId, blockedUserId });
  }

  unblock(blockerUserId: number, blockedUserId: number): Observable<void> {
    const params = new HttpParams()
      .set('blockerUserId', String(blockerUserId))
      .set('blockedUserId', String(blockedUserId));
    return this.http.delete<void>(API_BASE, { params });
  }

  getBlockedIds(blockerUserId: number): Observable<number[]> {
    return this.http.get<number[]>(`${API_BASE}/${blockerUserId}/ids`);
  }
}
