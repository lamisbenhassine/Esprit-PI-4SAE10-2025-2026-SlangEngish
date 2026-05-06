import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Stream, StreamRequest, StreamStatus } from '../../models/stream.model';
import { GESTION_COURS_API_BASE } from '../../api-config';

@Injectable({
  providedIn: 'root'
})
export class StreamAdminService {

  private readonly apiUrl = `${GESTION_COURS_API_BASE}/stream`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Stream[]> {
    return this.http.get<Stream[]>(`${this.apiUrl}/all`);
  }

  create(request: StreamRequest): Observable<Stream> {
    return this.http.post<Stream>(`${this.apiUrl}/create`, request);
  }

  update(id: number, request: StreamRequest): Observable<Stream> {
    return this.http.put<Stream>(`${this.apiUrl}/update/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/delete/${id}`);
  }

  updateStatus(stream: Stream, status: StreamStatus): Observable<Stream> {
    if (!stream.id) {
      throw new Error('Stream id is required to update status');
    }

    const request: StreamRequest = {
      title: stream.title,
      startTime: stream.startTime,
      status
    };

    return this.update(stream.id, request);
  }
}

