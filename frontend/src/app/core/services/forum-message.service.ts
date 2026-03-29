import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Use relative base URL so Angular proxy can route to backend (dev) or gateway (prod).
const API_BASE_URL = '/api/forum';
const API_URL = `${API_BASE_URL}/messages`;
const TOPICS_URL = `${API_BASE_URL}/topics`;

export interface ForumMessage {
    id?: number;
    authorId: number;
    content: string;
    parentMessageId?: number | null;
    createdAt?: string;
    updatedAt?: string;
}

export interface CreateMessageRequest {
    topicId: number;
    authorId: number;
    content: string;
    parentMessageId?: number | null;
}

@Injectable({
    providedIn: 'root'
})
export class ForumMessageService {
    constructor(private http: HttpClient) { }

    getMessagesByTopic(topicId: number): Observable<ForumMessage[]> {
        return this.http.get<ForumMessage[]>(`${TOPICS_URL}/${topicId}/messages`);
    }

    getReplies(parentMessageId: number): Observable<ForumMessage[]> {
        return this.http.get<ForumMessage[]>(`${API_URL}/replies/${parentMessageId}`);
    }

    createMessage(request: CreateMessageRequest): Observable<ForumMessage> {
        return this.http.post<ForumMessage>(`${TOPICS_URL}/${request.topicId}/messages`, {
            authorId: request.authorId,
            content: request.content,
            parentMessageId: request.parentMessageId ?? null
        });
    }

    updateMessage(id: number, content: string): Observable<ForumMessage> {
        return this.http.put<ForumMessage>(`${API_URL}/${id}`, { content });
    }

    deleteMessage(id: number): Observable<void> {
        return this.http.delete<void>(`${API_URL}/${id}`);
    }
}
