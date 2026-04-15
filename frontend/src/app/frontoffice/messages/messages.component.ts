import { Component, OnInit } from '@angular/core';
import {
  DirectMessageService,
  DirectConversationSummary,
  DirectMessage,
  DirectKind
} from '../../core/services/direct-message.service';
import { UserProfile, UserProfileService } from '../../core/services/user-profile.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorMessage, ComposeAssistService } from '../../core/services/compose-assist.service';
import { FrontofficeIdentityService } from '../../core/services/frontoffice-identity.service';
import { DmReadPointerService } from '../../core/services/dm-read-pointer.service';
import { MessagingUnreadService } from '../../core/services/messaging-unread.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-messages',
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.css']
})
export class MessagesComponent implements OnInit {
  currentUserId = 2;
  availableUsers: UserProfile[] = [];
  userById: { [id: number]: UserProfile } = {};

  inboxPeer: DirectConversationSummary[] = [];
  inboxTutor: DirectConversationSummary[] = [];
  tutors: UserProfile[] = [];
  tutorSearch = '';

  selected: DirectConversationSummary | null = null;
  thread: DirectMessage[] = [];
  draft = '';

  peerOtherId = '';
  loading = false;
  pendingTargetUserId: number | null = null;

  constructor(
    private dm: DirectMessageService,
    private users: UserProfileService,
    private identity: FrontofficeIdentityService,
    private readPtr: DmReadPointerService,
    private messagingUnread: MessagingUnreadService,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private composeAssist: ComposeAssistService
  ) {}

  polishDraft(): void {
    this.composeAssist.smartPolish$(this.draft).subscribe({
      next: ({ text, source }) => {
        this.draft = text;
        this.snackBar.open(
          source === 'ai' ? 'Texte amélioré (IA).' : 'Corrections locales (sans IA).',
          'OK',
          { duration: 2800 }
        );
      },
      error: err =>
        this.snackBar.open(apiErrorMessage(err, this.composeAssist.profanityHint), 'OK', { duration: 6000 })
    });
  }

  ngOnInit(): void {
    this.currentUserId = this.identity.getCurrentUserId();
    this.loadUsers();
    this.loadTutors();
    this.refreshInbox();
    this.route.queryParamMap.subscribe(params => {
      const raw = Number(params.get('withUserId'));
      this.pendingTargetUserId = Number.isFinite(raw) && raw > 0 ? raw : null;
      this.tryOpenPendingTarget();
    });
  }

  loadUsers(): void {
    this.users.getAll().subscribe({
      next: list => {
        const all = (Array.isArray(list) ? list : []).filter(u => u.id !== 1);
        this.userById = {};
        all.forEach(u => {
          if (u.id != null) {
            this.userById[u.id] = u;
          }
        });
        /** Front-office : seuls les étudiants écrivent ici ; les tuteurs répondent dans le back-office. */
        this.availableUsers = all.filter(
          u => (u.accountRole || 'STUDENT').toUpperCase() === 'STUDENT'
        );
        const validIds = new Set(this.availableUsers.map(u => u.id).filter((id): id is number => id != null));
        const me = this.userById[this.currentUserId];
        const isTutorProfile =
          me != null && (me.accountRole || 'STUDENT').toUpperCase() === 'TUTOR';
        if (!validIds.has(this.currentUserId) || isTutorProfile) {
          const fallback = this.availableUsers.find(u => u.id != null)?.id ?? 2;
          this.currentUserId = fallback;
          this.identity.setCurrentUserId(fallback);
          this.refreshInbox();
        }
        if (this.currentUserId === 1) {
          const suggested = this.availableUsers.find(u => u.id != null)?.id;
          if (suggested) {
            this.currentUserId = suggested;
            this.identity.setCurrentUserId(suggested);
            this.refreshInbox();
          }
        }
        this.tryOpenPendingTarget();
      },
      error: () => {
        this.availableUsers = [];
        this.userById = {};
      }
    });
  }

  loadTutors(): void {
    this.users.getByRole('TUTOR').subscribe({
      next: list => (this.tutors = (Array.isArray(list) ? list : []).filter(t => t.id !== 1)),
      error: () => (this.tutors = [])
    });
  }

