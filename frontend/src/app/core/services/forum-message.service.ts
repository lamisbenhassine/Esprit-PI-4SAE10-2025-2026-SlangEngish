import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = 'http://localhost:8098/api/forum/messages';

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
        return this.http.get<ForumMessage[]>(`${API_URL}/topic/${topicId}`);
    }

    getReplies(parentMessageId: number): Observable<ForumMessage[]> {
        return this.http.get<ForumMessage[]>(`${API_URL}/replies/${parentMessageId}`);
    }

    createMessage(request: CreateMessageRequest): Observable<ForumMessage> {
        return this.http.post<ForumMessage>(API_URL, request);
    }

    updateMessage(id: number, content: string): Observable<ForumMessage> {
        return this.http.put<ForumMessage>(`${API_URL}/${id}`, { content });
    }

    deleteMessage(id: number): Observable<void> {
        return this.http.delete<void>(`${API_URL}/${id}`);
    }
}
