import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';

export interface Reclamation {
  id?: number;
  sujet: string;
  description: string;
  studentId?: number;
  statut?: string;
  reponseAdmin?: string;
  createdAt?: string;
}

export interface TraiterReclamationPayload {
  statut: string;
  reponseAdmin: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReclamationService {
  private readonly apiUrl = `${API_URL}/reclamations`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(this.apiUrl);
  }

  getByStudent(studentId: number): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(`${this.apiUrl}?studentId=${studentId}`);
  }

  getUnreadNotifications(studentId: number): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(`${this.apiUrl}/notifications?studentId=${studentId}`);
  }

  getById(id: number): Observable<Reclamation> {
    return this.http.get<Reclamation>(`${this.apiUrl}/${id}`);
  }

  create(reclamation: Reclamation): Observable<Reclamation> {
    return this.http.post<Reclamation>(this.apiUrl, reclamation);
  }

  update(id: number, reclamation: Reclamation): Observable<Reclamation> {
    return this.http.put<Reclamation>(`${this.apiUrl}/${id}`, reclamation);
  }

  traiterParAdmin(id: number, payload: TraiterReclamationPayload): Observable<Reclamation> {
    return this.http.put<Reclamation>(`${this.apiUrl}/${id}/traitement`, payload);
  }

  markNotificationAsRead(id: number): Observable<Reclamation> {
    return this.http.put<Reclamation>(`${this.apiUrl}/${id}/notifications/read`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
