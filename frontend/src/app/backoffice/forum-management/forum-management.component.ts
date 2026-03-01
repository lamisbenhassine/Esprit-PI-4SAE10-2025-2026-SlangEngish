import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { ForumTopicService, ForumTopic } from '../../core/services/forum-topic.service';
import { ForumDialogComponent } from './forum-dialog/forum-dialog.component';

@Component({
    selector: 'app-forum-management',
    templateUrl: './forum-management.component.html',
    styleUrls: ['./forum-management.component.css']
})
export class ForumManagementComponent implements OnInit {
    Math = Math; // Expose Math to template
    topics: ForumTopic[] = [];
    filteredTopics: ForumTopic[] = [];
    loading = false;

    // Search and filter properties
    searchQuery: string = '';
    sortBy: 'title' | 'category' | 'views' | 'createdAt' = 'createdAt';
    sortOrder: 'asc' | 'desc' = 'desc';

    // Pagination properties
    currentPage: number = 1;
    itemsPerPage: number = 6;
    totalPages: number = 1;

    constructor(
        private forumTopicService: ForumTopicService,
        private snackBar: MatSnackBar,
        private dialog: MatDialog
    ) { }

    ngOnInit(): void {
        this.refreshTopics();
    }

    // Same helper logic as subscriptions for visual consistency
    getTopicImage(topic: ForumTopic): string {
        const colors = ['43e97b', '38f9d7', 'fa709a', 'fee140'];
        const index = (topic.id || 0) % colors.length;
        return `https://via.placeholder.com/300x200/${colors[index]}/333333?text=${topic.category}`;
    }

    refreshTopics(): void {
        this.loading = true;
        this.forumTopicService.getGeneralTopics().subscribe({
            next: (data) => {
                console.log('Topics loaded successfully:', data);
                this.topics = data;
                this.applyFilters();
                this.loading = false;
            },
            error: (err) => {
                console.error('Error loading topics:', err);
                const status = err.status ? `(HTTP ${err.status})` : '';
                this.snackBar.open(`✕ Error loading topics ${status}`, 'Close', { duration: 5000 });
                this.loading = false;
            }
        });
    }

    applyFilters(): void {
        // Apply search filter
        let filtered = this.topics;
        if (this.searchQuery.trim()) {
            const query = this.searchQuery.toLowerCase();
            filtered = filtered.filter(topic =>
                topic.title.toLowerCase().includes(query) ||
                topic.description.toLowerCase().includes(query) ||
                topic.category.toLowerCase().includes(query)
            );
        }

        // Apply sorting
        filtered = [...filtered].sort((a, b) => {
            let aValue: any, bValue: any;

            switch (this.sortBy) {
                case 'title':
                    aValue = a.title.toLowerCase();
                    bValue = b.title.toLowerCase();
                    break;
                case 'category':
                    aValue = a.category.toLowerCase();
                    bValue = b.category.toLowerCase();
                    break;
                case 'views':
                    aValue = a.views;
                    bValue = b.views;
                    break;
                case 'createdAt':
                    aValue = new Date(a.createdAt || '').getTime();
                    bValue = new Date(b.createdAt || '').getTime();
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
        this.filteredTopics = filtered.slice(startIndex, endIndex);
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

    openCreateDialog(): void {
        const dialogRef = this.dialog.open(ForumDialogComponent, {
            width: '500px',
            data: {}
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.loading = true;
                const newTopic = { ...result, views: 0 };
                this.forumTopicService.createTopic(newTopic).subscribe({
                    next: () => {
                        this.snackBar.open('✓ Topic created successfully', 'Close', {
                            duration: 3000,
                            panelClass: ['success-snackbar']
                        });
                        this.searchQuery = '';
                        this.currentPage = 1;
                        this.refreshTopics();
                    },
                    error: (err) => {
                        this.loading = false;
                        console.error('Error creating topic:', err);
                        const errorMsg = err.status ? `(HTTP ${err.status}: ${err.statusText || 'Error'})` : err.message || 'Unknown error';
                        this.snackBar.open(`✕ Error creating topic: ${errorMsg}`, 'Close', { duration: 7000 });
                    }
                });
            }
        });
    }

    editTopic(topic: ForumTopic): void {
        const dialogRef = this.dialog.open(ForumDialogComponent, {
            width: '500px',
            data: { topic: { ...topic } }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.loading = true;
                this.forumTopicService.updateTopic(topic.id!, result).subscribe({
                    next: () => {
                        this.snackBar.open('✓ Topic updated successfully', 'Close', {
                            duration: 3000,
                            panelClass: ['success-snackbar']
                        });
                        this.refreshTopics();
                    },
                    error: (err) => {
                        this.loading = false;
                        console.error('Update error:', err);
                        const errorMsg = err.status ? `(HTTP ${err.status}: ${err.statusText || 'Error'})` : err.message || 'Unknown error';
                        this.snackBar.open(`✕ Error updating topic: ${errorMsg}`, 'Close', { duration: 7000 });
                    }
                });
            }
        });
    }

    deleteTopic(id: number): void {
        if (confirm('Are you sure you want to delete this forum topic?')) {
            this.forumTopicService.deleteTopic(id).subscribe({
                next: () => {
                    this.snackBar.open('✓ Topic deleted successfully', 'Close', {
                        duration: 3000,
                        panelClass: ['success-snackbar']
                    });
                    this.refreshTopics();
                },
                error: (err) => {
                    console.error('Delete error:', err);
                    const errorMsg = err.status ? `(HTTP ${err.status}: ${err.statusText || 'Error'})` : err.message || 'Unknown error';
                    this.snackBar.open(`✕ Error deleting topic: ${errorMsg}`, 'Close', { duration: 7000 });
                }
            });
        }
    }
}
