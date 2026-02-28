import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SavedOffer } from '../models/job-offer.model';

@Injectable({ providedIn: 'root' })
export class SavedOfferService {
  private readonly baseUrl = `${environment.apiUrl}/api/saved-offers`;

  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<SavedOffer[]> {
    return this.http.get<SavedOffer[]>(this.baseUrl);
  }

  save(jobOfferId: number, studentId: number): Observable<SavedOffer> {
    const payload: Partial<SavedOffer> = {
      jobOfferId,
      studentId,
    };
    return this.http.post<SavedOffer>(this.baseUrl, payload);
  }

  unsave(savedOfferId: number): Observable<void> {
    const url = `${this.baseUrl}/${savedOfferId}/remove`;
    return this.http.delete<void>(url);
  }
}

