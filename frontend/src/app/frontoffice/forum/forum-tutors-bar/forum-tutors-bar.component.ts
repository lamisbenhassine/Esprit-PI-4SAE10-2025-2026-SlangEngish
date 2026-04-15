import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { UserProfile, UserProfileService } from '../../../core/services/user-profile.service';
import { ForumTopicService } from '../../../core/services/forum-topic.service';
import { FrontofficeIdentityService } from '../../../core/services/frontoffice-identity.service';

@Component({
  selector: 'app-forum-tutors-bar',
  templateUrl: './forum-tutors-bar.component.html',
  styleUrls: ['./forum-tutors-bar.component.css']
})
export class ForumTutorsBarComponent implements OnInit, OnDestroy {
  tutors: UserProfile[] = [];
  loading = false;

  /** File « sujet sans aucun message dans le fil » — visible tuteur / équipe. */
  showStaffForumQueue = false;
  unansweredCount = 0;
  loadingUnanswered = false;
  private identitySub?: Subscription;

  constructor(
    private users: UserProfileService,
    private router: Router,
    private forumTopics: ForumTopicService,
    private identity: FrontofficeIdentityService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.users.getByRole('TUTOR').subscribe({
      next: list => {
        this.tutors = (Array.isArray(list) ? list : []).filter(t => t.id != null && t.id !== 1);
        this.loading = false;
      },
      error: () => {
        this.tutors = [];
        this.loading = false;
      }
    });
    this.refreshStaffRoleAndQueue();
    this.identitySub = this.identity.userId$.subscribe(() => this.refreshStaffRoleAndQueue());
  }

  ngOnDestroy(): void {
    this.identitySub?.unsubscribe();
  }

  private refreshStaffRoleAndQueue(): void {
    const uid = this.identity.getCurrentUserId();
    this.users.getById(uid).subscribe({
      next: u => {
        const r = (u?.accountRole || 'STUDENT').toUpperCase();
        this.showStaffForumQueue = r === 'TUTOR' || r === 'ADMIN';
        if (this.showStaffForumQueue) {
          this.loadUnansweredCount(uid);
        } else {
          this.unansweredCount = 0;
        }
      },
      error: () => {
        this.showStaffForumQueue = false;
        this.unansweredCount = 0;
      }
    });
  }

  private loadUnansweredCount(viewerUserId: number): void {
    this.loadingUnanswered = true;
    this.forumTopics.getUnansweredTopics(100, viewerUserId).subscribe({
      next: rows => {
        this.unansweredCount = Array.isArray(rows) ? rows.length : 0;
        this.loadingUnanswered = false;
      },
      error: () => {
        this.unansweredCount = 0;
        this.loadingUnanswered = false;
      }
    });
  }

  /** Démo : id pair = « en ligne » (même règle que la messagerie). */
  isOnline(u: UserProfile): boolean {
    return u.id != null && u.id % 2 === 0;
  }

  initials(u: UserProfile): string {
    const a = (u.firstName || '').charAt(0);
    const b = (u.lastName || '').charAt(0);
    return (a + b).toUpperCase() || '?';
  }

  displayName(u: UserProfile): string {
    return [u.firstName, u.lastName].filter(Boolean).join(' ') || 'Tuteur';
  }

  openPrivateChat(t: UserProfile, event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    if (!t.id) {
      return;
    }
    this.router.navigate(['/frontoffice/messages'], {
      queryParams: { withUserId: t.id }
    });
  }
}
