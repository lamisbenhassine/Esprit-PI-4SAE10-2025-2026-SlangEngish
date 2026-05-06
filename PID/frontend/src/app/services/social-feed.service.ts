import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

const API = '/api/forum';

export interface FeedPost {
  id: number;
  title: string;
  description: string;
  authorId: number;
  authorFirstName?: string | null;
  authorLastName?: string | null;
  /** STUDENT | TUTOR | ADMIN */
  authorRole?: string | null;
  coverImageUrl?: string | null;
  coverVideoUrl?: string | null;
  /** GENERAL | LEVEL | COURSE */
  spaceType?: string | null;
  spaceKey?: string | null;
  spaceTitle?: string | null;
  category: string;
  views: number;
  createdAt?: string;
  likeCount: number;
  likedByViewer: boolean;
  commentCount: number;
  repostCount: number;
  repostedByViewer: boolean;
  locked?: boolean;
}

export interface LikeResponse {
  liked: boolean;
  likeCount: number;
}

export interface RepostResponse {
  reposted: boolean;
  repostCount: number;
}

export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED';

export interface ForumInvitation {
  id?: number;
  fromUserId: number;
  toUserId: number;
  message: string;
  status: InvitationStatus;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class SocialFeedService {
  constructor(private http: HttpClient) {}

  getFeed(viewerUserId: number): Observable<FeedPost[]> {
    const params = new HttpParams().set('viewerUserId', String(viewerUserId));
    return this.http.get<FeedPost[]>(`${API}/feed`, { params });
  }

  toggleLike(topicId: number, userId: number): Observable<LikeResponse> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.post<LikeResponse>(`${API}/feed/topics/${topicId}/like`, null, { params });
  }

  toggleRepost(topicId: number, userId: number): Observable<RepostResponse> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.post<RepostResponse>(`${API}/feed/topics/${topicId}/repost`, null, { params });
  }

  getInbox(userId: number): Observable<ForumInvitation[]> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.get<ForumInvitation[]>(`${API}/invitations/inbox`, { params });
  }

  sendInvitation(fromUserId: number, toUserId: number, message: string): Observable<ForumInvitation> {
    return this.http.post<ForumInvitation>(`${API}/invitations`, { fromUserId, toUserId, message });
  }

  respondInvitation(id: number, userId: number, accept: boolean): Observable<ForumInvitation> {
    return this.http.post<ForumInvitation>(`${API}/invitations/${id}/respond`, { userId, accept });
  }
}
