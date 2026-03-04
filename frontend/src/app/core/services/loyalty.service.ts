import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Base URL : proxy (relative) en dev ng serve, direct (absolu) sinon */
function getLoyaltyBaseUrl(): string {
  if (typeof window !== 'undefined' && window.location.port === '4200') {
    return '/api/inscription/loyalty';
  }
  return 'http://localhost:8030/api/inscription/loyalty';
}

const API_URL = getLoyaltyBaseUrl();

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

  /**
   * Vue admin : liste de tous les comptes de fidélité.
   */
  getAllAccounts(): Observable<LoyaltySummary[]> {
    return this.http.get<LoyaltySummary[]>(`${API_URL}/admin/accounts`);
  }

  /**
   * Seed de comptes de démonstration (Bronze / Silver / Gold) pour le backoffice.
   */
  seedDemoAccounts(): Observable<LoyaltySummary[]> {
    return this.http.post<LoyaltySummary[]>(`${API_URL}/admin/seed-demo`, {});
  }
}

