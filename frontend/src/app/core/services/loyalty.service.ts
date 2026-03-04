import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = 'http://localhost:8030/api/inscription/loyalty';

export interface LoyaltySummary {
  userId: number;
  balancePoints: number;
  lifetimePoints: number;
  tier: string;
  maxDiscountAmount: number;
  maxDiscountPercent: number;
  discountPer100Points: number;
}

export interface LoyaltyRedemptionPreview {
  userId: number;
  orderTotal: number;
  requestedPoints: number;
  appliedPoints: number;
  discountAmount: number;
  finalTotal: number;
  maxPossibleDiscount: number;
}

@Injectable({
  providedIn: 'root'
})
export class LoyaltyService {
  constructor(private http: HttpClient) {}

  getSummary(userId: number, orderTotal?: number): Observable<LoyaltySummary> {
    let params = new HttpParams();
    if (orderTotal != null) {
      params = params.set('orderTotal', String(orderTotal));
    }
    return this.http.get<LoyaltySummary>(`${API_URL}/${userId}/summary`, { params });
  }

  previewRedemption(userId: number, orderTotal: number, requestedPoints: number): Observable<LoyaltyRedemptionPreview> {
    return this.http.post<LoyaltyRedemptionPreview>(`${API_URL}/preview-redemption`, {
      userId,
      orderTotal,
      requestedPoints
    });
  }
}

