import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { SubscriptionDialogComponent } from './subscription-dialog/subscription-dialog.component';
import { SubscriptionPlanService, SubscriptionPlan } from '../../core/services/subscription-plan.service';

@Component({
    selector: 'app-subscription-management',
    templateUrl: './subscription-management.component.html',
    styleUrls: ['./subscription-management.component.css']
})
export class SubscriptionManagementComponent implements OnInit {
    plans: SubscriptionPlan[] = [];
    filteredPlans: SubscriptionPlan[] = [];
    loading = false;
    loadingSeed = false;

    // Search and filter properties
    searchQuery: string = '';
    sortBy: 'planType' | 'date' = 'date';
    sortOrder: 'asc' | 'desc' = 'desc';

    // Pagination properties
    currentPage: number = 1;
    itemsPerPage: number = 6;
    totalPages: number = 1;
    /** Total after search/sort (before pagination slice) */
    filteredTotalCount: number = 0;

    Math = Math; // Expose Math to template

    /** All searchable text for a plan (name, badges, dates, etc.) */
    private planSearchText(plan: SubscriptionPlan): string {
        const d = plan.date ? new Date(plan.date) : null;
        const parts = [
            plan.planType,
            plan.name,
            plan.description,
            plan.category,
            plan.date,
            d && !isNaN(d.getTime()) ? d.toLocaleDateString() : '',
            d && !isNaN(d.getTime()) ? d.toISOString().slice(0, 10) : ''
        ];
        return parts
            .filter((p): p is string => p != null && String(p).trim() !== '')
            .join(' ')
            .toLowerCase();
    }

    private matchesSearchQuery(plan: SubscriptionPlan, rawQuery: string): boolean {
        const query = rawQuery.trim().toLowerCase();
        if (!query) {
            return true;
        }
        const haystack = this.planSearchText(plan);
        const tokens = query.split(/\s+/).filter(Boolean);
        return tokens.every((t) => haystack.includes(t));
    }

    // Helper to get image for the plan - use uploaded image or fallback to placeholder
    getPlanImage(plan: SubscriptionPlan): string {
        // Debug: log the entire plan object
        console.log(`Plan ${plan.id} (${plan.planType}):`, {
            id: plan.id,
            planType: plan.planType,
            imageUrl: plan.imageUrl,
            imageUrlType: typeof plan.imageUrl,
            imageUrlLength: plan.imageUrl?.length,
            hasImageUrl: !!plan.imageUrl,
            imageUrlTrimmed: plan.imageUrl?.trim()
        });

        // Check if imageUrl exists and is not empty
        if (plan.imageUrl && plan.imageUrl.trim() !== '') {
            console.log(`✓ Using uploaded image for plan ${plan.id}`);
            return plan.imageUrl;
        }
        // Fallback to placeholder
        const colors = ['667eea', '764ba2', 'f093fb', '4facfe'];
        const index = (plan.id || 0) % colors.length;
        const placeholderUrl = `https://via.placeholder.com/300x200/${colors[index]}/ffffff?text=${plan.planType}`;
        console.log(`✗ Using placeholder for plan ${plan.id} - imageUrl is:`, plan.imageUrl);
        return placeholderUrl;
    }

    onImageError(event: any, plan: SubscriptionPlan): void {
        console.error(`Image load error for plan ${plan.id}:`, {
            imageUrl: plan.imageUrl,
            error: event
        });
        // Fallback to placeholder on error
        const colors = ['667eea', '764ba2', 'f093fb', '4facfe'];
        const index = (plan.id || 0) % colors.length;
        event.target.src = `https://via.placeholder.com/300x200/${colors[index]}/ffffff?text=${plan.planType}`;
    }

    constructor(
        private subscriptionPlanService: SubscriptionPlanService,
        private snackBar: MatSnackBar,
        private dialog: MatDialog
    ) { }

    ngOnInit(): void {
        this.allPlans();
    }

    loadDefaultOffers(): void {
        this.loadingSeed = true;
        this.subscriptionPlanService.seedDefaultPlans().subscribe({
            next: (created) => {
                this.loadingSeed = false;
                if (created.length > 0) {
                    this.snackBar.open(`✓ ${created.length} default offers created`, 'Close', {
                        duration: 3000,
                        panelClass: ['success-snackbar']
                    });
                    this.allPlans();
                } else {
                    this.snackBar.open('Offers already exist. Refreshing list.', 'Close', { duration: 3000 });
                    this.allPlans();
                }
            },
            error: (err) => {
                this.loadingSeed = false;
                console.error('Seed error:', err);
                if (err.status === 200) {
                    this.allPlans();
                    return;
                }
                const msg = err.status ? `HTTP ${err.status}` : (err.message || 'Backend unreachable. Is the inscription service running on port 8030?');
                this.snackBar.open(`✕ Could not load default offers: ${msg}`, 'Close', { duration: 7000 });
            }
        });
    }

