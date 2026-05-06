import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map, retry } from 'rxjs/operators';
import { ComposeAssistService } from './compose-assist.service';

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
    coverImageUrl?: string | null;
    coverVideoUrl?: string | null;
    pinned?: boolean | null;
    locked?: boolean | null;
    /** Présent sur certains endpoints (ex. sujets sans réponse) pour afficher l’espace forum. */
    space?: ForumSpace | null;
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
    constructor(
        private http: HttpClient,
        private composeAssist: ComposeAssistService
    ) {}

    private rejectIfTopicTextBad(
        title: string | null | undefined,
        description: string | null | undefined
    ): Observable<never> | null {
        const vt = this.composeAssist.validateForSend(title ?? '');
        if (!vt.ok) {
            return throwError(
                () =>
                    new HttpErrorResponse({
                        status: 400,
                        statusText: 'Bad Request',
                        error: { error: vt.message }
                    })
            );
        }
        const vd = this.composeAssist.validateForSend(description ?? '');
        if (!vd.ok) {
            return throwError(
                () =>
                    new HttpErrorResponse({
                        status: 400,
                        statusText: 'Bad Request',
                        error: { error: vd.message }
                    })
            );
        }
        return null;
    }

    private handleError(error: HttpErrorResponse) {
        console.error('API Error:', error);
        return throwError(() => error);
    }

    /**
     * Sujets sans aucun message dans le fil (priorité aux plus anciens côté API).
     * Utile back-office tuteurs / équipe.
     */
    getUnansweredTopics(limit = 40, viewerUserId?: number): Observable<ForumTopic[]> {
        let params = new HttpParams().set('limit', String(limit));
        if (viewerUserId != null) {
            params = params.set('viewerUserId', String(viewerUserId));
        }
        return this.http.get<ForumTopic[]>(`${API_URL}/unanswered`, { params }).pipe(
            map((body: unknown) => (Array.isArray(body) ? body : []) as ForumTopic[]),
            catchError(this.handleError)
        );
    }

    getGeneralTopics(viewerUserId?: number): Observable<ForumTopic[]> {
        let params = new HttpParams();
        if (viewerUserId != null) {
            params = params.set('viewerUserId', String(viewerUserId));
        }
        return this.http.get<ForumTopic[]>(`${API_URL}/general`, { params }).pipe(
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

    /** Espace forum pour un niveau CECRL (A1…C2) — nécessite un abonnement côté API. */
    getLevelSpace(level: string, userId: number): Observable<ForumSpace> {
        const params = new HttpParams()
            .set('userId', String(userId));
        return this.http.get<ForumSpace>(`${SPACES_URL}/level/${encodeURIComponent(level)}`, { params }).pipe(
            catchError(this.handleError)
        );
    }

    createTopic(topic: ForumTopic): Observable<ForumTopic> {
        const bad = this.rejectIfTopicTextBad(topic.title, topic.description);
        if (bad) {
            return bad;
        }
        return this.http.post<ForumTopic>(API_URL, topic).pipe(
            catchError(this.handleError)
        );
    }

    createTopicInSpace(
        spaceId: number,
        request: {
            userId: number;
            authorId: number;
            title: string;
            description: string;
            coverImageUrl?: string | null;
            coverVideoUrl?: string | null;
        }
    ): Observable<ForumTopic> {
        const bad = this.rejectIfTopicTextBad(request.title, request.description);
        if (bad) {
            return bad;
        }
        return this.http.post<ForumTopic>(`${SPACES_URL}/${spaceId}/topics`, request).pipe(
            catchError(this.handleError)
        );
    }

    moderateTopic(id: number, pinned?: boolean, locked?: boolean): Observable<ForumTopic> {
        let params = new HttpParams();
        if (pinned !== undefined) {
            params = params.set('pinned', String(pinned));
        }
        if (locked !== undefined) {
            params = params.set('locked', String(locked));
        }
        return this.http.patch<ForumTopic>(`${API_URL}/${id}/moderation`, null, { params }).pipe(
            catchError(this.handleError)
        );
    }

    updateTopic(id: number, topic: ForumTopic): Observable<ForumTopic> {
        const bad = this.rejectIfTopicTextBad(topic.title, topic.description);
        if (bad) {
            return bad;
        }
        return this.http.put<ForumTopic>(`${API_URL}/${id}`, topic).pipe(
            catchError(this.handleError)
        );
    }

    /**
     * @param actorUserId Si défini, l’API n’accepte la suppression que si l’utilisateur est l’auteur du sujet.
     */
    deleteTopic(id: number, actorUserId?: number): Observable<void> {
        let params = new HttpParams();
        if (actorUserId != null) {
            params = params.set('actorUserId', String(actorUserId));
        }
        return this.http.delete<void>(`${API_URL}/${id}`, { params }).pipe(
            catchError(this.handleError)
        );
    }
}
