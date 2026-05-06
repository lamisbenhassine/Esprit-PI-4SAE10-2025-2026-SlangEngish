import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Stream } from '../../models/stream.model';
import { GESTION_COURS_API_BASE } from '../../api-config';

@Injectable({
  providedIn: 'root'
})
export class StreamService {

  private readonly apiUrl = `${GESTION_COURS_API_BASE}/stream`;

  constructor(private http: HttpClient) {}

  getLiveStreams(): Observable<Stream[]> {
    return this.http.get<Stream[]>(`${this.apiUrl}/live`);
  }
}

