import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  DEMO_USER_PROFILES,
  demoUserById,
  demoUsersByRole
} from '../data/demo-user-profiles';
import { UserProfile } from '../models/user-profile.types';

const API = '/api/user';

export type { UserProfile };

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  constructor(private http: HttpClient) {}

  /** Si l’API échoue ou renvoie [], on utilise les profils de démo (microservice user souvent arrêté en local). */
  getAll(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(API).pipe(
      map(list => this.pickUsers(Array.isArray(list) ? list : [])),
      catchError(() => of([...DEMO_USER_PROFILES]))
    );
  }

  getByRole(role: 'TUTOR' | 'STUDENT'): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${API}/by-role/${role}`).pipe(
      map(list => {
        const arr = Array.isArray(list) ? list : [];
        return arr.length ? arr : demoUsersByRole(role);
      }),
      catchError(() => of(demoUsersByRole(role)))
    );
  }

  getById(id: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${API}/${id}`).pipe(
      catchError(() => {
        const d = demoUserById(id);
        return d ? of(d) : throwError(() => new Error('User not found'));
      })
    );
  }

  /** Plusieurs utilisateurs en une requête (noms sur le fil / sujet). */
  lookup(ids: number[]): Observable<UserProfile[]> {
    const unique = [...new Set(ids.filter(id => id != null && !Number.isNaN(id)))];
    if (!unique.length) {
      return of([]);
    }
    return this.http.post<UserProfile[]>(`${API}/lookup`, unique).pipe(
      map(apiList => this.mergeLookup(unique, Array.isArray(apiList) ? apiList : [])),
      catchError(() => of(this.mergeLookup(unique, [])))
    );
  }

  private pickUsers(fromApi: UserProfile[]): UserProfile[] {
    return fromApi.length > 0 ? fromApi : [...DEMO_USER_PROFILES];
  }

  private mergeLookup(requestedIds: number[], apiRows: UserProfile[]): UserProfile[] {
    const fromApi = new Map(apiRows.map(u => [u.id, u]));
    return requestedIds
      .map(id => fromApi.get(id) ?? demoUserById(id))
      .filter((u): u is UserProfile => u != null);
  }
}
