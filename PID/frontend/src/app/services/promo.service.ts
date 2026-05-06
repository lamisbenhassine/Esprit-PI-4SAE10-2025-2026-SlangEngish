import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

const API_URL = `${environment.apiUrl}/inscription/promo`;

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
