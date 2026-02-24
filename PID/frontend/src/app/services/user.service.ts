import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type Role = 'ADMIN' | 'TUTOR' | 'STUDENT' | 'CLUB_MANAGER' | 'EMPLOYEE';

export interface User {
  id?: number;
  firstName: string;
  lastName: string;
  email: string;
  password?: string;
  role: Role;
  photoBase64?: string;
  phone?: string;
  address?: string;
  // Optional UI-only fields
  avatar?: string;
  status?: 'active' | 'inactive' | 'pending';
  joinDate?: string;
  lastActive?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private readonly apiUrl = 'http://localhost:8011/api/users';

  constructor(private http: HttpClient) {}

  getAll(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl);
  }

  getById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  create(user: User): Observable<User> {
    return this.http.post<User>(this.apiUrl, user);
  }

  update(id: number, user: User): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, user);
  }

  updateProfile(id: number, data: Pick<User, 'firstName' | 'lastName' | 'email' | 'phone' | 'address'>): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/profile`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

