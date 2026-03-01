import { Component, OnInit } from '@angular/core';
import { CartService, Cart } from '../../../core/services/cart.service';
import { PromoService, PromoValidationResult } from '../../../core/services/promo.service';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-cart',
  template: `
    <div class="cart-page-container">
      <div class="step-indicator">
          <div class="step"><span>1</span> Personal Info</div>
          <div class="step-line active"></div>
          <div class="step active"><span>2</span> Choose Plan</div>
          <div class="step-line active"></div>
          <div class="step active"><span>3</span> Checkout</div>
      </div>

      <div class="cart-header">
        <h1>Review Your Selection</h1>
        <p>You're almost there! Finalize your order to start learning.</p>
      </div>

      <div *ngIf="loading" class="loading-state">
        <mat-spinner diameter="50"></mat-spinner>
      </div>

      <div class="cart-content" *ngIf="cart && cart.cartItems.length > 0; else emptyCart">
        <div class="cart-card">
          <div class="cart-items">
            <div class="cart-item" *ngFor="let item of cart.cartItems">
              <div class="item-icon">
                <mat-icon>subscriptions</mat-icon>
              </div>
              <div class="item-details">
                <h3>{{ item.planName }}</h3>
                <p>Full Professional Course Access</p>
              </div>
              <div class="item-price">
                {{ item.unitPrice }} TND<span *ngIf="(item.quantity || 0) > 1"> × {{ item.quantity }}</span>
                <span *ngIf="(item.quantity || 0) > 1 && item.totalPrice"> = {{ item.totalPrice }} TND</span>
              </div>
              <button class="remove-btn" (click)="removeItem(item)" mat-icon-button color="warn">
                <mat-icon>delete_outline</mat-icon>
              </button>
            </div>
          </div>

          <div class="cart-summary">
            <div class="summary-row">
              <span>Subtotal</span>
              <span>{{ cart.totalAmount }} TND</span>
            </div>
            <div class="summary-row" *ngIf="appliedPromo?.valid && appliedPromo?.discountAmount">
              <span>Réduction ({{ appliedPromo?.code }})</span>
              <span class="discount">-{{ appliedPromo?.discountAmount | number:'1.2-2' }} TND</span>
            </div>
            <div class="summary-row">
              <span>Tax (Included)</span>
              <span>0.00 TND</span>
            </div>
            <div class="summary-row total">
              <span>Total Amount</span>
              <span>{{ displayTotal | number:'1.2-2' }} TND</span>
            </div>

            <div class="promo-block">
              <h4 class="promo-title"><mat-icon>local_offer</mat-icon> Code promo</h4>
              <p class="promo-hint">Ex. BIENVENUE10 pour 10 % de réduction</p>
              <div class="promo-row" *ngIf="!appliedPromo?.valid">
                <input type="text" [(ngModel)]="promoCodeInput" placeholder="Entrez votre code"
                  (keyup.enter)="applyPromo()" class="promo-input">
                <button type="button" class="btn-promo" (click)="applyPromo()" [disabled]="promoApplying || !promoCodeInput.trim()">
                  <span *ngIf="!promoApplying">Appliquer</span>
                  <span *ngIf="promoApplying"><i class="fas fa-spinner fa-spin"></i></span>
                </button>
              </div>
              <div class="promo-applied" *ngIf="appliedPromo?.valid">
                <span><mat-icon>check_circle</mat-icon> {{ appliedPromo?.code }} (-{{ appliedPromo?.discountAmount | number:'1.2-2' }} TND)</span>
                <button type="button" class="btn-remove-promo" (click)="removePromo()">Retirer</button>
              </div>
            </div>

            <div class="summary-actions">
              <div class="left-actions">
                <button class="clear-btn" (click)="clearCart()" mat-button color="warn">Empty Cart</button>
                <button class="shopping-btn" routerLink="/frontoffice/inscription/offers" mat-button color="primary">
                  <mat-icon>add_shopping_cart</mat-icon>
                  Keep Shopping
                </button>
              </div>
              <button class="checkout-btn" (click)="proceedToCheckout()" mat-raised-button color="primary">
                Proceed to Payment
                <mat-icon>payments</mat-icon>
              </button>
            </div>
          </div>
        </div>
      </div>

      <ng-template #emptyCart>
        <div class="empty-cart-state" *ngIf="!loading">
          <mat-icon class="empty-icon">shopping_cart</mat-icon>
          <h3>Your cart is empty</h3>
          <p>Go back to the catalog to choose a learning plan.</p>
          <button mat-raised-button color="primary" routerLink="/frontoffice/inscription/offers">
            Browse Plans
          </button>
        </div>
      </ng-template>
    </div>
    `,
  styles: [`
      .cart-page-container { max-width: 1000px; margin: 40px auto; padding: 0 20px; }

      /* Step Indicator Styles */
      .step-indicator { display: flex; align-items: center; justify-content: center; margin-bottom: 50px; gap: 15px; }
      .step { display: flex; align-items: center; gap: 10px; color: #a0aec0; font-weight: 600; font-size: 14px; }
      .step span { width: 28px; height: 28px; border-radius: 50%; background: #edf2f7; display: flex; align-items: center; justify-content: center; font-size: 12px; }
      .step.active { color: #4361ee; }
      .step.active span { background: #4361ee; color: white; }
      .step-line { width: 40px; height: 2px; background: #edf2f7; }
      .step-line.active { background: #4361ee; }

      .cart-header { text-align: center; margin-bottom: 40px; }
      .cart-header h1 { font-size: 32px; font-weight: 800; color: #1a202c; margin-bottom: 8px; }
      .cart-header p { color: #718096; }

      .cart-card { background: white; border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.05); border: 1px solid #edf2f7; overflow: hidden; }
      .cart-items { padding: 30px; border-bottom: 1px solid #f7fafc; }
      .cart-item { display: flex; align-items: center; gap: 20px; padding: 15px 0; border-bottom: 1px solid #f7fafc; }
      .cart-item:last-child { border-bottom: none; }
      .item-icon { background: #f0f3ff; color: #4361ee; padding: 12px; border-radius: 12px; }
      .item-details { flex: 1; }
      .item-details h3 { margin: 0; font-size: 16px; font-weight: 700; color: #2d3748; }
      .item-details p { margin: 0; font-size: 12px; color: #718096; }
      .item-price { font-weight: 700; color: #1a202c; }

      .cart-summary { padding: 30px; background: #f8fafc; }
      .summary-row { display: flex; justify-content: space-between; margin-bottom: 12px; color: #4a5568; }
      .summary-row.total { margin-top: 20px; padding-top: 20px; border-top: 2px solid #edf2f7; color: #1a202c; font-weight: 800; font-size: 20px; }

      .summary-row.discount { color: #38a169; }
      .promo-block { margin-top: 20px; padding-top: 20px; border-top: 1px solid #edf2f7; }
      .promo-title { display: flex; align-items: center; gap: 8px; margin: 0 0 6px 0; font-size: 14px; color: #4a5568; }
      .promo-hint { font-size: 12px; color: #64748b; margin: 0 0 10px 0; }
      .promo-row { display: flex; gap: 10px; }
      .promo-input { flex: 1; padding: 10px 14px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 1rem; }
      .btn-promo { padding: 10px 20px; background: #4361ee; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; }
      .btn-promo:disabled { opacity: 0.6; cursor: not-allowed; }
      .promo-applied { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; background: #f0fff4; border-radius: 8px; color: #276749; }
      .btn-remove-promo { background: none; border: none; color: #4361ee; cursor: pointer; font-size: 0.9rem; }
      .summary-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 30px; }
      .left-actions { display: flex; gap: 10px; }
      .checkout-btn { padding: 0 40px !important; height: 50px !important; border-radius: 12px !important; font-weight: 700 !important; }

      .empty-cart-state { text-align: center; padding: 60px 20px; }
      .empty-icon { font-size: 64px; width: 64px; height: 64px; color: #cbd5e0; margin-bottom: 20px; }
      .empty-cart-state h3 { font-size: 24px; font-weight: 700; color: #2d3748; margin-bottom: 10px; }
      .empty-cart-state p { margin-bottom: 30px; color: #718096; }
    `]
})
export class CartComponent implements OnInit {
  cart: Cart | null = null;
  loading = false;
  currentUserId = 1;
  promoCodeInput = '';
  appliedPromo: PromoValidationResult | null = null;
  promoApplying = false;

