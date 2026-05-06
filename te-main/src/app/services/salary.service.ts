import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SalaryPrediction {
  jobTitle: string;
  salaryMonthlyTND: number;
  salaryAnnualTND: number;
  salaryRangeLow: number;
  salaryRangeHigh: number;
  salaryUSD: number;
  confidence: number;
  currency: string;
  status: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class SalaryService {

  private apiUrl = '/api/salary';

  constructor(private http: HttpClient) {}

  predictFromOffer(offerId: number): Observable<SalaryPrediction> {
    return this.http.get<SalaryPrediction>(
      `${this.apiUrl}/predict/offer/${offerId}`
    );
  }

  predictFromTitle(jobTitle: string): Observable<SalaryPrediction> {
    return this.http.get<SalaryPrediction>(
      `${this.apiUrl}/predict?jobTitle=${encodeURIComponent(jobTitle)}`
    );
  }
}