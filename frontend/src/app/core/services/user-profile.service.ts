import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

const API = '/api/user';

export interface UserProfile {
  id: number;
  firstName: string;
  lastName: string;
  email?: string;
  englishLevel?: string;
  subscriptionStatus?: string;
  accountRole?: string;
}

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  constructor(private http: HttpClient) {}

  getByRole(role: 'TUTOR' | 'STUDENT'): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${API}/by-role/${role}`);
  }

  getById(id: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${API}/${id}`);
  }

  /** Plusieurs utilisateurs en une requête (noms sur le fil / sujet). */
  lookup(ids: number[]): Observable<UserProfile[]> {
    const unique = [...new Set(ids.filter(id => id != null && !Number.isNaN(id)))];
    if (!unique.length) {
      return of([]);
    }
    return this.http.post<UserProfile[]>(`${API}/lookup`, unique);
  }
}
