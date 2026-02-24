import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';
import { Evaluation } from '../../core/models';
import { EvaluationApiService } from '../../core/services/evaluation-api.service';
import { CurrentUserService } from '../../core/services/current-user.service';
import { getDisplayUploadUrl } from '../../core/utils/upload-url.util';
import { MatSnackBar } from '@angular/material/snack-bar';

const PLACEHOLDER_GRADIENT = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';

export type EvalFilter = 'all' | 'upcoming' | 'ongoing' | 'past';

@Component({
  selector: 'app-evaluations-list',
  templateUrl: './evaluations-list.component.html',
  styleUrls: ['./evaluations-list.component.css']
})
export class EvaluationsListComponent implements OnInit {
  evaluations: Evaluation[] = [];
  loading = true;
  searchText = '';
  filterStatus: EvalFilter = 'all';
  pageSize = 9;
  pageIndex = 0;

  constructor(
    private api: EvaluationApiService,
    private currentUser: CurrentUserService,
    private router: Router,
    private snackBar: MatSnackBar,
    private sanitizer: DomSanitizer
  ) {}

  /** Safe style for card background (uploaded photo or placeholder). */
  getEvaluationImageStyle(imageUrl: string | undefined): SafeStyle {
    const url = getDisplayUploadUrl(imageUrl);
    if (!url) {
      return this.sanitizer.bypassSecurityTrustStyle(PLACEHOLDER_GRADIENT);
    }
    return this.sanitizer.bypassSecurityTrustStyle(`url(${url})`);
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    const userId = this.currentUser.getUserId();
    this.api.getAvailableForUser(userId).subscribe({
      next: (list) => {
        this.evaluations = list;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load evaluations', 'Close', { duration: 3000 });
      }
    });
  }

  startEvaluation(e: Evaluation): void {
    if (!e.id) return;
    this.router.navigate(['/frontoffice/evaluations', e.id, 'take']);
  }

  formatDate(s: string | undefined): string {
    if (!s) return '-';
    return new Date(s).toLocaleDateString();
  }

  private get now(): Date {
    return new Date();
  }

  private matchesSearch(e: Evaluation): boolean {
    if (!this.searchText.trim()) return true;
    const q = this.searchText.trim().toLowerCase();
    return (e.title || '').toLowerCase().includes(q);
  }

  private matchesFilter(e: Evaluation): boolean {
    const start = e.dateStart ? new Date(e.dateStart).getTime() : 0;
    const end = e.dateEnd ? new Date(e.dateEnd).getTime() : 0;
    const now = this.now.getTime();
    switch (this.filterStatus) {
      case 'upcoming':
        return start > now;
      case 'ongoing':
        return start <= now && end >= now;
      case 'past':
        return end < now;
      default:
        return true;
    }
  }

  get filteredEvaluations(): Evaluation[] {
    return this.evaluations.filter(e => this.matchesSearch(e) && this.matchesFilter(e));
  }

  get paginatedEvaluations(): Evaluation[] {
    const list = this.filteredEvaluations;
    const start = this.pageIndex * this.pageSize;
    return list.slice(start, start + this.pageSize);
  }

  get totalFiltered(): number {
    return this.filteredEvaluations.length;
  }

  onPageChange(event: { pageIndex: number; pageSize: number }): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
  }
}
