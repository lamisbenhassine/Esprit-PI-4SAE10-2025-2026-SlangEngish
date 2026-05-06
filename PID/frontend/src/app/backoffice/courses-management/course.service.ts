import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Course } from '../../models/course.model';
import { GESTION_COURS_API_BASE } from '../../api-config';

@Injectable({
  providedIn: 'root'
})
export class CourseService {
  private readonly apiUrl = `${GESTION_COURS_API_BASE}/course`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Course[]> {
    return this.http.get<Course[]>(`${this.apiUrl}/all`);
  }

  create(course: Course): Observable<Course> {
    return this.http.post<Course>(`${this.apiUrl}/add`, course);
  }

  update(id: number, course: Course): Observable<Course> {
    return this.http.put<Course>(`${this.apiUrl}/update/${id}`, course);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/delete/${id}`);
  }
}

