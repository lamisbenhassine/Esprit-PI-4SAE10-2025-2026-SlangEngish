import { Component, OnInit } from '@angular/core';
import { OrderService, CreateOrderRequest } from '../../../core/services/order.service';
import { PaymentService, ProcessPaymentRequest } from '../../../core/services/payment.service';
import { CartService, Cart } from '../../../core/services/cart.service';
import { PromoService, PromoValidationResult } from '../../../core/services/promo.service';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit {
  step = 1;
  paymentMethod = 'Credit Card';
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

  constructor(
    private orderService: OrderService,
    private paymentService: PaymentService,
    private cartService: CartService,
    private promoService: PromoService,
    private router: Router,
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
      paymentMethod: this.paymentMethod,
      promoCode: this.appliedPromo?.valid ? this.appliedPromo.code : undefined
    };

    const doCreateOrder = () => {
      this.orderService.createOrderFromCart(request).subscribe({
        next: (order) => {
          this.orderId = order.id ?? 0;
          this.orderNumber = order.orderNumber;
          this.orderAmount = order.totalAmount ?? 0;
          this.isProcessing = false;
          this.step = 2;
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

    const request: ProcessPaymentRequest = {
      orderId: this.orderId,
      amount: this.orderAmount,
      method: this.paymentMethod
    };

    this.paymentService.processPayment(request).subscribe({
      next: (payment) => {
        this.transactionId = payment.transactionId;
        this.isProcessing = false;
        this.cart = null;
        this.step = 3; // ✅ Move to confirmation step

        this.snackBar.open('🎉 Payment successful! Your subscription is now active.', '', {
          duration: 5000,
          panelClass: ['success-snackbar']
        });
      },
      error: (err) => {
        this.isProcessing = false;
        console.error('Payment error:', err);
        let msg = 'Payment failed. Please try again.';
        if (err.status === 0) {
          msg = 'Cannot reach the server. Please make sure the backend is running.';
        }
        this.snackBar.open(msg, 'RETRY', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  selectMethod(method: string) {
    this.paymentMethod = method;
  }

  goToDashboard() {
    this.router.navigate(['/frontoffice/dashboard']);
  }

  startLearning() {
    this.router.navigate(['/frontoffice/courses']);
  }
}
