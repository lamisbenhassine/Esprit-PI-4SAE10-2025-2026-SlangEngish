import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map, retry } from 'rxjs/operators';

const API_URL = '/api/inscription/plans';

/** Métier avancé 1 (Duolingo) : coût par mois */
export interface CostPerMonthDTO {
  planId: number;
  planName: string;
  totalPrice: number;
  durationDays: number;
  durationMonths: number;
  costPerMonth: number;
  currency: string;
}

/** Métier avancé 1 : comparaison annuelle */
export interface AnnualComparisonDTO {
  monthlyPlanId: number;
  yearlyPlanId: number;
  monthlyPlanName: string;
  yearlyPlanName: string;
  monthlyPrice: number;
  yearlyPrice: number;
  monthlyPriceTimes12: number;
  savingsAmount: number;
  savingsPercent: number;
  yearlyCostPerMonth: number;
  currency: string;
}

/** Métier avancé 1 : plan famille prix par utilisateur */
export interface PricePerUserDTO {
  planId: number;
  planName: string;
  totalPrice: number;
  numberOfUsers: number;
  pricePerUser: number;
  currency: string;
}

/** Métier avancé 1 : recommandation (scoring) */
export interface RecommendationDTO {
  score: number;
  message: string;
  suggestedLevel?: string;
  recommendedPlanId?: number;
  recommendedPlanName?: string;
}

/** Réduction frères/sœurs : 2+ inscrits = réduction sur le total */
export interface SiblingDiscountDTO {
  planId: number;
  planName: string;
  basePricePerPerson: number;
  numberOfSiblings: number;
  discountPercent: number;
  discountAmount: number;
  totalAfterDiscount: number;
  pricePerPersonAfterDiscount: number;
  currency: string;
}

export interface SubscriptionPlan {
    id?: number;
    planType: string;
    name?: string;
    price?: number;
    currency?: string;
    durationDays?: number;
    description?: string;
    category?: string;  // e.g. 'general', 'medical', 'engineering', 'business'
    date?: string;
    userId?: number;
    courseId?: number;
    imageUrl?: string;
}

@Injectable({
    providedIn: 'root'
})
export class SubscriptionPlanService {
    constructor(private http: HttpClient) { }

    private handleError(error: HttpErrorResponse) {
        console.error('API Error:', error);
        return throwError(() => error);
    }

    getAllPlans(): Observable<SubscriptionPlan[]> {
        return this.http.get<SubscriptionPlan[]>(API_URL).pipe(
            retry(1),
            map((body: any) => Array.isArray(body) ? body : (body && Array.isArray(body.content) ? body.content : [])),
            catchError(this.handleError)
        );
    }

    getPlanById(id: number): Observable<SubscriptionPlan> {
        return this.http.get<SubscriptionPlan>(`${API_URL}/${id}`).pipe(
            catchError(this.handleError)
        );
    }

    createPlan(plan: SubscriptionPlan): Observable<SubscriptionPlan> {
        return this.http.post<SubscriptionPlan>(API_URL, plan).pipe(
            catchError(this.handleError)
        );
    }

    updatePlan(id: number, plan: SubscriptionPlan): Observable<SubscriptionPlan> {
        return this.http.put<SubscriptionPlan>(`${API_URL}/${id}`, plan).pipe(
            catchError(this.handleError)
        );
    }

    deletePlan(id: number): Observable<void> {
        return this.http.delete<void>(`${API_URL}/${id}`).pipe(
            catchError(this.handleError)
        );
    }

    /** Crée les offres par défaut côté backend si la table est vide. Retourne la liste des plans créés. */
    seedDefaultPlans(): Observable<SubscriptionPlan[]> {
        return this.http.post<SubscriptionPlan[]>(`${API_URL}/seed`, {}).pipe(
            map((body: any) => Array.isArray(body) ? body : []),
            catchError(this.handleError)
        );
    }

    // ─── Métier avancé 1 (style Duolingo) : calculs de tarification ─────────────────

    getCostPerMonth(planId: number): Observable<CostPerMonthDTO> {
        return this.http.get<CostPerMonthDTO>(`${API_URL}/${planId}/cost-per-month`).pipe(
            catchError(this.handleError)
        );
    }

    getCostPerMonthAll(): Observable<CostPerMonthDTO[]> {
        return this.http.get<CostPerMonthDTO[]>(`${API_URL}/cost-per-month-all`).pipe(
            catchError(this.handleError)
        );
    }

    compareAnnual(monthlyPlanId: number, yearlyPlanId: number): Observable<AnnualComparisonDTO> {
        return this.http.get<AnnualComparisonDTO>(`${API_URL}/compare-annual`, {
            params: { monthlyPlanId: String(monthlyPlanId), yearlyPlanId: String(yearlyPlanId) }
        }).pipe(catchError(this.handleError));
    }

    compareAnnualAuto(): Observable<AnnualComparisonDTO> {
        return this.http.get<AnnualComparisonDTO>(`${API_URL}/compare-annual-auto`).pipe(
            catchError(this.handleError)
        );
    }

    getPricePerUser(planId: number, users: number = 6): Observable<PricePerUserDTO> {
        return this.http.get<PricePerUserDTO>(`${API_URL}/${planId}/price-per-user`, {
            params: { users: String(users) }
        }).pipe(catchError(this.handleError));
    }

    getSiblingDiscount(planId: number, siblings: number = 2): Observable<SiblingDiscountDTO> {
        return this.http.get<SiblingDiscountDTO>(`${API_URL}/${planId}/sibling-discount`, {
            params: { siblings: String(siblings) }
        }).pipe(catchError(this.handleError));
    }

    getRecommendation(params: { minutesPerDay?: number; level?: string; goal?: string }): Observable<RecommendationDTO> {
        let httpParams: Record<string, string> = {};
        if (params.minutesPerDay != null) httpParams['minutesPerDay'] = String(params.minutesPerDay);
        if (params.level) httpParams['level'] = params.level;
        if (params.goal) httpParams['goal'] = params.goal;
        return this.http.get<RecommendationDTO>(`${API_URL}/recommendation`, { params: httpParams }).pipe(
            catchError(this.handleError)
        );
    }
}
