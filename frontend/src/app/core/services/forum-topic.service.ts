import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map, retry } from 'rxjs/operators';

// Use relative base URL so Angular proxy can route to backend (dev) or gateway (prod).
const API_BASE_URL = '/api/forum';
const API_URL = `${API_BASE_URL}/topics`;
const SPACES_URL = `${API_BASE_URL}/spaces`;

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

export interface ForumSpace {
    id?: number;
    type: 'GENERAL' | 'LEVEL' | 'COURSE';
    key: string;
    title: string;
    isPublic: boolean;
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
            map((body: any) => Array.isArray(body) ? body : []),
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

    getTopicsBySpace(spaceId: number, userId?: number): Observable<ForumTopic[]> {
        let params = new HttpParams();
        if (userId != null) {
            params = params.set('userId', userId.toString());
        }
        return this.http.get<ForumTopic[]>(`${SPACES_URL}/${spaceId}/topics`, { params }).pipe(
            catchError(this.handleError)
        );
    }

    getCourseSpace(courseKey: string, userId: number): Observable<ForumSpace> {
        const params = new HttpParams().set('userId', userId.toString());
        return this.http.get<ForumSpace>(`${SPACES_URL}/course/${courseKey}`, { params }).pipe(
            catchError(this.handleError)
        );
    }

    getGeneralSpace(): Observable<ForumSpace> {
        return this.http.get<ForumSpace>(`${SPACES_URL}/general`).pipe(
            catchError(this.handleError)
        );
    }

    createTopic(topic: ForumTopic): Observable<ForumTopic> {
        return this.http.post<ForumTopic>(API_URL, topic).pipe(
            catchError(this.handleError)
        );
    }

    createTopicInSpace(spaceId: number, request: { userId: number; authorId: number; title: string; description: string; }): Observable<ForumTopic> {
        return this.http.post<ForumTopic>(`${SPACES_URL}/${spaceId}/topics`, request).pipe(
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