  constructor(
    private cartService: CartService,
    private promoService: PromoService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    const navState = this.router.getCurrentNavigation()?.extras?.state;
    if (navState?.['cart']) {
      this.cart = navState['cart'] as Cart;
    }
  }

  ngOnInit() {
    this.cartService.cart$.subscribe(cart => {
      if (cart) this.cart = cart;
    });

    // Always load from backend so items have ids and state matches server (fixes remove + "cart is empty")
    this.loadCart();
  }

  get displayTotal(): number {
    console.log('💰 Calculating displayTotal:', {
      appliedPromo: this.appliedPromo,
      cartTotal: this.cart?.totalAmount
    });
    
    if (this.appliedPromo?.valid && this.appliedPromo.discountAmount != null && this.cart?.totalAmount != null) {
      // ✅ CORRECTION: Calculer le total après réduction
      const totalAfterDiscount = this.cart.totalAmount - this.appliedPromo.discountAmount;
      const finalTotal = Math.max(0, totalAfterDiscount); // Éviter les négatifs
      console.log('✅ Discount applied:', {
        original: this.cart.totalAmount,
        discount: this.appliedPromo.discountAmount,
        final: finalTotal
      });
      return finalTotal;
    }
    console.log('❌ No discount applied, returning:', this.cart?.totalAmount ?? 0);
    return this.cart?.totalAmount ?? 0;
  }

