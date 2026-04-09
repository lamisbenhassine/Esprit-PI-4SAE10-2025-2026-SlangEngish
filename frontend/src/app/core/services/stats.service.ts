import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardStats {
    totalOrders: number;
    totalPayments: number;
    totalRevenue: number;
    revenueToday: number;
    revenueThisMonth: number;
    newSubscriptionsToday: number;
    newSubscriptionsThisMonth: number;
    planStats: Array<{ planType: string, count: number, revenue: number }>;
    mostPopularPlan: string;
    completedPayments: number;
    pendingPayments: number;
    failedPayments: number;
    conversionRate: number;
    lastUpdated: string;
    recognizedRevenueByMonth?: Array<{
        year: number;
        month: number;
        recognizedAmount: number;
        orders: number;
    }>;
}

@Injectable({
    providedIn: 'root'
})
export class StatsService {
    /** Relatif : ng serve (proxy.conf.json → 8030) ou gateway en prod */
    private readonly apiUrl = '/api/inscription/stats';

    constructor(private http: HttpClient) { }

    getStats(): Observable<DashboardStats> {
        return this.http.get<DashboardStats>(this.apiUrl);
    }
}
