import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GESTION_CLASSES_API_BASE } from '../../api-config';
import {
  CreateOrUpdateTaskPayload,
  KanbanBoard,
  KanbanTask,
  MoveTaskPayload
} from './kanban.models';

@Injectable({
  providedIn: 'root'
})
export class KanbanService {
  private readonly apiUrl = `${GESTION_CLASSES_API_BASE}/kanban`;

  constructor(private readonly http: HttpClient) {}

  getBoard(userId: number): Observable<KanbanBoard> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.get<KanbanBoard>(`${this.apiUrl}/board`, { params });
  }

  createTask(userId: number, body: CreateOrUpdateTaskPayload): Observable<KanbanTask> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.post<KanbanTask>(`${this.apiUrl}/tasks`, body, { params });
  }

  updateTask(userId: number, taskId: number, body: CreateOrUpdateTaskPayload): Observable<KanbanTask> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.put<KanbanTask>(`${this.apiUrl}/tasks/${taskId}`, body, { params });
  }

  deleteTask(userId: number, taskId: number): Observable<void> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.delete<void>(`${this.apiUrl}/tasks/${taskId}`, { params });
  }

  moveTask(userId: number, taskId: number, body: MoveTaskPayload): Observable<KanbanTask> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.patch<KanbanTask>(`${this.apiUrl}/tasks/${taskId}/move`, body, { params });
  }
}

