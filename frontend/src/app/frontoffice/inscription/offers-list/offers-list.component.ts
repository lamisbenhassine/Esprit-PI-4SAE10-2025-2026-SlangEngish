import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { SubscriptionPlanService, SubscriptionPlan, CostPerMonthDTO, AnnualComparisonDTO, PricePerUserDTO, SiblingDiscountDTO, RecommendationDTO } from '../../../core/services/subscription-plan.service';
import { CartService, AddItemRequest } from '../../../core/services/cart.service';
import { ProrataService, ProrataCalculateResponse } from '../../../core/services/prorata.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-offers-list',
  templateUrl: './offers-list.component.html',
  styleUrls: ['./offers-list.component.css']
})
export class OffersListComponent implements OnInit {
  plans: SubscriptionPlan[] = [];
  filteredPlans: SubscriptionPlan[] = [];
  loading = false;
  error: string = '';
  success: string = '';
  demoMode = false;
  currentUserId = 1;

  // Search and filter properties
  searchQuery: string = '';
  sortBy: string = 'date';
  sortOrder: 'asc' | 'desc' = 'desc';
  selectedCategory: string = '';  // '' = all, 'general', 'medical', 'engineering', 'business'

  // Pagination properties
  currentPage: number = 1;
  itemsPerPage: number = 6;
  totalPages: number = 1;
  recommendedPlan: SubscriptionPlan | null = null;
  recommendedPlanIdFromQuery: number | null = null;

  // Métier avancé 1 (Duolingo) : coût par mois, comparaison annuelle, recommandation
  costPerMonthByPlanId: Record<number, CostPerMonthDTO> = {};
  annualComparison: AnnualComparisonDTO | null = null;
  familyPriceExample: PricePerUserDTO | null = null;
  siblingDiscountExample: SiblingDiscountDTO | null = null;
  recommendation: RecommendationDTO | null = null;
  recommendationMinutes: number = 20;
  recommendationLevel: string = 'B1';
  recommendationGoal: string = '';

  // Formulaire interactif : tarif famille et réduction conviviale (frères/sœurs)
  familyNumberOfPeople = 6;
  familySelectedPlanId: number | null = null;
  familyComputeResult: PricePerUserDTO | null = null;
  siblingNumberOfSiblings = 2;
  siblingSelectedPlanId: number | null = null;
  siblingComputeResult: SiblingDiscountDTO | null = null;

  // Prorata upgrade/downgrade (métier avancé 2)
  prorataCurrentPlanId: number | null = null;
  prorataNewPlanId: number | null = null;
  prorataPricePaid: number = 79;
  prorataDurationDays: number = 30;
  prorataStartDate: string = '';
  prorataResult: ProrataCalculateResponse | null = null;

  Math = Math; // Expose Math to template

