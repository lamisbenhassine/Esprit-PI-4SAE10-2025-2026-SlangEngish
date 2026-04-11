import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Chemins sous {@code /api/forum/messages/...} (alignés sur ForumMessageController). */
const API = '/api/forum/messages';

export interface ModerationMessageRow {
  id: number;
  topicId: number;
  topicTitle: string;
  authorId: number;
  content: string;
  attachments?: string | null;
  parentMessageId?: number | null;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class ForumModerationService {
  constructor(private http: HttpClient) {}

  listRecent(moderatorUserId: number, limit = 100): Observable<ModerationMessageRow[]> {
    const params = new HttpParams()
      .set('moderatorUserId', String(moderatorUserId))
      .set('limit', String(limit));
    return this.http.get<ModerationMessageRow[]>(`${API}/moderation/list`, { params });
  }

  deleteMessage(messageId: number, moderatorUserId: number): Observable<void> {
    const params = new HttpParams().set('moderatorUserId', String(moderatorUserId));
    return this.http.delete<void>(`${API}/moderation/${messageId}`, { params });
  }

  blockUser(moderatorUserId: number, blockedUserId: number): Observable<void> {
    return this.http.post<void>(`${API}/moderation/block`, { moderatorUserId, blockedUserId });
  }
}
