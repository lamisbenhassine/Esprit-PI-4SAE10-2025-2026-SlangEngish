import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

const API_URL = `${environment.apiUrl}/inscription/prorata`;

export interface ProrataCalculateRequest {
  currentPlanId: number;
  newPlanId: number;
  pricePaid: number;
  durationDays: number;
  subscriptionStartDate: string; // yyyy-MM-dd
  changeDate?: string; // optional, yyyy-MM-dd
}

export interface ProrataCalculateResponse {
  currentPlanName: string;
  newPlanName: string;
  currency: string;
  daysRemaining: number;
  credits: number;
  newPlanProrataPrice: number;
  amountToPay: number;
  refund: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProrataService {
  constructor(private http: HttpClient) {}

  calculate(request: ProrataCalculateRequest): Observable<ProrataCalculateResponse> {
    return this.http.post<ProrataCalculateResponse>(`${API_URL}/calculate`, request).pipe(
      catchError((err: HttpErrorResponse) => throwError(() => err))
    );
  }
}
