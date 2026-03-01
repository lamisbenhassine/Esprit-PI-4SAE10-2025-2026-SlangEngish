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

@Injectable({
    providedIn: 'root'
})
export class PaymentService {
    constructor(private http: HttpClient) { }

    processPayment(request: ProcessPaymentRequest): Observable<Payment> {
        return this.http.post<Payment>(`${API_URL}/process`, request);
    }

    verifyUserPayment(userId: number): Observable<boolean> {
        return this.http.get<boolean>(`${API_URL}/verify/${userId}`);
    }
}
