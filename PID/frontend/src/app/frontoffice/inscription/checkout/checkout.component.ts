import { Component, OnInit } from '@angular/core';
import { OrderService, CreateOrderRequest } from '../../../core/services/order.service';
import { PaymentService, PaymentStatusResponse } from '../../../core/services/payment.service';
import { CartService, Cart } from '../../../core/services/cart.service';
import { PromoService, PromoValidationResult } from '../../../core/services/promo.service';
import { LoyaltyService, LoyaltySummary, LoyaltyRedemptionPreview } from '../../../core/services/loyalty.service';
import { AuthService } from '../../../services/auth.service';
import { UserContextService } from '../../../services/user-context.service';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit {
  step = 1;
  orderId = 0;
  orderNumber = '';
  orderAmount = 0;
  transactionId = '';
  isProcessing = false;

  cart: Cart | null = null;
  cartLoading = true;

  promoCodeInput = '';
  appliedPromo: PromoValidationResult | null = null;
  promoApplying = false;

  // Loyalty points (Métier 6)
  loyaltySummary: LoyaltySummary | null = null;
  loyaltyPreview: LoyaltyRedemptionPreview | null = null;
  loyaltyPointsToUse: number = 0;
  loyaltyLoading = false;

  private stripePublishableKey = '';

  constructor(
    private orderService: OrderService,
    private paymentService: PaymentService,
    private cartService: CartService,
    private promoService: PromoService,
    private loyaltyService: LoyaltyService,
    private auth: AuthService,
    private userContext: UserContextService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {
    const navState = this.router.getCurrentNavigation()?.extras?.state;
    if (navState?.['cart']) {
      this.cart = navState['cart'] as Cart;
      this.cartLoading = false;
      // Loyalty summary depends on cart total
      queueMicrotask(() => this.loadLoyaltySummary());
    }
    if (navState?.['appliedPromo']) {
      this.appliedPromo = navState['appliedPromo'];
      this.promoCodeInput = this.appliedPromo?.code ?? '';
      // Promo affects totals → refresh loyalty max discount
      queueMicrotask(() => this.loadLoyaltySummary());
    }
  }

  ngOnInit(): void {
    // Load Stripe publishable key from backend config
    this.paymentService.getStripeConfig().subscribe({
      next: (cfg) => {
        this.stripePublishableKey = cfg.publishableKey;
      },
      error: () => {
        // Keep silent here; errors will be shown when user clicks Pay.
      }
    });

    // Handle return from Stripe: success or canceled
    this.route.queryParams.subscribe((params: Params) => {
      const success = params['success'] === 'true';
      const canceled = params['canceled'] === 'true';
      const qOrderId = params['orderId'];
      const qOrderNumber = params['orderNumber'];

      if (canceled && qOrderId) {
        this.orderId = +qOrderId;
        this.orderNumber = qOrderNumber || '';
        this.orderAmount = 0;
        this.step = 1;
        this.snackBar.open('Paiement annulé. Vous pouvez réessayer ou modifier votre commande.', 'OK', {
          duration: 5000,
          panelClass: ['warning-snackbar']
        });
        this.clearQueryParams();
        return;
      }
      if (success && qOrderId) {
        this.orderId = +qOrderId;
        this.orderNumber = qOrderNumber || '';
        this.step = 3;
        this.orderAmount = 0;
        this.transactionId = '';
        const stripeSessionId = params['session_id'] as string | undefined;
        const finish = () => {
          this.pollPaymentStatus();
          this.clearQueryParams();
        };
        if (stripeSessionId) {
          this.paymentService.verifyStripeCheckoutSession(stripeSessionId, this.orderId).subscribe({
            next: () => finish(),
            error: () => finish()
          });
        } else {
          finish();
        }
        return;
      }
    });

    // Always sync cart from backend so displayed cart = backend cart (fixes "cart is empty" on Confirm Order)
    this.cartService.getCartByUserId(this.effectiveUserId()).subscribe({
      next: (cart) => {
        this.cart = cart;
        this.cartLoading = false;
        this.loadLoyaltySummary();
      },
      error: () => {
        this.cartLoading = false;
      }
    });
  }

  /** Auth en priorité, sinon userId de session (sessionStorage), sinon 1. */
  private effectiveUserId(): number {
    const id = this.auth.getCurrentUserId();
    if (id != null && id > 0) {
      return id;
    }
    const ctx = this.userContext.getCurrentUserId();
    return ctx > 0 ? ctx : 1;
  }

  private parseApiErrorBody(raw: unknown): string | null {
    if (raw == null) {
      return null;
    }
    if (typeof raw === 'string') {
      try {
        const o = JSON.parse(raw) as { error?: string; detail?: string; message?: string };
        return o.error || o.detail || o.message || raw;
      } catch {
        return raw;
      }
    }
    if (typeof raw === 'object') {
      const o = raw as Record<string, unknown>;
      const first =
        (typeof o['error'] === 'string' && o['error']) ||
        (typeof o['detail'] === 'string' && o['detail']) ||
        (typeof o['message'] === 'string' && o['message']) ||
        (typeof o['title'] === 'string' && o['title']);
      if (typeof first === 'string') {
        return first;
      }
    }
    return null;
  }

  private clearQueryParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      queryParamsHandling: ''
    });
  }

  private pollPaymentStatus(): void {
    const maxAttempts = 15;
    const intervalMs = 2000;
    let attempts = 0;
    const check = () => {
      this.paymentService.getPaymentStatusByOrderId(this.orderId).subscribe({
        next: (res: PaymentStatusResponse) => {
          if (res.status === 'completed' && res.payment) {
            this.transactionId = res.payment.transactionId || '';
            this.orderAmount = res.payment.amount != null ? res.payment.amount : 0;
            return;
          }
          attempts++;
          if (attempts < maxAttempts) {
            setTimeout(check, intervalMs);
          } else {
            this.snackBar.open(
              'Paiement enregistré. Un email de confirmation vous a été envoyé (vérifiez vos spams si besoin).',
              'OK',
              { duration: 8000, panelClass: ['success-snackbar'] }
            );
          }
        },
        error: () => {
          attempts++;
          if (attempts < maxAttempts) {
            setTimeout(check, intervalMs);
          }
        }
      });
    };
    check();
  }

  get displayTotal(): number {
    if (this.appliedPromo?.valid && this.appliedPromo.totalAfterDiscount != null && this.cart?.totalAmount != null) {
      return this.appliedPromo.totalAfterDiscount;
    }
    return this.cart?.totalAmount ?? 0;
  }

  get displayTotalAfterLoyalty(): number {
    if (this.loyaltyPreview && this.loyaltyPreview.finalTotal != null) {
      return this.loyaltyPreview.finalTotal;
    }
    return this.displayTotal;
  }

  applyPromo(): void {
    if (!this.promoCodeInput.trim() || !this.cart?.totalAmount) return;
    this.promoApplying = true;
    this.promoService.validate(this.promoCodeInput.trim(), this.cart.totalAmount).subscribe({
      next: (result) => {
        this.promoApplying = false;
        this.appliedPromo = result;
        // Promo changes total → recompute loyalty
        this.loyaltyPreview = null;
        this.loadLoyaltySummary();
        if (result.valid) {
          this.snackBar.open(`Code "${result.code}" appliqué : -${result.discountAmount?.toFixed(2)} €`, 'OK', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });
        } else {
          this.snackBar.open(result.message || 'Code invalide.', 'OK', {
            duration: 4000,
            panelClass: ['error-snackbar']
          });
        }
      },
      error: () => {
        this.promoApplying = false;
        this.snackBar.open('Impossible de vérifier le code promo.', 'OK', { duration: 3000 });
      }
    });
  }

  removePromo(): void {
    this.appliedPromo = null;
    this.promoCodeInput = '';
    this.loyaltyPreview = null;
    this.loadLoyaltySummary();
  }

  loadLoyaltySummary(): void {
    if (!this.cart || !this.cart.totalAmount) {
      this.loyaltySummary = null;
      this.loyaltyPreview = null;
      return;
    }
    this.loyaltyLoading = true;
    this.loyaltyService.getSummary(this.effectiveUserId(), this.displayTotal).subscribe({
      next: (summary) => {
        this.loyaltySummary = summary;
        this.loyaltyLoading = false;
      },
      error: (err) => {
        this.loyaltyLoading = false;
        this.loyaltySummary = null;
        this.loyaltyPreview = null;
        console.warn('Loyalty API error:', err?.status, err?.statusText || err?.message, err?.url);
      }
    });
  }

  previewLoyalty(): void {
    if (!this.loyaltySummary || !this.cart || !this.cart.totalAmount) {
      return;
    }
    const pts = Math.max(0, Math.floor(this.loyaltyPointsToUse || 0));
    if (pts <= 0) {
      this.loyaltyPreview = null;
      return;
    }
    this.loyaltyLoading = true;
    this.loyaltyService.previewRedemption(this.effectiveUserId(), this.displayTotal, pts).subscribe({
      next: (preview) => {
        this.loyaltyPreview = preview;
        this.loyaltyLoading = false;
      },
      error: () => {
        this.loyaltyLoading = false;
        this.loyaltyPreview = null;
      }
    });
  }

  createOrder() {
    if (this.isProcessing) return;
    this.isProcessing = true;

    const request: CreateOrderRequest = {
      userId: this.effectiveUserId(),
      paymentMethod: 'Stripe',
      promoCode: this.appliedPromo?.valid ? this.appliedPromo.code : undefined
    };

    const doCreateOrder = () => {
      this.orderService.createOrderFromCart(request).subscribe({
        next: (order) => {
          this.orderId = order.id ?? 0;
          this.orderNumber = order.orderNumber;
          this.orderAmount = order.totalAmount ?? 0;
          // Sync cart (should now be empty) so navbar badge clears
          this.cartService.clearCart(this.effectiveUserId()).subscribe({
            next: () => {},
            error: () => {}
          });
          this.isProcessing = false;
          // Immediately launch Stripe Checkout
          this.processPayment();
        },
        error: (err) => this.handleOrderError(err)
      });
    };

    // Re-fetch cart to avoid "cart is empty" when backend was out of sync; if GET fails, still try with current cart
    this.cartService.getCartByUserId(this.effectiveUserId()).subscribe({
      next: (backendCart) => {
        if (!backendCart?.cartItems?.length) {
          this.isProcessing = false;
          this.cart = backendCart || null;
          this.snackBar.open(
            'Votre panier est vide côté serveur. Rechargez la page panier ou réajoutez votre offre.',
            'OK',
            { duration: 8000, panelClass: ['error-snackbar'] }
          );
          return;
        }
        doCreateOrder();
      },
      error: () => {
        // GET failed (e.g. server down or CORS): if we have items in memory, try creating order anyway
        if (this.cart?.cartItems?.length) {
          doCreateOrder();
        } else {
          this.isProcessing = false;
          this.snackBar.open('Impossible de charger le panier. Vérifiez la gateway (8100) et le service inscription (8050).', 'CLOSE', {
            duration: 6000,
            panelClass: ['error-snackbar']
          });
        }
      }
    });
  }

  private handleOrderError(err: any) {
    this.isProcessing = false;
    const raw = err?.error;
    console.error('Order creation error:', err?.status, raw);
    let msg = 'Échec de la création de la commande. Réessayez.';
    if (err.status === 0) {
      msg = 'Serveur injoignable. Vérifiez la gateway (8100) et le microservice inscription (8050).';
    } else if (err.status === 400) {
      msg = this.parseApiErrorBody(raw) || msg;
    }
    this.snackBar.open(msg, 'CLOSE', {
      duration: 8000,
      panelClass: ['error-snackbar']
    });
  }

  processPayment() {
    if (this.isProcessing) return;
    this.isProcessing = true;

    // Stripe Checkout: ask backend for a Checkout Session, then redirect with Stripe.js
    const successUrl =
      window.location.origin +
      '/frontoffice/inscription/checkout?success=true&orderId=' +
      this.orderId +
      '&orderNumber=' +
      encodeURIComponent(this.orderNumber || '') +
      '&session_id={CHECKOUT_SESSION_ID}';
    const cancelUrl = window.location.origin + '/frontoffice/inscription/checkout?canceled=true&orderId=' + this.orderId + '&orderNumber=' + encodeURIComponent(this.orderNumber || '');

    const loyaltyPoints = Math.max(0, Math.floor(this.loyaltyPointsToUse || 0));

    this.paymentService.createStripeCheckoutSession(this.orderId, successUrl, cancelUrl, loyaltyPoints).subscribe({
      next: async (res) => {
        const stripePublicKey = this.stripePublishableKey;
        if (!stripePublicKey) {
          this.isProcessing = false;
          this.snackBar.open('Stripe publishable key is not configured on backend (stripe.publishable-key).', 'OK', {
            duration: 7000,
            panelClass: ['error-snackbar']
          });
          return;
        }

        const stripe = await this.loadStripe(stripePublicKey);
        if (!stripe) {
          this.isProcessing = false;
          this.snackBar.open('Unable to initialize Stripe.', 'OK', { duration: 4000 });
          return;
        }
        await stripe.redirectToCheckout({ sessionId: res.sessionId });
      },
      error: (err) => {
        this.isProcessing = false;
        console.error('Stripe checkout error:', err);
        const msg = err?.error?.error || 'Payment initialization failed. Please try again.';
        this.snackBar.open(msg, 'OK', {
          duration: 7000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  private loadStripe(publishableKey: string): Promise<any> {
    const w = window as any;
    if (w.Stripe) return Promise.resolve(w.Stripe(publishableKey));

    return new Promise((resolve) => {
      const existing = document.querySelector('script[data-stripejs="true"]') as HTMLScriptElement | null;
      if (existing) {
        existing.addEventListener('load', () => resolve(w.Stripe ? w.Stripe(publishableKey) : null));
        existing.addEventListener('error', () => resolve(null));
        return;
      }

      const script = document.createElement('script');
      script.src = 'https://js.stripe.com/v3/';
      script.async = true;
      script.defer = true;
      script.dataset['stripejs'] = 'true';
      script.onload = () => resolve(w.Stripe ? w.Stripe(publishableKey) : null);
      script.onerror = () => resolve(null);
      document.head.appendChild(script);
    });
  }

  goToDashboard() {
    this.router.navigate(['/frontoffice/dashboard']);
  }

  startLearning() {
    this.router.navigate(['/frontoffice/courses']);
  }
}
