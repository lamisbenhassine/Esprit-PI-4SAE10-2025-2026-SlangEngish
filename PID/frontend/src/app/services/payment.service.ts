import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';

const PAYMENT_API = `${API_URL}/inscription/payment`;

export interface Payment {
    id?: number;
    orderId: number;
    amount: number;
    method: string;
    status: string;
    transactionId: string;
}

export interface ProcessPaymentRequest {
    orderId: number;
    amount: number;
    method: string;
}

export interface StripeCheckoutSessionResponse {
    sessionId: string;
}

export interface StripeConfigResponse {
    publishableKey: string;
}

export interface PaymentStatusResponse {
    status: 'pending' | 'completed';
    payment?: Payment;
    orderId?: number;
}

@Injectable({
    providedIn: 'root'
})
export class PaymentService {
    constructor(private http: HttpClient) { }

    processPayment(request: ProcessPaymentRequest): Observable<Payment> {
        return this.http.post<Payment>(`${PAYMENT_API}/process`, request);
    }

    /** Finalise le paiement côté API après retour Stripe (sans webhook). */
    verifyStripeCheckoutSession(sessionId: string, expectedOrderId?: number): Observable<Payment> {
        const body: { sessionId: string; expectedOrderId?: number } = { sessionId };
        if (expectedOrderId != null && expectedOrderId > 0) {
            body.expectedOrderId = expectedOrderId;
        }
        return this.http.post<Payment>(`${PAYMENT_API}/stripe/verify-session`, body);
    }

    /**
     * Create a Stripe Checkout Session for a given order.
     * The backend returns a sessionId that we pass to Stripe.js.
     * Optionally sends loyaltyPoints to apply a real discount.
     */
    createStripeCheckoutSession(orderId: number, successUrl: string, cancelUrl: string, loyaltyPoints?: number): Observable<StripeCheckoutSessionResponse> {
        const body: any = { orderId, successUrl, cancelUrl };
        if (loyaltyPoints && loyaltyPoints > 0) {
            body.loyaltyPoints = Math.floor(loyaltyPoints);
        }
        return this.http.post<StripeCheckoutSessionResponse>(`${PAYMENT_API}/stripe/checkout-session`, body);
    }

    getStripeConfig(): Observable<StripeConfigResponse> {
        return this.http.get<StripeConfigResponse>(`${PAYMENT_API}/stripe/config`);
    }

    verifyUserPayment(userId: number): Observable<boolean> {
        return this.http.get<boolean>(`${PAYMENT_API}/verify/${userId}`);
    }

    getPaymentByOrderId(orderId: number): Observable<Payment> {
        return this.http.get<Payment>(`${PAYMENT_API}/order/${orderId}`);
    }

    /**
     * Statut du paiement (pour polling après retour Stripe). Retourne toujours 200.
     */
    getPaymentStatusByOrderId(orderId: number): Observable<PaymentStatusResponse> {
        return this.http.get<PaymentStatusResponse>(`${PAYMENT_API}/order/${orderId}/status`);
    }
}
