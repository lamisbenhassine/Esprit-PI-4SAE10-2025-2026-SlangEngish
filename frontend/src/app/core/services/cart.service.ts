import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';

const API_URL = 'http://localhost:8030/api/inscription/cart';

export interface CartItem {
    id?: number;
    subscriptionPlanId: number;
    quantity?: number;
    unitPrice: number;
    totalPrice?: number;
    planName: string;
}

export interface Cart {
    id?: number;
    userId: number;
    totalAmount: number;
    cartItems: CartItem[];
}

export interface AddItemRequest {
    userId: number;
    subscriptionPlanId: number;
    unitPrice: number;
    planName: string;
}

@Injectable({
    providedIn: 'root'
})
export class CartService {
    private cartSubject = new BehaviorSubject<Cart | null>(null);
    public cart$ = this.cartSubject.asObservable();

    constructor(private http: HttpClient) { }

    getCartByUserId(userId: number): Observable<Cart> {
        return this.http.get<Cart>(`${API_URL}/${userId}`).pipe(
            tap(cart => this.cartSubject.next(cart))
        );
    }

    addItemToCart(request: AddItemRequest): Observable<Cart> {
        return this.http.post<Cart>(`${API_URL}/add`, request).pipe(
            tap(cart => this.cartSubject.next(cart))
        );
    }

    /** Métier avancé : ajout au panier avec réduction frères/sœurs (logique côté backend). */
    addWithSiblingDiscount(userId: number, subscriptionPlanId: number, siblingCount: number): Observable<Cart> {
        return this.http.post<Cart>(`${API_URL}/add-with-sibling-discount`, {
            userId,
            subscriptionPlanId,
            siblingCount
        }).pipe(
            tap(cart => this.cartSubject.next(cart))
        );
    }

    /** Métier avancé : ajout au panier en tarif famille (N personnes). */
    addWithFamilyRate(userId: number, subscriptionPlanId: number, numberOfPeople: number): Observable<Cart> {
        return this.http.post<Cart>(`${API_URL}/add-with-family-rate`, {
            userId,
            subscriptionPlanId,
            numberOfPeople
        }).pipe(
            tap(cart => this.cartSubject.next(cart))
        );
    }

    removeItemFromCart(userId: number, itemId: number): Observable<Cart> {
        return this.http.delete<Cart>(`${API_URL}/item/${itemId}/user/${userId}`).pipe(
            tap(cart => this.cartSubject.next(cart))
        );
    }

    removeItemByPlanId(userId: number, subscriptionPlanId: number): Observable<Cart> {
        return this.http.delete<Cart>(`${API_URL}/user/${userId}/plan/${subscriptionPlanId}`).pipe(
            tap(cart => this.cartSubject.next(cart))
        );
    }

    clearCart(userId: number): Observable<void> {
        return this.http.delete<void>(`${API_URL}/${userId}/clear`).pipe(
            tap(() => this.cartSubject.next({ userId, totalAmount: 0, cartItems: [] }))
        );
    }

    getCartCount(): Observable<number> {
        return new Observable<number>(observer => {
            this.cart$.subscribe(cart => {
                observer.next(cart ? cart.cartItems.length : 0);
            });
        });
    }
}
