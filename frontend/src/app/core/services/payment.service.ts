import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = 'http://localhost:8030/api/inscription/payment';

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

@Injectable({
    providedIn: 'root'
})
export class PaymentService {
    constructor(private http: HttpClient) { }

    processPayment(request: ProcessPaymentRequest): Observable<Payment> {
        return this.http.post<Payment>(`${API_URL}/process`, request);
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
        return this.http.post<StripeCheckoutSessionResponse>(`${API_URL}/stripe/checkout-session`, body);
    }

    getStripeConfig(): Observable<StripeConfigResponse> {
        return this.http.get<StripeConfigResponse>(`${API_URL}/stripe/config`);
    }

    verifyUserPayment(userId: number): Observable<boolean> {
        return this.http.get<boolean>(`${API_URL}/verify/${userId}`);
    }

    getPaymentByOrderId(orderId: number): Observable<Payment> {
        return this.http.get<Payment>(`${API_URL}/order/${orderId}`);
    }
}