  constructor(
    private subscriptionPlanService: SubscriptionPlanService,
    private cartService: CartService,
    private prorataService: ProrataService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['level']) {
        this.searchQuery = params['level'];
      }
      if (params['planId']) {
        const id = Number(params['planId']);
        this.recommendedPlanIdFromQuery = isNaN(id) ? null : id;
      }
      this.loadPlans();
    });
  }

  // Mock plans for fallback when backend is unavailable
  private getMockPlans(): SubscriptionPlan[] {
    return [
      { id: 1, planType: 'A1', name: 'Discovery Plan', price: 49, durationDays: 30, description: 'Perfect for absolute beginners. Start your English journey with confidence.', category: 'general' },
      { id: 2, planType: 'A2', name: 'Starter Plan', price: 79, durationDays: 60, description: 'Build your foundation in everyday English conversations.', category: 'general' },
      { id: 3, planType: 'B1', name: 'Intermediate Plan', price: 99, durationDays: 90, description: 'Expand your vocabulary and fluency in professional settings.', category: 'general' },
      { id: 4, planType: 'B2', name: 'Advanced Plan', price: 129, durationDays: 90, description: 'Master complex grammar and business English for career growth.', category: 'general' },
      { id: 5, planType: 'C1', name: 'Proficiency Plan', price: 159, durationDays: 120, description: 'Near-native level fluency. Ideal for academic and executive roles.', category: 'general' },
      { id: 6, planType: 'C2', name: 'Mastery Plan', price: 199, durationDays: 180, description: 'Complete mastery of English at native level.', category: 'general' },
      { id: 7, planType: 'B1', name: 'Medical English Pro', price: 149, durationDays: 90, description: 'Specialized English for healthcare professionals. Covers clinical terminology, patient communication, and medical reports.', category: 'medical' },
      { id: 8, planType: 'B2', name: 'Medical English Advanced', price: 179, durationDays: 120, description: 'Advanced English for doctors and specialists. Research writing, conferences, and international collaboration.', category: 'medical' },
      { id: 9, planType: 'B1', name: 'Engineering English Pro', price: 139, durationDays: 90, description: 'Technical English for engineers. Covers report writing, project communication, and technical presentations.', category: 'engineering' },
      { id: 10, planType: 'B2', name: 'Engineering English Advanced', price: 169, durationDays: 120, description: 'Advanced technical English for senior engineers and project managers.', category: 'engineering' },
      { id: 11, planType: 'B2', name: 'Business English Pro', price: 149, durationDays: 90, description: 'Master business communication, negotiations, and boardroom English.', category: 'business' },
      { id: 12, planType: 'C1', name: 'Business English Executive', price: 189, durationDays: 120, description: 'Executive-level business English for leadership and international management.', category: 'business' },
    ];
  }

  loadPlans() {
    this.loading = true;
    this.subscriptionPlanService.getAllPlans().subscribe({
      next: (data) => {
        this.demoMode = false;
        this.plans = Array.isArray(data) ? data : [];
        this.applyFilters();
        this.initFamilySiblingPlanIds();
        this.loadPricingMetier();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading plans from backend:', err);
        this.loading = false;
        this.demoMode = false;
        this.error = 'Unable to load subscription offers. Please make sure the Inscription service (port 8030) is running.';
      }
    });
  }

  /** Charge les calculs du métier avancé (chaque requête indépendante pour afficher famille/fratrie même si annuel échoue). */
  loadPricingMetier() {
    const firstPlan = this.plans.length > 0 ? this.plans[0] : null;
    const firstPlanId = firstPlan?.id != null ? firstPlan.id : null;

    const requests: Record<string, import('rxjs').Observable<unknown>> = {
      costAll: this.subscriptionPlanService.getCostPerMonthAll().pipe(catchError(() => of(null))),
      annual: this.subscriptionPlanService.compareAnnualAuto().pipe(catchError(() => of(null)))
    };
    if (firstPlanId != null) {
      requests['family'] = this.subscriptionPlanService.getPricePerUser(firstPlanId, 6).pipe(catchError(() => of(null)));
      requests['sibling'] = this.subscriptionPlanService.getSiblingDiscount(firstPlanId, 2).pipe(catchError(() => of(null)));
    }
    forkJoin(requests).subscribe({
      next: (res: { costAll?: CostPerMonthDTO[] | null; annual?: AnnualComparisonDTO | null; family?: PricePerUserDTO | null; sibling?: SiblingDiscountDTO | null }) => {
        this.costPerMonthByPlanId = {};
        const costAll = res.costAll;
        if (Array.isArray(costAll)) {
          costAll.forEach((dto: CostPerMonthDTO) => {
            if (dto?.planId != null) this.costPerMonthByPlanId[dto.planId] = dto;
          });
        }
        this.annualComparison = res.annual ?? null;
        this.familyPriceExample = res.family ?? null;
        this.siblingDiscountExample = res.sibling ?? null;
        // Fallback tarif famille si backend n'a pas répondu : calcul local (prix / 6)
        if (this.familyPriceExample == null && firstPlan && (firstPlan.price != null && firstPlan.price > 0)) {
          this.familyPriceExample = {
            planId: firstPlan.id!,
            planName: firstPlan.name || firstPlan.planType || '',
            totalPrice: firstPlan.price,
            numberOfUsers: 6,
            pricePerUser: firstPlan.price / 6,
            currency: firstPlan.currency || 'TND'
          };
        }
      },
      error: () => {
        this.costPerMonthByPlanId = {};
        this.annualComparison = null;
        this.familyPriceExample = null;
        this.siblingDiscountExample = null;
        if (firstPlan && (firstPlan.price != null && firstPlan.price > 0)) {
          this.familyPriceExample = {
            planId: firstPlan.id!,
            planName: firstPlan.name || firstPlan.planType || '',
            totalPrice: firstPlan.price,
            numberOfUsers: 6,
            pricePerUser: firstPlan.price / 6,
            currency: firstPlan.currency || 'TND'
          };
        }
      }
    });
  }

  loadRecommendation() {
    this.subscriptionPlanService.getRecommendation({
      minutesPerDay: this.recommendationMinutes,
      level: this.recommendationLevel || undefined,
      goal: this.recommendationGoal || undefined
    }).subscribe({
      next: (dto) => this.recommendation = dto,
      error: () => {
        // Fallback si backend indisponible : score et message simulés (même formule que le backend)
        const min = this.recommendationMinutes || 0;
        let score = 50;
        if (min >= 20) score = 80;
        else if (min >= 10) score = 65;
        else if (min > 0) score = 40;
        this.recommendation = {
          score,
          message: min >= 20
            ? `Idéal : vous étudiez ${min} min/jour. Niveau ${this.recommendationLevel || '-'}. (Backend hors ligne : score simulé.)`
            : `Recommandé si vous pouvez consacrer au moins 15–20 min/jour. (Backend hors ligne : score simulé.)`,
          suggestedLevel: this.recommendationLevel || undefined,
          recommendedPlanId: undefined,
          recommendedPlanName: undefined
        };
      }
    });
  }

  /** Initialise les plans sélectionnés pour famille/fratrie et prorata. */
  initFamilySiblingPlanIds(): void {
    const first = this.plans[0];
    const second = this.plans[1];
    if (first?.id != null && this.familySelectedPlanId == null) this.familySelectedPlanId = first.id;
    if (first?.id != null && this.siblingSelectedPlanId == null) this.siblingSelectedPlanId = first.id;
    if (first?.id != null && this.prorataCurrentPlanId == null) this.prorataCurrentPlanId = first.id;
    if (second?.id != null && this.prorataNewPlanId == null) this.prorataNewPlanId = second.id;
    if (!this.prorataStartDate) {
      const d = new Date();
      d.setDate(d.getDate() - 10);
      this.prorataStartDate = d.toISOString().slice(0, 10);
    }
  }

  /** Calcule le tarif famille (prix par personne) selon le formulaire. */
  computeFamilyPrice(): void {
    const planId = this.familySelectedPlanId ?? this.plans[0]?.id;
    if (planId == null || this.familyNumberOfPeople < 1) {
      this.familyComputeResult = null;
      return;
    }
    const plan = this.plans.find(p => p.id === planId);
    this.subscriptionPlanService.getPricePerUser(planId, this.familyNumberOfPeople).pipe(
      catchError(() => of(null))
    ).subscribe({
      next: (dto) => {
        if (dto) {
          this.familyComputeResult = dto;
        } else if (plan && (plan.price != null && plan.price > 0)) {
          this.familyComputeResult = {
            planId: plan.id!,
            planName: plan.name || plan.planType || '',
            totalPrice: plan.price,
            numberOfUsers: this.familyNumberOfPeople,
            pricePerUser: plan.price / this.familyNumberOfPeople,
            currency: plan.currency || 'TND'
          };
        } else {
          this.familyComputeResult = null;
        }
      }
    });
  }

  /** Calcule la réduction conviviale (frères/sœurs) selon le formulaire. */
  computeSiblingDiscount(): void {
    const planId = this.siblingSelectedPlanId ?? this.plans[0]?.id;
    if (planId == null || this.siblingNumberOfSiblings < 2) {
      this.siblingComputeResult = null;
      return;
    }
    const plan = this.plans.find(p => p.id === planId);
    this.subscriptionPlanService.getSiblingDiscount(planId, this.siblingNumberOfSiblings).pipe(
      catchError(() => of(null))
    ).subscribe({
      next: (dto) => {
        if (dto) {
          this.siblingComputeResult = dto;
        } else if (plan && (plan.price != null && plan.price > 0)) {
          const percent = this.siblingNumberOfSiblings >= 4 ? 15 : this.siblingNumberOfSiblings === 3 ? 12 : 10;
          const totalBase = plan.price * this.siblingNumberOfSiblings;
          const discountAmount = totalBase * percent / 100;
          const totalAfter = totalBase - discountAmount;
          this.siblingComputeResult = {
            planId: plan.id!,
            planName: plan.name || plan.planType || '',
            basePricePerPerson: plan.price,
            numberOfSiblings: this.siblingNumberOfSiblings,
            discountPercent: percent,
            discountAmount,
            totalAfterDiscount: totalAfter,
            pricePerPersonAfterDiscount: totalAfter / this.siblingNumberOfSiblings,
            currency: plan.currency || 'TND'
          };
        } else {
          this.siblingComputeResult = null;
        }
      }
    });
  }

  /** Prorata: credit for remaining time, amount to pay or refund. Backend first, fallback to client-side calc. */
  computeProrata(): void {
    const currentId = this.prorataCurrentPlanId ?? this.plans[0]?.id;
    const newId = this.prorataNewPlanId ?? this.plans[1]?.id;
    if (currentId == null || newId == null || currentId === newId) {
      this.snackBar.open('Choose different current and new plans.', 'OK', { duration: 3000 });
      this.prorataResult = null;
      return;
    }
    if (!this.prorataStartDate || this.prorataDurationDays < 1 || this.prorataPricePaid == null || this.prorataPricePaid < 0) {
      this.snackBar.open('Fill start date, duration and price paid.', 'OK', { duration: 3000 });
      this.prorataResult = null;
      return;
    }
    const currentPlan = this.plans.find(p => p.id === currentId);
    const newPlan = this.plans.find(p => p.id === newId);
    this.prorataService.calculate({
      currentPlanId: currentId,
      newPlanId: newId,
      pricePaid: this.prorataPricePaid,
      durationDays: this.prorataDurationDays,
      subscriptionStartDate: this.prorataStartDate
    }).pipe(
      catchError(() => of(null))
    ).subscribe({
      next: (res) => {
        if (res) {
          this.prorataResult = res;
        } else {
          this.prorataResult = this.computeProrataFallback(currentPlan, newPlan);
        }
      }
    });
  }

  private computeProrataFallback(currentPlan: SubscriptionPlan | undefined, newPlan: SubscriptionPlan | undefined): ProrataCalculateResponse | null {
    if (!newPlan) return null;
    const start = new Date(this.prorataStartDate);
    const end = new Date(start);
    end.setDate(end.getDate() + this.prorataDurationDays);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    end.setHours(0, 0, 0, 0);
    const daysRemaining = Math.max(0, Math.floor((end.getTime() - today.getTime()) / (24 * 60 * 60 * 1000)));
    const totalDays = Math.max(1, this.prorataDurationDays);
    const credits = (this.prorataPricePaid / totalDays) * daysRemaining;
    const newPrice = newPlan.price ?? 0;
    const newDuration = newPlan.durationDays && newPlan.durationDays > 0 ? newPlan.durationDays : 30;
    const newPlanProrataPrice = (newPrice / newDuration) * daysRemaining;
    let amountToPay = newPlanProrataPrice - credits;
    let refund = 0;
    if (amountToPay < 0) {
      refund = -amountToPay;
      amountToPay = 0;
    }
    return {
      currentPlanName: currentPlan?.name || currentPlan?.planType || 'Current plan',
      newPlanName: newPlan.name || newPlan.planType || 'New plan',
      currency: newPlan.currency || 'TND',
      daysRemaining,
      credits: Math.round(credits * 100) / 100,
      newPlanProrataPrice: Math.round(newPlanProrataPrice * 100) / 100,
      amountToPay: Math.round(amountToPay * 100) / 100,
      refund: Math.round(refund * 100) / 100
    };
  }

  /** Ajout au panier avec tarif famille (N personnes, prix par personne). */
  addToCartWithFamilyRate(): void {
    const planId = this.familySelectedPlanId ?? this.plans[0]?.id;
    if (planId == null || !this.familyComputeResult || this.familyNumberOfPeople < 2) {
      this.snackBar.open('Calculez d\'abord le tarif famille (plan + nombre de personnes).', 'OK', { duration: 4000 });
      return;
    }
    this.loading = true;
    this.error = '';
    this.cartService.addWithFamilyRate(this.currentUserId, planId, this.familyNumberOfPeople).subscribe({
      next: (cart) => {
        this.loading = false;
        this.router.navigate(['/frontoffice/inscription/cart'], { state: { cart } });
        this.snackBar.open(
          `Tarif famille : ${this.familyNumberOfPeople} personne(s) ajoutée(s) au panier.`,
          'OK',
          { duration: 5000 }
        );
      },
      error: (err) => {
        this.loading = false;
        const isNetworkError = !err?.status || err?.status === 0 || (err?.message && (err.message.includes('fetch') || err.message.includes('Network')));
        const msg = isNetworkError
          ? 'Service indisponible. Démarrez le microservice Inscription (port 8030).'
          : (typeof err?.error === 'string' ? err.error : err?.error?.message) || err?.message || 'Erreur lors de l\'ajout au panier.';
        this.error = msg;
        this.snackBar.open(msg, 'OK', { duration: 6000 });
      }
    });
  }

  /** Scénario métier complet : accès aux offres avec la réduction frères/sœurs appliquée au panier. */
  addToCartWithSiblingDiscount(): void {
    const planId = this.siblingSelectedPlanId ?? this.plans[0]?.id;
    const plan = this.plans.find(p => p.id === planId);
    if (planId == null || !this.siblingComputeResult || this.siblingNumberOfSiblings < 2) {
      this.snackBar.open('Étape 1 : choisissez un plan et le nombre de frères/sœurs, puis cliquez sur « Calculer la réduction ».', 'OK', { duration: 5000 });
      return;
    }
    this.loading = true;
    this.error = '';
    this.cartService.addWithSiblingDiscount(this.currentUserId, planId, this.siblingNumberOfSiblings).subscribe({
      next: (cart) => {
        this.loading = false;
        this.router.navigate(['/frontoffice/inscription/cart'], { state: { cart } });
        this.snackBar.open(
          `${this.siblingNumberOfSiblings} inscription(s) au tarif fratrie ajoutée(s). Total : ${this.siblingComputeResult?.currency ?? 'TND'} ${this.siblingComputeResult?.totalAfterDiscount ?? 0}`,
          'OK',
          { duration: 5000 }
        );
      },
      error: (err) => {
        this.loading = false;
        const isNetworkError = !err?.status || err?.status === 0 || (err?.message && (err.message.includes('fetch') || err.message.includes('Network')));
        const friendlyMessage = isNetworkError
          ? 'Service indisponible. Démarrez le microservice Inscription (port 8030) puis réessayez.'
          : (typeof err?.error === 'string' ? err.error : err?.error?.message) || err?.message || 'Impossible d\'ajouter au panier. Réessayez.';
        this.error = friendlyMessage;
        this.snackBar.open(friendlyMessage, 'OK', { duration: 7000 });
      }
    });
  }

  getCostPerMonth(plan: SubscriptionPlan): CostPerMonthDTO | null {
    return plan.id != null ? this.costPerMonthByPlanId[plan.id] || null : null;
  }

  /** Coût par mois pour affichage : backend si dispo, sinon calcul local (même formule). */
  getCostPerMonthDisplay(plan: SubscriptionPlan): { costPerMonth: number; currency: string } | null {
    const dto = this.getCostPerMonth(plan);
    if (dto) return { costPerMonth: dto.costPerMonth, currency: dto.currency };
    const price = plan.price ?? 0;
    const days = plan.durationDays ?? 30;
    if (days <= 0) return null;
    const months = days / 30;
    return { costPerMonth: price / months, currency: plan.currency || 'TND' };
  }

  applyFilters(): void {
    let filtered = [...this.plans];
    this.recommendedPlan = null;

    // 0. Filter by profession category first
    if (this.selectedCategory) {
      filtered = filtered.filter(p => (p.category || 'general') === this.selectedCategory);
    }

    // 1. Identify recommended plan
    // 1.a If a specific planId was provided in query params, use it first
    if (this.recommendedPlanIdFromQuery != null) {
      const idx = filtered.findIndex(p => p.id === this.recommendedPlanIdFromQuery);
      if (idx !== -1) {
        this.recommendedPlan = filtered[idx];
        filtered.splice(idx, 1);
      }
    }

    // 1.b Otherwise, use level/searchQuery to pick a recommended plan
    if (!this.recommendedPlan && this.searchQuery.trim()) {
      const query = this.searchQuery.toUpperCase();
      const recommendedIndex = filtered.findIndex(p => p.planType.toUpperCase() === query);

      if (recommendedIndex !== -1) {
        this.recommendedPlan = filtered[recommendedIndex];
        // Remove it from the main list so it's not duplicated
        filtered.splice(recommendedIndex, 1);
      }
    }

    // 2. Apply search filter for the rest
    if (this.searchQuery.trim() && !this.recommendedPlan) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(plan =>
        (plan.name || '').toLowerCase().includes(query) ||
        plan.planType.toLowerCase().includes(query) ||
        (plan.description || '').toLowerCase().includes(query)
      );
    }

    // 3. Apply sorting
    filtered.sort((a, b) => {
      let aValue: any, bValue: any;
      switch (this.sortBy) {
        case 'planType':
          aValue = a.planType.toLowerCase();
          bValue = b.planType.toLowerCase();
          break;
        case 'date':
          aValue = new Date(a.date || 0).getTime();
          bValue = new Date(b.date || 0).getTime();
          break;
        default: return 0;
      }
      if (aValue < bValue) return this.sortOrder === 'asc' ? -1 : 1;
      if (aValue > bValue) return this.sortOrder === 'asc' ? 1 : -1;
      return 0;
    });

    // 4. Calculate pagination
    this.totalPages = Math.ceil(filtered.length / this.itemsPerPage);
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.filteredPlans = filtered.slice(startIndex, endIndex);
  }

  onSearchChange(): void {
    this.currentPage = 1;
    this.applyFilters();
  }

  onSortChange(): void {
    this.currentPage = 1;
    this.applyFilters();
  }

  selectCategory(category: string): void {
    this.selectedCategory = category;
    this.currentPage = 1;
    this.applyFilters();
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.applyFilters();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = Math.min(5, this.totalPages);
    let startPage = Math.max(1, this.currentPage - Math.floor(maxPages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxPages - 1);

    if (endPage - startPage < maxPages - 1) {
      startPage = Math.max(1, endPage - maxPages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }

  getPlanImage(plan: SubscriptionPlan): string {
    if (plan.imageUrl) {
      return plan.imageUrl;
    }
    const colors = ['667eea', '764ba2', 'f093fb', '4facfe'];
    const index = (plan.id || 0) % colors.length;
    return `https://via.placeholder.com/300x200/${colors[index]}/ffffff?text=${plan.planType}`;
  }

  viewDetails(plan: SubscriptionPlan) {
    this.addToCart(plan);
  }

  addToCart(plan: SubscriptionPlan) {
    if (!plan.id) {
      this.error = 'Plan ID is missing';
      return;
    }

    const request: AddItemRequest = {
      userId: this.currentUserId,
      subscriptionPlanId: plan.id,
      unitPrice: plan.price ?? 0,
      planName: plan.name || plan.planType
    };

    console.log('Adding to cart:', request);
    this.loading = true;
    this.error = '';
    this.success = '';

    this.cartService.addItemToCart(request).subscribe({
      next: (cart) => {
        this.loading = false;
        this.snackBar.open(`${plan.name || plan.planType} added to cart!`, 'VIEW CART', {
          duration: 5000,
          panelClass: ['success-snackbar']
        }).onAction().subscribe(() => {
          this.router.navigate(['/frontoffice/inscription/cart'], { state: { cart } });
        });
        // Redirect with cart in state so cart page shows correct data even if GET races with POST
        this.router.navigate(['/frontoffice/inscription/cart'], { state: { cart } });
      },
      error: (err) => {
        console.error('Error adding to cart:', err);
        this.loading = false;
        this.snackBar.open('Error adding to cart. Please try again.', 'CLOSE', { duration: 3000 });
      }
    });
  }
}