  refreshInbox(): void {
    this.dm.inbox(this.currentUserId).subscribe({
      next: rows => {
        const all = Array.isArray(rows) ? rows : [];
        this.inboxPeer = all.filter(r => r.kind === 'PEER');
        this.inboxTutor = all.filter(r => r.kind === 'WITH_TUTOR');
        this.tryOpenPendingTarget();
      },
      error: () => this.snackBar.open('Unable to load conversations.', 'OK', { duration: 4000 })
    });
  }

  setCurrentUserId(id: number): void {
    const next = Number(id);
    if (!next || Number.isNaN(next)) {
      return;
    }
    this.currentUserId = next;
    this.identity.setCurrentUserId(next);
    this.selected = null;
    this.thread = [];
    this.peerOtherId = '';
    this.refreshInbox();
  }

  openSummary(row: DirectConversationSummary): void {
    this.selected = row;
    this.loadThread(row.id);
  }

  loadThread(conversationId: number): void {
    this.dm.getMessages(conversationId, this.currentUserId).subscribe({
      next: msgs => {
        this.thread = Array.isArray(msgs) ? msgs : [];
        const maxId = this.thread.reduce((acc, m) => Math.max(acc, m.id), 0);
        this.readPtr.markConversationRead(this.currentUserId, conversationId, maxId);
        this.messagingUnread.refreshStudentViewer(this.currentUserId);
      },
      error: () => this.snackBar.open('Unable to load this conversation.', 'OK', { duration: 4000 })
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
        this.snackBar.open('Conversation opened.', 'OK', { duration: 2000 });
      },
      error: () => {
        this.loading = false;
        this.snackBar.open(
          'Cannot open this chat right now.',
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
        this.snackBar.open(
          'Impossible d’ouvrir la conversation. Démarrez le microservice forum (port 8040) et user (8010), puis redémarrez le forum après config Feign.',
          'OK',
          { duration: 7000 }
        );
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
        this.snackBar.open('Message envoyé', undefined, { duration: 2000 });
      },
      error: err =>
        this.snackBar.open(apiErrorMessage(err, 'Envoi impossible.'), 'OK', { duration: 6000 })
    });
  }

  isOwn(m: DirectMessage): boolean {
    return m.senderId === this.currentUserId;
  }

  kindLabel(kind: DirectKind): string {
    return kind === 'WITH_TUTOR' ? 'Tutor' : 'Peer';
  }

  userName(userId: number): string {
    const u = this.userById[userId];
    if (u?.firstName || u?.lastName) {
      return [u.firstName, u.lastName].filter(Boolean).join(' ');
    }
    return 'Member';
  }

  userRole(userId: number): 'Student' | 'Tutor' | 'Team' {
    const role = (this.userById[userId]?.accountRole || 'STUDENT').toUpperCase();
    if (role === 'TUTOR') {
      return 'Tutor';
    }
    if (role === 'ADMIN') {
      return 'Team';
    }
    return 'Student';
  }

  /** Présence type réseau pro (déterministe : id pair = en ligne). */
  tutorOnline(t: UserProfile): boolean {
    return t.id != null && t.id % 2 === 0;
  }

  filteredTutors(): UserProfile[] {
    const q = this.tutorSearch.trim().toLowerCase();
    if (!q) {
      return this.tutors;
    }
    return this.tutors.filter(t =>
      `${t.firstName || ''} ${t.lastName || ''}`.toLowerCase().includes(q)
    );
  }

  openTutorConversation(tutor: UserProfile): void {
    this.openWithTutor(tutor);
  }

  private tryOpenPendingTarget(): void {
    if (!this.pendingTargetUserId) {
      return;
    }
    const targetId = this.pendingTargetUserId;
    const existing = this.inboxTutor.find(r => r.otherUserId === targetId);
    if (existing) {
      this.openSummary(existing);
      this.pendingTargetUserId = null;
      return;
    }
    const tutor = this.tutors.find(t => t.id === targetId);
    if (tutor) {
      this.openWithTutor(tutor);
      this.pendingTargetUserId = null;
      return;
    }
    const user = this.userById[targetId];
    if (user) {
      if ((user.accountRole || '').toUpperCase() === 'TUTOR') {
        this.openWithTutor(user);
      } else {
        this.dm.open(this.currentUserId, targetId, 'PEER').subscribe({
          next: c => {
            this.refreshInbox();
            this.openSummary({ id: c.id, otherUserId: targetId, kind: c.kind });
          },
          error: () => this.snackBar.open('Unable to open this chat.', 'OK', { duration: 3500 })
        });
      }
      this.pendingTargetUserId = null;
    }
  }
}
