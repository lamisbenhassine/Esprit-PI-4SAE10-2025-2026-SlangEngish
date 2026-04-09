import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

const API = '/api/forum/direct';

export type DirectKind = 'PEER' | 'WITH_TUTOR';

export interface DirectConversation {
  id: number;
  participantLowId: number;
  participantHighId: number;
  kind: DirectKind;
  createdAt?: string;
  updatedAt?: string;
}

export interface DirectConversationSummary {
  id: number;
  otherUserId: number;
  kind: DirectKind;
  lastMessagePreview?: string;
  updatedAt?: string;
}

export interface DirectMessage {
  id: number;
  conversationId: number;
  senderId: number;
  content: string;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class DirectMessageService {
  constructor(private http: HttpClient) {}

  open(userId: number, withUserId: number, kind: DirectKind): Observable<DirectConversation> {
    return this.http.post<DirectConversation>(`${API}/open`, { userId, withUserId, kind });
  }

  inbox(userId: number): Observable<DirectConversationSummary[]> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.get<DirectConversationSummary[]>(`${API}/inbox`, { params });
  }

  getMessages(conversationId: number, userId: number): Observable<DirectMessage[]> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.get<DirectMessage[]>(`${API}/${conversationId}/messages`, { params });
  }

  send(conversationId: number, senderId: number, content: string): Observable<DirectMessage> {
    return this.http.post<DirectMessage>(`${API}/${conversationId}/messages`, { senderId, content });
  }
}
