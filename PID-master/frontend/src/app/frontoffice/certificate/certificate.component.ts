import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EvaluationApiService } from '../../core/services/evaluation-api.service';
import { CurrentUserService } from '../../core/services/current-user.service';
import { MatSnackBar } from '@angular/material/snack-bar';

const PLATFORM_NAME = 'SlangEnglish';
const REQUIRED_PASSED = 5;
const MIN_PERCENT = 50;

@Component({
  selector: 'app-certificate',
  templateUrl: './certificate.component.html',
  styleUrls: ['./certificate.component.css']
})
export class CertificateComponent implements OnInit {
  loading = true;
  eligible = false;
  passedCount = 0;
  studentName = '';
  certificateDate = '';

  constructor(
    private api: EvaluationApiService,
    private currentUser: CurrentUserService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    const userId = this.currentUser.getUserId();
    this.api.getCertificateEligibility(userId).subscribe({
      next: (res) => {
        this.eligible = res.eligible;
        this.passedCount = res.passedCount;
        this.certificateDate = new Date().toLocaleDateString('en-US', {
          weekday: 'long',
          year: 'numeric',
          month: 'long',
          day: 'numeric'
        });
        this.loadStudentName(userId);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load certificate status', 'Close', { duration: 3000 });
        this.router.navigate(['/frontoffice/evaluations']);
      }
    });
  }

  private loadStudentName(userId: number): void {
    this.api.getUserById(userId).subscribe({
      next: (user) => {
        this.studentName = [user.name, user.surname].filter(Boolean).join(' ') || 'Student';
      },
      error: () => {
        this.studentName = 'Student';
      }
    });
  }

  get platformName(): string {
    return PLATFORM_NAME;
  }

  get congratulationMessage(): string {
    if (this.passedCount >= REQUIRED_PASSED) {
      return `Congratulations! You have successfully passed ${this.passedCount} evaluation${this.passedCount === 1 ? '' : 's'} with ${MIN_PERCENT}% or above.`;
    }
    return `Keep going! You have passed ${this.passedCount} out of ${REQUIRED_PASSED} evaluations with ${MIN_PERCENT}% or above.`;
  }

  get requiredPassed(): number {
    return REQUIRED_PASSED;
  }

  backToEvaluations(): void {
    this.router.navigate(['/frontoffice/evaluations']);
  }
}
