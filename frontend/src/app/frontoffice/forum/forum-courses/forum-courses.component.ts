import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ForumTopic, ForumTopicService } from '../../../core/services/forum-topic.service';
import { PaymentService } from '../../../core/services/payment.service';

@Component({
  selector: 'app-forum-courses',
  template: `
    <div class="container-fluid">
      <h1>Course Forum</h1>

      <div *ngIf="!isPaid" class="alert alert-warning">
        <strong>Heads up!</strong> You need an active subscription to access course forums.
        <a routerLink="/frontoffice/inscription/offers" class="alert-link">View subscription plans</a>
      </div>

      <div class="mb-3">
        <mat-form-field appearance="outline" class="w-100">
          <mat-label>Course key</mat-label>
          <input matInput [(ngModel)]="courseKey" placeholder="EX: GRAMMAR_1, TOEIC, SPEAKING">
        </mat-form-field>
        <button mat-raised-button color="primary" (click)="loadCourse()" [disabled]="!isPaid || !courseKey.trim() || loading">
          Load
        </button>
      </div>

      <div *ngIf="loading" class="spinner-border text-primary"></div>

      <div *ngIf="error" class="alert alert-danger mt-3">{{ error }}</div>

      <div *ngIf="!loading && topics.length > 0" class="list-group mt-3">
        <a *ngFor="let topic of topics" [routerLink]="['/frontoffice/forum/topic', topic.id]"
           class="list-group-item list-group-item-action">
          <div class="d-flex w-100 justify-content-between align-items-start gap-2">
            <app-topic-post-translate-block
              class="flex-grow-1"
              variant="card"
              [title]="topic.title"
              [description]="topic.description || ''"
              [previewMaxLength]="220"
            ></app-topic-post-translate-block>
            <small class="text-nowrap">Views: {{ topic.views }}</small>
          </div>
        </a>
      </div>
    </div>
  `
})
export class ForumCoursesComponent {
  topics: ForumTopic[] = [];
  loading = false;
  error = '';

  isPaid = false;
  currentUserId = 1;

  courseKey = '';

  constructor(
    private route: ActivatedRoute,
    private forumTopicService: ForumTopicService,
    private paymentService: PaymentService
  ) {
    const fromRoute = this.route.snapshot.paramMap.get('courseKey');
    if (fromRoute) this.courseKey = fromRoute;
    this.checkPayment();
  }

  checkPayment() {
    this.paymentService.verifyUserPayment(this.currentUserId).subscribe({
      next: (paid) => {
        this.isPaid = paid;
        if (paid && this.courseKey.trim()) this.loadCourse();
      },
      error: () => this.isPaid = false
    });
  }

  loadCourse() {
    if (!this.isPaid || !this.courseKey.trim()) return;
    this.loading = true;
    this.error = '';
    const key = this.courseKey.trim();

    this.forumTopicService.getCourseSpace(key, this.currentUserId).subscribe({
      next: (space) => {
        this.forumTopicService.getTopicsBySpace(space.id!, this.currentUserId).subscribe({
          next: (topics) => {
            this.topics = topics;
            this.loading = false;
          },
          error: () => {
            this.error = 'Error loading course topics';
            this.loading = false;
          }
        });
      },
      error: () => {
        this.error = 'Access denied or server error';
        this.loading = false;
      }
    });
  }
}