    allPlans(): void {
        this.loading = true;
        this.subscriptionPlanService.getAllPlans().subscribe({
            next: (data) => {
                const list = Array.isArray(data) ? data : [];
                console.log('Plans loaded successfully:', list.length, 'plans');
                list.forEach(plan => {
                    console.log(`Plan ${plan.id} (${plan.planType}): imageUrl =`, plan.imageUrl);
                });
                this.plans = list;
                this.applyFilters();
                this.loading = false;
            },
            error: (err) => {
                console.error('Error loading plans:', err);
                // Ne pas afficher d'erreur pour une réponse 200 (succès avec corps vide ou mal formé)
                if (err.status === 200) {
                    this.plans = [];
                    this.applyFilters();
                } else {
                    const status = err.status ? `(HTTP ${err.status})` : '';
                    this.snackBar.open(`✕ Error loading plans ${status}`, 'Close', { duration: 5000 });
                }
                this.loading = false;
            }
        });
    }

    applyFilters(): void {
        let filtered = this.plans.filter((plan) => this.matchesSearchQuery(plan, this.searchQuery));

        filtered = [...filtered].sort((a, b) => {
            let aValue: string | number;
            let bValue: string | number;

            switch (this.sortBy) {
                case 'planType':
                    aValue = (a.planType || '').toLowerCase();
                    bValue = (b.planType || '').toLowerCase();
                    break;
                case 'date':
                    aValue = new Date(a.date || 0).getTime();
                    bValue = new Date(b.date || 0).getTime();
                    break;
                default:
                    return 0;
            }

            if (aValue < bValue) return this.sortOrder === 'asc' ? -1 : 1;
            if (aValue > bValue) return this.sortOrder === 'asc' ? 1 : -1;
            return 0;
        });

        this.filteredTotalCount = filtered.length;
        this.totalPages = Math.max(1, Math.ceil(filtered.length / this.itemsPerPage));
        if (this.currentPage > this.totalPages) {
            this.currentPage = this.totalPages;
        }
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

    deletePlan(id: number): void {
        if (confirm('Are you sure you want to delete this plan?')) {
            this.subscriptionPlanService.deletePlan(id).subscribe({
                next: () => {
                    this.snackBar.open('✓ Plan deleted successfully', 'Close', {
                        duration: 3000,
                        panelClass: ['success-snackbar']
                    });
                    this.allPlans();
                },
                error: (err) => {
                    console.error('Delete error:', err);
                    const errorMsg = err.status ? `(HTTP ${err.status}: ${err.statusText || 'Error'})` : err.message || 'Unknown error';
                    this.snackBar.open(`✕ Error deleting plan: ${errorMsg}`, 'Close', { duration: 7000 });
                }
            });
        }
    }

    openCreateDialog(): void {
        const dialogRef = this.dialog.open(SubscriptionDialogComponent, {
            width: '450px',
            data: {}
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.loading = true;
                this.subscriptionPlanService.createPlan(result).subscribe({
                    next: (createdPlan) => {
                        this.snackBar.open('✓ Plan created successfully', 'Close', {
                            duration: 3000,
                            panelClass: ['success-snackbar']
                        });
                        this.searchQuery = ''; // Clear search to see new item
                        this.currentPage = 1;
                        this.allPlans();
                    },
                    error: (err) => {
                        this.loading = false;
                        console.error('Create error:', err);
                        const errorMsg = err.status ? `(HTTP ${err.status}: ${err.statusText || 'Error'})` : err.message || 'Unknown error';
                        this.snackBar.open(`✕ Error creating plan: ${errorMsg}`, 'Close', { duration: 7000 });
                    }
                });
            }
        });
    }

    openEditDialog(plan: SubscriptionPlan): void {
        const dialogRef = this.dialog.open(SubscriptionDialogComponent, {
            width: '450px',
            data: { plan: { ...plan } }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.loading = true;
                this.subscriptionPlanService.updatePlan(plan.id!, result).subscribe({
                    next: (updatedPlan) => {
                        this.snackBar.open('✓ Plan updated successfully', 'Close', {
                            duration: 3000,
                            panelClass: ['success-snackbar']
                        });
                        this.allPlans();
                    },
                    error: (err) => {
                        this.loading = false;
                        console.error('Update error:', err);
                        const errorMsg = err.status ? `(HTTP ${err.status}: ${err.statusText || 'Error'})` : err.message || 'Unknown error';
                        this.snackBar.open(`✕ Error updating plan: ${errorMsg}`, 'Close', { duration: 7000 });
                    }
                });
            }
        });
    }
}
