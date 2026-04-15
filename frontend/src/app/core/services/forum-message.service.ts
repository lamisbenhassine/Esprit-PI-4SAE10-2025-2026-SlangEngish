import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { ComposeAssistService } from './compose-assist.service';

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
    /** JSON string: [{"type":"image"|"video","url":"..."}] */
    attachments?: string | null;
}

export interface CreateMessageRequest {
    topicId: number;
    authorId: number;
    content: string;
    parentMessageId?: number | null;
    attachments?: string | null;
}

@Injectable({
    providedIn: 'root'
})
export class ForumMessageService {
    constructor(
        private http: HttpClient,
        private composeAssist: ComposeAssistService
    ) {}

    getMessagesByTopic(topicId: number, viewerUserId?: number): Observable<ForumMessage[]> {
        let params = new HttpParams();
        if (viewerUserId != null) {
            params = params.set('viewerUserId', String(viewerUserId));
        }
        return this.http.get<ForumMessage[]>(`${TOPICS_URL}/${topicId}/messages`, { params });
    }

    getReplies(parentMessageId: number, viewerUserId?: number): Observable<ForumMessage[]> {
        let params = new HttpParams();
        if (viewerUserId != null) {
            params = params.set('viewerUserId', String(viewerUserId));
        }
        return this.http.get<ForumMessage[]>(`${API_URL}/replies/${parentMessageId}`, { params });
    }

    createMessage(request: CreateMessageRequest): Observable<ForumMessage> {
        const v = this.composeAssist.validateForSend(request.content);
        if (!v.ok) {
            return throwError(
                () =>
                    new HttpErrorResponse({
                        status: 400,
                        statusText: 'Bad Request',
                        error: { error: v.message }
                    })
            );
        }
        return this.http.post<ForumMessage>(`${TOPICS_URL}/${request.topicId}/messages`, {
            authorId: request.authorId,
            content: request.content,
            parentMessageId: request.parentMessageId ?? null,
            attachments: request.attachments ?? null
        });
    }

    updateMessage(id: number, content: string, attachments?: string | null): Observable<ForumMessage> {
        const v = this.composeAssist.validateForSend(content);
        if (!v.ok) {
            return throwError(
                () =>
                    new HttpErrorResponse({
                        status: 400,
                        statusText: 'Bad Request',
                        error: { error: v.message }
                    })
            );
        }
        const body: { content: string; attachments?: string | null } = { content };
        if (attachments !== undefined) {
            body.attachments = attachments;
        }
        return this.http.put<ForumMessage>(`${API_URL}/${id}`, body);
    }

    deleteMessage(id: number): Observable<void> {
        return this.http.delete<void>(`${API_URL}/${id}`);
    }
}
