import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = 'http://localhost:8030/api/inscription/orders';

export interface Order {
    id?: number;
    orderNumber: string;
    userId: number;
    totalAmount: number;
    paymentMethod: string;
    discountAmount?: number;
    promoCode?: string;
}

export interface CreateOrderRequest {
    userId: number;
    paymentMethod: string;
    promoCode?: string;
}

@Injectable({
    providedIn: 'root'
})
export class OrderService {
    constructor(private http: HttpClient) { }

    createOrderFromCart(request: CreateOrderRequest): Observable<Order> {
        return this.http.post<Order>(`${API_URL}/create`, request);
    }
}
