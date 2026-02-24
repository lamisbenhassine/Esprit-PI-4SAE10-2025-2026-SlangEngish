import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';
import { Evaluation } from '../../core/models';
import { EvaluationApiService } from '../../core/services/evaluation-api.service';
import { CurrentUserService } from '../../core/services/current-user.service';
import { getDisplayUploadUrl } from '../../core/utils/upload-url.util';
import { MatSnackBar } from '@angular/material/snack-bar';

const PLACEHOLDER_GRADIENT = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';

@Component({
  selector: 'app-evaluations-list',
  templateUrl: './evaluations-list.component.html',
  styleUrls: ['./evaluations-list.component.css']
})
export class EvaluationsListComponent implements OnInit {
  evaluations: Evaluation[] = [];
  loading = true;

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
}
