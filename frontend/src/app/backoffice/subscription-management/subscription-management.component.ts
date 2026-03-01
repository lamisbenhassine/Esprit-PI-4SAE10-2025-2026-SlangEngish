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

    // Search and filter properties
    searchQuery: string = '';
    sortBy: 'planType' | 'date' = 'date';
    sortOrder: 'asc' | 'desc' = 'desc';

    // Pagination properties
    currentPage: number = 1;
    itemsPerPage: number = 6;
    totalPages: number = 1;

    Math = Math; // Expose Math to template

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

    allPlans(): void {
        this.loading = true;
        this.subscriptionPlanService.getAllPlans().subscribe({
            next: (data) => {
                console.log('Plans loaded successfully:', data);
                // Debug: log each plan's imageUrl
                data.forEach(plan => {
                    console.log(`Plan ${plan.id} (${plan.planType}): imageUrl =`, plan.imageUrl);
                });
                this.plans = data;
                this.applyFilters();
                this.loading = false;
            },
            error: (err) => {
                console.error('Error loading plans:', err);
                const status = err.status ? `(HTTP ${err.status})` : '';
                this.snackBar.open(`✕ Error loading plans ${status}`, 'Close', { duration: 5000 });
                this.loading = false;
            }
        });
    }

    applyFilters(): void {
        // Apply search filter
        let filtered = this.plans;
        if (this.searchQuery.trim()) {
            const query = this.searchQuery.toLowerCase();
            filtered = filtered.filter(plan =>
                plan.planType.toLowerCase().includes(query) ||
                (plan.date || '').toLowerCase().includes(query)
            );
        }

        // Apply sorting
        filtered = [...filtered].sort((a, b) => {
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
                default:
                    return 0;
            }

            if (aValue < bValue) return this.sortOrder === 'asc' ? -1 : 1;
            if (aValue > bValue) return this.sortOrder === 'asc' ? 1 : -1;
            return 0;
        });

        // Calculate pagination
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
