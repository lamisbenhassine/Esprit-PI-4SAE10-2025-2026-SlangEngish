import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SubscriptionPlanService, SubscriptionPlan } from '../../../core/services/subscription-plan.service';
import { CartService, AddItemRequest } from '../../../core/services/cart.service';

@Component({
    selector: 'app-offer-detail',
    templateUrl: './offer-detail.component.html',
    styleUrls: ['./offer-detail.component.css']
})
export class OfferDetailComponent implements OnInit {
    plan: SubscriptionPlan | null = null;
    loading = false;
    error = '';
    success = '';
    currentUserId = 1;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private subscriptionPlanService: SubscriptionPlanService,
        private cartService: CartService
    ) { }

    ngOnInit(): void {
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.loadPlan(+id);
        } else {
            this.error = 'Invalid offer ID';
        }
    }

    loadPlan(id: number): void {
        this.loading = true;
        this.subscriptionPlanService.getPlanById(id).subscribe({
            next: (data) => {
                this.plan = data;
                this.loading = false;
            },
            error: (err) => {
                this.error = 'Error loading plan details';
                this.loading = false;
            }
        });
    }

    getPlanImage(plan: SubscriptionPlan): string {
        if (plan.imageUrl) return plan.imageUrl;
        const colors = ['667eea', '764ba2', 'f093fb', '4facfe'];
        const index = (plan.id || 0) % colors.length;
        return `https://via.placeholder.com/600x400/${colors[index]}/ffffff?text=${plan.planType}`;
    }

    chooseOffer(): void {
        if (!this.plan || !this.plan.id) return;

        const request: AddItemRequest = {
            userId: this.currentUserId,
            subscriptionPlanId: this.plan.id,
            unitPrice: this.plan.price ?? 0,
            planName: this.plan.name || this.plan.planType
        };

        this.loading = true;
        this.cartService.addItemToCart(request).subscribe({
            next: (cart) => {
                this.success = 'Added to cart! Redirecting...';
                setTimeout(() => {
                    this.router.navigate(['/frontoffice/inscription/cart'], { state: { cart } });
                }, 1500);
            },
            error: (err) => {
                this.error = 'Error adding to cart';
                this.loading = false;
            }
        });
    }

    goBack(): void {
        this.router.navigate(['/frontoffice/inscription/offers']);
    }
}
