import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

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
    /** Relatif : ng serve (proxy.conf.json → gateway 8100) */
    private readonly apiUrl = '/api/inscription/stats';

    constructor(private http: HttpClient) { }

    getStats(): Observable<DashboardStats> {
        return this.http.get<unknown>(this.apiUrl).pipe(
            map((raw) => this.normalizeDashboardStats(raw))
        );
    }

    /** BigDecimal / champs null côté Java → nombres utilisables dans les pipes Angular. */
    private normalizeDashboardStats(raw: unknown): DashboardStats {
        const o = raw as Record<string, unknown>;
        const n = (v: unknown): number => {
            if (v == null) {
                return 0;
            }
            if (typeof v === 'number' && !Number.isNaN(v)) {
                return v;
            }
            if (typeof v === 'string') {
                const x = parseFloat(v);
                return Number.isNaN(x) ? 0 : x;
            }
            if (typeof v === 'object' && v !== null && 'toString' in v) {
                const x = parseFloat(String(v));
                return Number.isNaN(x) ? 0 : x;
            }
            return 0;
        };
        const months = o['recognizedRevenueByMonth'];
        let recognized: DashboardStats['recognizedRevenueByMonth'] = undefined;
        if (Array.isArray(months)) {
            recognized = months.map((m) => {
                const row = m as Record<string, unknown>;
                return {
                    year: n(row['year']),
                    month: n(row['month']),
                    recognizedAmount: n(row['recognizedAmount']),
                    orders: n(row['orders'])
                };
            });
        }
        let planStats: DashboardStats['planStats'] = [];
        if (Array.isArray(o['planStats'])) {
            planStats = (o['planStats'] as Record<string, unknown>[]).map((p) => ({
                planType: typeof p['planType'] === 'string' ? p['planType'] : String(p['planType'] ?? ''),
                count: n(p['count']),
                revenue: n(p['revenue'])
            }));
        }
        return {
            totalOrders: n(o['totalOrders']),
            totalPayments: n(o['totalPayments']),
            totalRevenue: n(o['totalRevenue']),
            revenueToday: n(o['revenueToday']),
            revenueThisMonth: n(o['revenueThisMonth']),
            newSubscriptionsToday: n(o['newSubscriptionsToday']),
            newSubscriptionsThisMonth: n(o['newSubscriptionsThisMonth']),
            planStats,
            mostPopularPlan: typeof o['mostPopularPlan'] === 'string' ? o['mostPopularPlan'] : '—',
            completedPayments: n(o['completedPayments']),
            pendingPayments: n(o['pendingPayments']),
            failedPayments: n(o['failedPayments']),
            conversionRate: n(o['conversionRate']),
            lastUpdated: typeof o['lastUpdated'] === 'string' ? o['lastUpdated'] : '',
            recognizedRevenueByMonth: recognized
        };
    }
}
