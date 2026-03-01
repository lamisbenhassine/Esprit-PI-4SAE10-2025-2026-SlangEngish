import { Component, OnInit } from '@angular/core';
import { ForumTopicService, ForumTopic } from '../../../core/services/forum-topic.service';
import { PaymentService } from '../../../core/services/payment.service';

@Component({
    selector: 'app-forum-levels',
    template: `
    <div class="container-fluid">
      <h1>Level-based Forums</h1>
      
      <div *ngIf="!isPaid" class="alert alert-warning">
        <strong>Heads up!</strong> You need an active subscription to access level-specific forums.
        <a routerLink="/frontoffice/inscription/offers" class="alert-link">View subscription plans</a>
      </div>
      
      <div class="btn-group mb-4" role="group">
        <button *ngFor="let level of levels" type="button" 
                class="btn" 
                [ngClass]="selectedLevel === level ? 'btn-primary' : 'btn-outline-primary'"
                (click)="selectLevel(level)"
                [disabled]="!isPaid">
          Level {{ level }}
        </button>
      </div>
      
      <div *ngIf="loading" class="spinner-border text-primary"></div>
      
      <div *ngIf="selectedLevel && !loading">
        <h3>Topics - Level {{ selectedLevel }}</h3>
        
        <div *ngIf="topics.length === 0" class="alert alert-info">
          No topics for level {{ selectedLevel }} yet.
        </div>
        
        <div class="list-group">
          <a *ngFor="let topic of topics" [routerLink]="['/frontoffice/forum/topic', topic.id]" 
             class="list-group-item list-group-item-action">
             <div class="d-flex w-100 justify-content-between">
               <h5 class="mb-1">{{ topic.title }}</h5>
               <small>👁️ {{ topic.views }}</small>
             </div>
             <p class="mb-1">{{ topic.description }}</p>
          </a>
        </div>
        
        <div *ngIf="error" class="alert alert-danger mt-3">{{ error }}</div>
      </div>
    </div>
  `
})
export class ForumLevelsComponent implements OnInit {
    levels = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];
    selectedLevel = '';
    topics: ForumTopic[] = [];
    loading = false;
    error = '';
    isPaid = false;
    currentUserId = 1;

    constructor(
        private forumTopicService: ForumTopicService,
        private paymentService: PaymentService
    ) { }

    ngOnInit() {
        this.checkPayment();
    }

    checkPayment() {
        this.paymentService.verifyUserPayment(this.currentUserId).subscribe({
            next: (paid) => this.isPaid = paid,
            error: () => this.isPaid = false
        });
    }

    selectLevel(level: string) {
        if (!this.isPaid) return;
        this.selectedLevel = level;
        this.loading = true;
        this.error = '';

        this.forumTopicService.getTopicsByLevel(level, this.currentUserId).subscribe({
            next: (data) => {
                this.topics = data;
                this.loading = false;
            },
            error: (err) => {
                this.error = 'Access denied or server error';
                this.loading = false;
            }
        });
    }
}
