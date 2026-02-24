import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JobOffer } from '../models/job-offer.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class JobOfferService {
  private readonly base = `${environment.apiUrl}/api/joboffers`;

  constructor(private http: HttpClient) {}

  findAll(): Observable<JobOffer[]> {
    return this.http.get<JobOffer[]>(this.base);
  }

  findById(id: number): Observable<JobOffer> {
    return this.http.get<JobOffer>(`${this.base}/${id}`);
  }

  create(offer: JobOffer): Observable<JobOffer> {
    return this.http.post<JobOffer>(this.base, offer);
  }

  update(id: number, offer: JobOffer): Observable<JobOffer> {
    return this.http.put<JobOffer>(`${this.base}/${id}`, offer);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