  applyPromo(): void {
    if (!this.promoCodeInput.trim()) {
      this.snackBar.open('Entrez un code promo.', 'OK', { duration: 3000 });
      return;
    }
    const amount = this.cart?.totalAmount != null ? Number(this.cart.totalAmount) : 0;
    if (amount <= 0) {
      this.snackBar.open('Le panier doit contenir au moins un article pour appliquer un code promo.', 'OK', { duration: 4000 });
      return;
    }
    this.promoApplying = true;
    this.promoService.validate(this.promoCodeInput.trim(), amount).subscribe({
      next: (result) => {
        this.promoApplying = false;
        this.appliedPromo = {
          valid: result.valid,
          message: result.message,
          code: result.code,
          discountAmount: result.discountAmount != null ? Number(result.discountAmount) : 0,
          totalAfterDiscount: result.totalAfterDiscount != null ? Number(result.totalAfterDiscount) : undefined
        };
        if (result.valid) {
          this.snackBar.open(`Code "${result.code}" appliqué : -${this.appliedPromo.discountAmount?.toFixed(2)} TND`, 'OK', { duration: 3000 });
        } else {
          this.snackBar.open(result.message || 'Code invalide ou inactif.', 'OK', { duration: 5000 });
        }
      },
      error: (error) => {
        this.promoApplying = false;
        this.snackBar.open('Service indisponible. Démarrez le microservice Inscription (port 8030) pour utiliser un code promo.', 'OK', { duration: 5000 });
      }
    });
  }

  removePromo(): void {
    this.appliedPromo = null;
    this.promoCodeInput = '';
  }

  loadCart() {
    this.loading = true;
    this.cartService.getCartByUserId(this.currentUserId).subscribe({
      next: (data) => {
        this.cart = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  removeItem(item: { id?: number; subscriptionPlanId: number }) {
    const userId = this.currentUserId;
    const req = item.id != null
      ? this.cartService.removeItemFromCart(userId, item.id)
      : this.cartService.removeItemByPlanId(userId, item.subscriptionPlanId);
    req.subscribe({
      next: (cart) => {
        this.cart = cart;
      },
      error: () => {
        this.snackBar.open('Erreur lors de la suppression. Rechargement du panier…', 'OK', { duration: 3000 });
        this.cartService.getCartByUserId(userId).subscribe({
          next: (data) => this.cart = data
        });
      }
    });
  }

  clearCart() {
    this.cartService.clearCart(this.currentUserId).subscribe({
      next: () => { this.cart = null; },
      error: () => this.loadCart()
    });
  }

  proceedToCheckout() {
    this.router.navigate(['/frontoffice/inscription/checkout'], {
      state: { cart: this.cart, appliedPromo: this.appliedPromo?.valid ? this.appliedPromo : null }
    });
  }
}
