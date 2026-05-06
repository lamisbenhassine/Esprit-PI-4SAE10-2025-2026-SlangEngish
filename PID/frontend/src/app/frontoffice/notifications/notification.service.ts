import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GESTION_COURS_API_BASE } from '../../api-config';

export type NotificationType = 'COURSE' | 'STREAM' | 'RECORDING';

export interface NotificationDto {
  id: number;
  userId: number;
  message: string;
  read: boolean;
  createdAt: string;
  type: NotificationType;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private readonly apiUrl = `${GESTION_COURS_API_BASE}/notifications`;

  constructor(private http: HttpClient) {}

  getNotifications(userId: number): Observable<NotificationDto[]> {
    return this.http.get<NotificationDto[]>(`${this.apiUrl}/${userId}`);
  }

  getUnreadNotifications(userId: number): Observable<NotificationDto[]> {
    return this.http.get<NotificationDto[]>(`${this.apiUrl}/unread/${userId}`);
  }

  markAsRead(id: number): Observable<NotificationDto> {
    return this.http.put<NotificationDto>(`${this.apiUrl}/mark-as-read/${id}`, {});
  }

  deleteNotification(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/delete/${id}`);
  }
}

