import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';

const API_URL = 'http://localhost:8098/api/forum/topics';

export interface ForumTopic {
    id?: number;
    title: string;
    description: string;
    authorId: number;
    category: string;
    isPublic: boolean;
    views: number;
    createdAt?: string;
    updatedAt?: string;
}

@Injectable({
    providedIn: 'root'
})
export class ForumTopicService {
    constructor(private http: HttpClient) { }

    private handleError(error: HttpErrorResponse) {
        console.error('API Error:', error);
        return throwError(() => error);
    }

    getGeneralTopics(): Observable<ForumTopic[]> {
        return this.http.get<ForumTopic[]>(`${API_URL}/general`).pipe(
            retry(2),
            catchError(this.handleError)
        );
    }

    getTopicsByLevel(category: string, userId: number): Observable<ForumTopic[]> {
        const params = new HttpParams().set('userId', userId.toString());
        return this.http.get<ForumTopic[]>(`${API_URL}/level/${category}`, { params }).pipe(
            catchError(this.handleError)
        );
    }

    getTopicById(id: number): Observable<ForumTopic> {
        return this.http.get<ForumTopic>(`${API_URL}/${id}`).pipe(
            catchError(this.handleError)
        );
    }

    createTopic(topic: ForumTopic): Observable<ForumTopic> {
        return this.http.post<ForumTopic>(API_URL, topic).pipe(
            catchError(this.handleError)
        );
    }

    updateTopic(id: number, topic: ForumTopic): Observable<ForumTopic> {
        return this.http.put<ForumTopic>(`${API_URL}/${id}`, topic).pipe(
            catchError(this.handleError)
        );
    }

    deleteTopic(id: number): Observable<void> {
        return this.http.delete<void>(`${API_URL}/${id}`).pipe(
            catchError(this.handleError)
        );
    }
}
