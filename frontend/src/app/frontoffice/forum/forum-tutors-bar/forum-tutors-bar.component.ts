import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserProfile, UserProfileService } from '../../../core/services/user-profile.service';

@Component({
  selector: 'app-forum-tutors-bar',
  templateUrl: './forum-tutors-bar.component.html',
  styleUrls: ['./forum-tutors-bar.component.css']
})
export class ForumTutorsBarComponent implements OnInit {
  tutors: UserProfile[] = [];
  loading = false;

  constructor(
    private users: UserProfileService,
    private router: Router
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
