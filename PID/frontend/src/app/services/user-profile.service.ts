import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  DEMO_USER_PROFILES,
  demoUserById,
  demoUsersByRole
} from '../core/data/demo-user-profiles';
import { UserProfile } from '../core/models/user-profile.types';

const API = '/api/users';

export type { UserProfile };

@Injectable({ providedIn: 'root' })
export class UserProfileService {
  constructor(private http: HttpClient) {}

  /** Endpoint réel users-service via gateway: GET /api/users */
  getAll(): Observable<UserProfile[]> {
    return this.http.get<unknown[]>(API).pipe(
      map(list => this.normalizeList(Array.isArray(list) ? list : [])),
      catchError(() => of([...DEMO_USER_PROFILES]))
    );
  }

  getByRole(role: 'TUTOR' | 'STUDENT'): Observable<UserProfile[]> {
    return this.http.get<unknown[]>(`${API}?role=${role}`).pipe(
      map(list => {
        const arr = this.normalizeList(Array.isArray(list) ? list : []);
        return arr.length ? arr : demoUsersByRole(role);
      }),
      catchError(() => of(demoUsersByRole(role)))
    );
  }

  getById(id: number): Observable<UserProfile> {
    return this.http.get<unknown>(`${API}/${id}`).pipe(
      map(row => this.normalizeUser(row)),
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
    // Le user-service n'expose pas /lookup ; on compose via GET /api/users/{id}.
    return forkJoin(
      unique.map(id =>
        this.http.get<unknown>(`${API}/${id}`).pipe(
          map(row => this.normalizeUser(row)),
          catchError(() => of(demoUserById(id) ?? null))
        )
      )
    ).pipe(
      map(rows => rows.filter((u): u is UserProfile => u != null))
    );
  }

  private normalizeList(fromApi: unknown[]): UserProfile[] {
    return fromApi
      .map(row => this.normalizeUser(row))
      .filter((u): u is UserProfile => !!u && typeof u.id === 'number' && u.id > 0);
  }

  private normalizeUser(raw: unknown): UserProfile {
    const row = (raw ?? {}) as Record<string, unknown>;
    const roleRaw = row['accountRole'] ?? row['role'];
    return {
      id: Number(row['id'] ?? 0),
      firstName: String(row['firstName'] ?? ''),
      lastName: String(row['lastName'] ?? ''),
      email: typeof row['email'] === 'string' ? row['email'] : undefined,
      englishLevel: typeof row['englishLevel'] === 'string' ? row['englishLevel'] : undefined,
      subscriptionStatus: typeof row['subscriptionStatus'] === 'string' ? row['subscriptionStatus'] : undefined,
      accountRole: typeof roleRaw === 'string' ? roleRaw : undefined
    };
  }
}
