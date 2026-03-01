import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = 'http://localhost:8030/api/inscription/promo';

export interface PromoValidationResult {
  valid: boolean;
  message?: string;
  code?: string;
  discountAmount?: number;
  totalAfterDiscount?: number;
}

@Injectable({
  providedIn: 'root'
})
export class PromoService {
  constructor(private http: HttpClient) {}

  validate(code: string, amount: number): Observable<PromoValidationResult> {
    return this.http.get<PromoValidationResult>(`${API_URL}/validate`, {
      params: { code, amount: String(amount) }
    });
  }
}
