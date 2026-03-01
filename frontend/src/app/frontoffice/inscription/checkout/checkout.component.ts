import { Component, OnInit } from '@angular/core';
import { OrderService, CreateOrderRequest } from '../../../core/services/order.service';
import { PaymentService } from '../../../core/services/payment.service';
import { CartService, Cart } from '../../../core/services/cart.service';
import { PromoService, PromoValidationResult } from '../../../core/services/promo.service';
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
  currentUserId = 1;
  isProcessing = false;

  cart: Cart | null = null;
  cartLoading = true;

  promoCodeInput = '';
  appliedPromo: PromoValidationResult | null = null;
  promoApplying = false;

  private stripePublishableKey = '';

  constructor(
    private orderService: OrderService,
    private paymentService: PaymentService,
    private cartService: CartService,
    private promoService: PromoService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {
    const navState = this.router.getCurrentNavigation()?.extras?.state;
    if (navState?.['cart']) {
      this.cart = navState['cart'] as Cart;
      this.cartLoading = false;
    }
    if (navState?.['appliedPromo']) {
      this.appliedPromo = navState['appliedPromo'];
      this.promoCodeInput = this.appliedPromo?.code ?? '';
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
        // Fetch payment to get transactionId (after webhook has run)
        this.paymentService.getPaymentByOrderId(this.orderId).subscribe({
          next: (payment) => {
            this.transactionId = payment.transactionId || '';
            this.orderAmount = payment.amount != null ? payment.amount : 0;
          }
        });
        this.clearQueryParams();
        return;
      }
    });

    // Always sync cart from backend so displayed cart = backend cart (fixes "cart is empty" on Confirm Order)
    this.cartService.getCartByUserId(this.currentUserId).subscribe({
      next: (cart) => {
        this.cart = cart;
        this.cartLoading = false;
      },
      error: () => {
        this.cartLoading = false;
      }
    });
  }

  private clearQueryParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      queryParamsHandling: ''
    });
  }

  get displayTotal(): number {
    if (this.appliedPromo?.valid && this.appliedPromo.totalAfterDiscount != null && this.cart?.totalAmount != null) {
      return this.appliedPromo.totalAfterDiscount;
    }
    return this.cart?.totalAmount ?? 0;
  }

  applyPromo(): void {
    if (!this.promoCodeInput.trim() || !this.cart?.totalAmount) return;
    this.promoApplying = true;
    this.promoService.validate(this.promoCodeInput.trim(), this.cart.totalAmount).subscribe({
      next: (result) => {
        this.promoApplying = false;
        this.appliedPromo = result;
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
  }

  createOrder() {
    if (this.isProcessing) return;
    this.isProcessing = true;

    const request: CreateOrderRequest = {
      userId: this.currentUserId,
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
          this.cartService.clearCart(this.currentUserId).subscribe({
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
    this.cartService.getCartByUserId(this.currentUserId).subscribe({
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
          this.snackBar.open('Impossible de charger le panier. Vérifiez que le serveur (port 8030) est démarré.', 'CLOSE', {
            duration: 6000,
            panelClass: ['error-snackbar']
          });
        }
      }
    });
  }

  private handleOrderError(err: any) {
    this.isProcessing = false;
    console.error('Order creation error:', err);
    let msg = 'Échec de la création de la commande. Réessayez.';
    if (err.status === 0) {
      msg = 'Serveur injoignable. Vérifiez que le backend tourne sur le port 8030.';
    } else if (err.status === 400 && err.error?.error) {
      msg = err.error.error;
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
    const successUrl = window.location.origin + '/frontoffice/inscription/checkout?success=true&orderId=' + this.orderId + '&orderNumber=' + encodeURIComponent(this.orderNumber || '');
    const cancelUrl = window.location.origin + '/frontoffice/inscription/checkout?canceled=true&orderId=' + this.orderId + '&orderNumber=' + encodeURIComponent(this.orderNumber || '');

    this.paymentService.createStripeCheckoutSession(this.orderId, successUrl, cancelUrl).subscribe({
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
