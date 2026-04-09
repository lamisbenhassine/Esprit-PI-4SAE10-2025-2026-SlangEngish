import { Component, OnInit } from '@angular/core';
import {
  DirectMessageService,
  DirectConversationSummary,
  DirectMessage,
  DirectKind
} from '../../core/services/direct-message.service';
import { UserProfile, UserProfileService } from '../../core/services/user-profile.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabChangeEvent } from '@angular/material/tabs';

@Component({
  selector: 'app-messages',
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.css']
})
export class MessagesComponent implements OnInit {
  currentUserId = 1;

  inboxPeer: DirectConversationSummary[] = [];
  inboxTutor: DirectConversationSummary[] = [];
  tutors: UserProfile[] = [];

  selected: DirectConversationSummary | null = null;
  thread: DirectMessage[] = [];
  draft = '';

  peerOtherId = '';
  loading = false;

  constructor(
    private dm: DirectMessageService,
    private users: UserProfileService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadTutors();
    this.refreshInbox();
  }

  loadTutors(): void {
    this.users.getByRole('TUTOR').subscribe({
      next: list => (this.tutors = Array.isArray(list) ? list : []),
      error: () => (this.tutors = [])
    });
  }

  refreshInbox(): void {
    this.dm.inbox(this.currentUserId).subscribe({
      next: rows => {
        const all = Array.isArray(rows) ? rows : [];
        this.inboxPeer = all.filter(r => r.kind === 'PEER');
        this.inboxTutor = all.filter(r => r.kind === 'WITH_TUTOR');
      },
      error: () => this.snackBar.open('Impossible de charger les conversations.', 'OK', { duration: 4000 })
    });
  }

  onTabChange(_ev: MatTabChangeEvent): void {
    this.selected = null;
    this.thread = [];
  }

  openSummary(row: DirectConversationSummary): void {
    this.selected = row;
    this.loadThread(row.id);
  }

  loadThread(conversationId: number): void {
    this.dm.getMessages(conversationId, this.currentUserId).subscribe({
      next: msgs => (this.thread = Array.isArray(msgs) ? msgs : []),
      error: () => this.snackBar.open('Accès ou chargement impossible.', 'OK', { duration: 4000 })
    });
  }

  openPeer(): void {
    const other = Number(this.peerOtherId);
    if (!other || other === this.currentUserId) {
      return;
    }
    this.loading = true;
    this.dm.open(this.currentUserId, other, 'PEER').subscribe({
      next: c => {
        this.loading = false;
        this.peerOtherId = '';
        this.refreshInbox();
        const sum: DirectConversationSummary = {
          id: c.id,
          otherUserId: other,
          kind: 'PEER'
        };
        this.openSummary(sum);
        this.snackBar.open('Conversation ouverte', 'OK', { duration: 2000 });
      },
      error: () => {
        this.loading = false;
        this.snackBar.open(
          'Impossible d’ouvrir (vérifiez l’ID ou utilisez l’onglet tuteur pour un formateur).',
          'OK',
          { duration: 5000 }
        );
      }
    });
  }

  openWithTutor(tutor: UserProfile): void {
    if (!tutor?.id) {
      return;
    }
    this.loading = true;
    this.dm.open(this.currentUserId, tutor.id, 'WITH_TUTOR').subscribe({
      next: c => {
        this.loading = false;
        this.refreshInbox();
        this.openSummary({
          id: c.id,
          otherUserId: tutor.id,
          kind: 'WITH_TUTOR'
        });
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Service utilisateur ou forum indisponible.', 'OK', { duration: 4000 });
      }
    });
  }

  send(): void {
    const text = this.draft.trim();
    if (!this.selected || !text) {
      return;
    }
    this.dm.send(this.selected.id, this.currentUserId, text).subscribe({
      next: m => {
        this.thread = [...this.thread, m];
        this.draft = '';
        this.refreshInbox();
      },
      error: () => this.snackBar.open('Envoi impossible.', 'OK', { duration: 3000 })
    });
  }

  isOwn(m: DirectMessage): boolean {
    return m.senderId === this.currentUserId;
  }

  kindLabel(kind: DirectKind): string {
    return kind === 'WITH_TUTOR' ? 'Tuteur' : 'Collègue';
  }
}
