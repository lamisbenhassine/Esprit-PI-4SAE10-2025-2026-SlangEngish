import { Component, OnInit } from '@angular/core';
import { ForumTopicService, ForumTopic } from '../../../core/services/forum-topic.service';
import { FrontofficeIdentityService } from '../../../core/services/frontoffice-identity.service';
import { UserProfile, UserProfileService } from '../../../core/services/user-profile.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-forum-unanswered-list',
  templateUrl: './forum-unanswered-list.component.html',
  styleUrls: ['./forum-unanswered-list.component.css']
})
export class ForumUnansweredListComponent implements OnInit {
  topics: ForumTopic[] = [];
  loading = false;
  userById: { [id: number]: UserProfile } = {};
  currentUserId = 2;

  constructor(
    private forumTopics: ForumTopicService,
    private identity: FrontofficeIdentityService,
    private users: UserProfileService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.identity.getCurrentUserId();
    this.users.getAll().subscribe({
      next: list => {
        this.userById = {};
        (Array.isArray(list) ? list : []).forEach(u => {
          if (u.id != null) {
            this.userById[u.id] = u;
          }
        });
      },
      error: () => (this.userById = {})
    });
    this.refresh();
  }

  refresh(): void {
    this.loading = true;
    this.forumTopics.getUnansweredTopics(80, this.currentUserId).subscribe({
      next: rows => {
        this.topics = Array.isArray(rows) ? rows : [];
        this.loading = false;
      },
      error: () => {
        this.topics = [];
        this.loading = false;
        this.snackBar.open('Impossible de charger la liste (microservice forum).', 'OK', { duration: 4000 });
      }
    });
  }

  spaceLabel(t: ForumTopic): string {
    const s = t.space;
    if (s?.title?.trim()) {
      return s.title.trim();
    }
    if (s?.key?.trim()) {
      return s.key.trim();
    }
    return t.category || '—';
  }

  authorLabel(authorId: number | undefined): string {
    if (authorId == null) {
      return '?';
    }
    const u = this.userById[authorId];
    if (u?.firstName || u?.lastName) {
      return [u.firstName, u.lastName].filter(Boolean).join(' ');
    }
    return `#${authorId}`;
  }
}
