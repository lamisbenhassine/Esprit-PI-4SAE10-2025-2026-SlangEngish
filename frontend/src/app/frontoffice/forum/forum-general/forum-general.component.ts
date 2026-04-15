import { Component, OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { ForumTopicService, ForumTopic } from '../../../core/services/forum-topic.service';
import { ForumMediaService } from '../../../core/services/forum-media.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserProfile, UserProfileService } from '../../../core/services/user-profile.service';
import { FrontofficeIdentityService } from '../../../core/services/frontoffice-identity.service';
import { apiErrorMessage, ComposeAssistService } from '../../../core/services/compose-assist.service';

@Component({
  selector: 'app-forum-general',
  templateUrl: './forum-general.component.html',
  styleUrls: ['./forum-general.component.css']
})
export class ForumGeneralComponent implements OnInit {
  topics: ForumTopic[] = [];
  filteredTopics: ForumTopic[] = [];
  loading = false;

  creating = false;
  newTitle = '';
  newDescription = '';
  newCoverUrl = '';
  coverUploading = false;
  currentUserId = 2;
  users: UserProfile[] = [];
  userById: { [id: number]: UserProfile } = {};
  generalSpaceId: number | null = null;
  
  // Search and filter properties
  searchQuery: string = '';
  sortBy: 'title' | 'category' | 'views' | 'createdAt' = 'createdAt';
  sortOrder: 'asc' | 'desc' = 'desc';
  
  // Pagination properties
  currentPage: number = 1;
  itemsPerPage: number = 9;
  totalPages: number = 1;
  
  Math = Math; // Expose Math to template

  constructor(
    private forumTopicService: ForumTopicService,
    private forumMediaService: ForumMediaService,
    private usersService: UserProfileService,
    private identity: FrontofficeIdentityService,
    private snackBar: MatSnackBar,
    private composeAssist: ComposeAssistService
  ) {}

  polishNewTopicFields(): void {
    const title$ = this.newTitle.trim()
      ? this.composeAssist.smartPolish$(this.newTitle)
      : of({ text: this.newTitle, source: 'local' as const });
    const desc$ = this.newDescription.trim()
      ? this.composeAssist.smartPolish$(this.newDescription)
      : of({ text: this.newDescription, source: 'local' as const });
    forkJoin({ t: title$, d: desc$ }).subscribe({
      next: ({ t, d }) => {
        this.newTitle = t.text;
        this.newDescription = d.text;
        const ai = t.source === 'ai' || d.source === 'ai';
        this.snackBar.open(
          ai ? 'Texte amélioré (IA).' : 'Corrections locales (sans IA).',
          'OK',
          { duration: 2800 }
        );
      },
      error: err =>
        this.snackBar.open(apiErrorMessage(err, this.composeAssist.profanityHint), 'OK', { duration: 6000 })
    });
  }

  ngOnInit() {
    this.currentUserId = this.identity.getCurrentUserId();
    this.usersService.getAll().subscribe({
      next: list => {
        this.users = (Array.isArray(list) ? list : []).filter(u => u.id !== 1);
        this.userById = {};
        this.users.forEach(u => {
          if (u.id != null) {
            this.userById[u.id] = u;
          }
        });
        if (this.currentUserId === 1) {
          const suggested = this.users.find(u => u.id != null && u.id !== 1)?.id;
          if (suggested) {
            this.currentUserId = suggested;
            this.identity.setCurrentUserId(suggested);
            this.loadTopics();
          }
        }
      },
      error: () => {
        this.users = [];
        this.userById = {};
      }
    });
    this.loadTopics();
    this.resolveGeneralSpace();
  }

  private resolveGeneralSpace(): void {
    this.forumTopicService.getGeneralSpace().subscribe({
      next: (space) => this.generalSpaceId = space.id ?? null,
      error: () => this.generalSpaceId = null
    });
  }

  private loadTopics(): void {
    this.loading = true;
    this.forumTopicService.getGeneralTopics(this.currentUserId).subscribe({
      next: (data) => {
        this.topics = data;
        this.applyFilters();
        this.loading = false;
      },
      error: () => this.loading = false
    });

  }

  setCurrentUserId(userId: number): void {
    const next = Number(userId);
    if (!next || Number.isNaN(next)) {
      return;
    }
    this.currentUserId = next;
    this.identity.setCurrentUserId(next);
    this.loadTopics();
  }

  authorName(authorId: number | undefined): string {
    if (authorId == null) {
      return 'Unknown author';
    }
    const u = this.userById[authorId];
    if (u?.firstName || u?.lastName) {
      return [u.firstName, u.lastName].filter(Boolean).join(' ');
    }
    return 'Member';
  }

  authorRole(authorId: number | undefined): string {
    if (authorId == null) {
      return 'Student';
    }
    const r = (this.userById[authorId]?.accountRole || 'STUDENT').toUpperCase();
    if (r === 'TUTOR') {
      return 'Tutor';
    }
    if (r === 'ADMIN') {
      return 'Team';
    }
    return 'Student';
  }

  deleteOwnTopic(topic: ForumTopic, ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    const id = topic.id;
    if (id == null || topic.authorId !== this.currentUserId) {
      return;
    }
    if (!confirm('Supprimer ce sujet ? Cette action est définitive.')) {
      return;
    }
    this.forumTopicService.deleteTopic(id, this.currentUserId).subscribe({
      next: () => {
        this.snackBar.open('Sujet supprimé.', 'OK', { duration: 2500 });
        this.loadTopics();
      },
      error: err => {
        const msg =
          err?.error?.error ||
          (typeof err?.error === 'string' ? err.error : null) ||
          'Suppression impossible.';
        this.snackBar.open(msg, 'OK', { duration: 5000 });
      }
    });
  }

  createTopic(): void {
    if (this.creating) return;
    if (!this.newTitle.trim() || !this.newDescription.trim()) return;
    if (this.generalSpaceId == null) return;

    this.creating = true;
    this.forumTopicService.createTopicInSpace(this.generalSpaceId, {
      userId: this.currentUserId,
      authorId: this.currentUserId,
      title: this.newTitle.trim(),
      description: this.newDescription.trim(),
      coverImageUrl: this.newCoverUrl.trim() || undefined
    }).subscribe({
      next: () => {
        this.newTitle = '';
        this.newDescription = '';
        this.newCoverUrl = '';
        this.creating = false;
        this.loadTopics();
      },
      error: err => {
        this.creating = false;
        this.snackBar.open(apiErrorMessage(err, 'Publication impossible.'), 'OK', { duration: 6000 });
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

    // Apply sorting (épinglés d’abord si tri par date)
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

  onCoverFile(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.coverUploading = true;
    this.forumMediaService.upload(file).subscribe({
      next: res => {
        this.newCoverUrl = res.url;
        this.coverUploading = false;
        input.value = '';
      },
      error: err => {
        this.coverUploading = false;
        input.value = '';
        this.snackBar.open(this.forumMediaService.describeUploadError(err), 'OK', { duration: 7000 });
      }
    });
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
}
