import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ForumTopicService, ForumTopic, ForumSpace } from '../../core/services/forum-topic.service';
import { ForumReportService, ForumReport, ForumReportStatus } from '../../core/services/forum-report.service';
import { ForumDialogComponent } from './forum-dialog/forum-dialog.component';
import { UserProfile, UserProfileService } from '../../core/services/user-profile.service';

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

    reports: ForumReport[] = [];
    reportsLoading = false;
    userById: { [id: number]: UserProfile } = {};

    /** Sujets sans aucun message (file d’attente tuteurs / équipe). */
    unansweredTopics: ForumTopic[] = [];
    unansweredLoading = false;

    /** Sujets forum général vs tous les espaces niveau (A1–C2). */
    topicSource: 'general' | 'levels' = 'general';
    readonly levelForumCodes = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];
    /** Utilisateur utilisé pour les appels API niveau (accès back-office). */
    readonly adminForumViewerUserId = 1;

    constructor(
        private forumTopicService: ForumTopicService,
        private forumReportService: ForumReportService,
        private usersService: UserProfileService,
        private snackBar: MatSnackBar,
        private dialog: MatDialog
    ) { }

    ngOnInit(): void {
        this.loadUsers();
        this.refreshTopics();
        this.loadReports();
        this.loadUnanswered();
    }

    loadUsers(): void {
        this.usersService.getAll().subscribe({
            next: list => {
                this.userById = {};
                (Array.isArray(list) ? list : []).forEach(u => {
                    if (u.id != null) {
                        this.userById[u.id] = u;
                    }
                });
            },
            error: () => {
                this.userById = {};
            }
        });
    }

    authorName(authorId: number | undefined): string {
        if (authorId == null) {
            return 'Auteur inconnu';
        }
        const u = this.userById[authorId];
        if (u?.firstName || u?.lastName) {
            return [u.firstName, u.lastName].filter(Boolean).join(' ');
        }
        return 'Auteur';
    }

    authorRole(authorId: number | undefined): string {
        if (authorId == null) {
            return 'Étudiant';
        }
        const r = (this.userById[authorId]?.accountRole || 'STUDENT').toUpperCase();
        if (r === 'TUTOR') {
            return 'Tuteur';
        }
        if (r === 'ADMIN') {
            return 'Équipe';
        }
        return 'Étudiant';
    }

    getTopicImage(topic: ForumTopic): string {
        if (topic.coverImageUrl) {
            return topic.coverImageUrl;
        }
        const colors = ['43e97b', '38f9d7', 'fa709a', 'fee140'];
        const index = (topic.id || 0) % colors.length;
        return `https://via.placeholder.com/300x200/${colors[index]}/333333?text=${topic.category}`;
    }

    loadUnanswered(): void {
        this.unansweredLoading = true;
        this.forumTopicService.getUnansweredTopics(60, this.adminForumViewerUserId).subscribe({
            next: rows => {
                this.unansweredTopics = Array.isArray(rows) ? rows : [];
                this.unansweredLoading = false;
            },
            error: () => {
                this.unansweredTopics = [];
                this.unansweredLoading = false;
                this.snackBar.open('Impossible de charger les sujets sans réponse (forum).', 'OK', { duration: 4000 });
            }
        });
    }

    spaceLabel(topic: ForumTopic): string {
        const s = topic.space as ForumSpace | null | undefined;
        if (s?.title?.trim()) {
            return s.title.trim();
        }
        if (s?.key?.trim()) {
            return s.key.trim();
        }
        return (topic.category || '—').toString();
    }

    openFrontTopic(topic: ForumTopic): void {
        if (!topic.id) {
            return;
        }
        const url = `/frontoffice/forum/topic/${topic.id}`;
        window.open(url, '_blank', 'noopener,noreferrer');
    }

    loadReports(): void {
        this.reportsLoading = true;
        this.forumReportService.listAll().subscribe({
            next: r => {
                this.reports = Array.isArray(r) ? r : [];
                this.reportsLoading = false;
            },
            error: () => {
                this.reportsLoading = false;
            }
        });
    }

    setReportStatus(report: ForumReport, status: ForumReportStatus): void {
        if (!report.id) {
            return;
        }
        this.forumReportService.updateStatus(report.id, status).subscribe({
            next: () => {
                this.snackBar.open('Signalement mis à jour', 'OK', { duration: 2500 });
                this.loadReports();
            },
            error: () => this.snackBar.open('Erreur', 'OK', { duration: 3000 })
        });
    }

    togglePin(topic: ForumTopic): void {
        if (!topic.id) {
            return;
        }
        const next = !topic.pinned;
        this.forumTopicService.moderateTopic(topic.id, next, undefined).subscribe({
            next: () => this.refreshTopics(),
            error: () => this.snackBar.open('Erreur modération', 'OK', { duration: 3000 })
        });
    }

    toggleLock(topic: ForumTopic): void {
        if (!topic.id) {
            return;
        }
        const next = !topic.locked;
        this.forumTopicService.moderateTopic(topic.id, undefined, next).subscribe({
            next: () => this.refreshTopics(),
            error: () => this.snackBar.open('Erreur modération', 'OK', { duration: 3000 })
        });
    }

    onTopicSourceChange(): void {
        this.currentPage = 1;
        this.searchQuery = '';
        this.refreshTopics();
    }

    refreshTopics(): void {
        this.loading = true;
        if (this.topicSource === 'general') {
            this.forumTopicService.getGeneralTopics().subscribe({
                next: (data) => {
                    const list = Array.isArray(data) ? data : [];
                    this.topics = list;
                    this.applyFilters();
                    this.loading = false;
                },
                error: (err) => {
                    console.error('Error loading topics:', err);
                    if (err.status === 200) {
                        this.topics = [];
                        this.applyFilters();
                    } else {
                        const status = err.status ? `(HTTP ${err.status})` : '';
                        this.snackBar.open(`Error loading topics ${status}`, 'Close', { duration: 5000 });
                    }
                    this.loading = false;
                }
            });
            return;
        }

        forkJoin(
            this.levelForumCodes.map(code =>
                this.forumTopicService.getTopicsByLevel(code, this.adminForumViewerUserId).pipe(
                    catchError(() => of([] as ForumTopic[]))
                )
            )
        ).subscribe({
            next: (arrays) => {
                this.topics = arrays.flat();
                this.applyFilters();
                this.loading = false;
            },
            error: () => {
                this.snackBar.open('Erreur chargement forums niveau', 'Close', { duration: 5000 });
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
                (topic.title || '').toLowerCase().includes(query) ||
                (topic.description || '').toLowerCase().includes(query) ||
                (topic.category || '').toLowerCase().includes(query)
            );
        }

        filtered = [...filtered].sort((a, b) => {
            const pa = a.pinned ? 1 : 0;
            const pb = b.pinned ? 1 : 0;
            if (pb !== pa) {
                return pb - pa;
            }
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
                        this.snackBar.open('Topic created successfully', 'Close', {
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
                        this.snackBar.open(`Error creating topic: ${errorMsg}`, 'Close', { duration: 7000 });
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
                        this.snackBar.open('Topic updated successfully', 'Close', {
                            duration: 3000,
                            panelClass: ['success-snackbar']
                        });
                        this.refreshTopics();
                    },
                    error: (err) => {
                        this.loading = false;
                        console.error('Update error:', err);
                        const errorMsg = err.status ? `(HTTP ${err.status}: ${err.statusText || 'Error'})` : err.message || 'Unknown error';
                        this.snackBar.open(`Error updating topic: ${errorMsg}`, 'Close', { duration: 7000 });
                    }
                });
            }
        });
    }

    deleteTopic(id: number): void {
        if (confirm('Are you sure you want to delete this forum topic?')) {
                this.forumTopicService.deleteTopic(id).subscribe({
                next: () => {
                    this.snackBar.open('Topic deleted successfully', 'Close', {
                        duration: 3000,
                        panelClass: ['success-snackbar']
                    });
                    this.refreshTopics();
                    this.loadUnanswered();
                },
                error: (err) => {
                    console.error('Delete error:', err);
                    const errorMsg = err.status ? `(HTTP ${err.status}: ${err.statusText || 'Error'})` : err.message || 'Unknown error';
                    this.snackBar.open(`Error deleting topic: ${errorMsg}`, 'Close', { duration: 7000 });
                }
            });
        }
    }
}
