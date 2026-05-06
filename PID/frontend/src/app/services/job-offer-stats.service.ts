import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';

export interface OfferPopularity {
  id: number;
  title: string;
  company: string;
  contractType: string;
  applicationCount: number;
  fillRate: number;
}

export interface JobOfferStats {
  totalOffers: number;
  activeOffers: number;
  expiredOffers: number;
  totalApplications: number;
  mostPopularOfferTitle: string;
  mostPopularOfferCompany: string;
  mostPopularOfferApplications: number;
  avgSalaryByContractType: { [key: string]: number };
  applicationsByStatus: { [key: string]: number };
  offersByContractType: { [key: string]: number };
  fillRateByOffer: { [key: string]: number };
  top5Offers: OfferPopularity[];
}

@Injectable({ providedIn: 'root' })
export class JobOfferStatsService {
  /** apiUrl already ends with /api (gateway) — do not prefix /api again. */
  private readonly base = `${API_URL}/joboffers-stats`;

  constructor(private http: HttpClient) {}

  getStats(): Observable<JobOfferStats> {
    return this.http.get<JobOfferStats>(this.base);
  }
}