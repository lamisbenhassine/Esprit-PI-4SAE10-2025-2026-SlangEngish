import { Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import {
  DirectMessageService,
  DirectConversationSummary,
  DirectMessage
} from '../../core/services/direct-message.service';
import { UserProfile, UserProfileService } from '../../core/services/user-profile.service';
import { BackofficeTutorIdentityService } from '../../core/services/backoffice-tutor-identity.service';
import { DmReadPointerService } from '../../core/services/dm-read-pointer.service';
import { MessagingUnreadService } from '../../core/services/messaging-unread.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorMessage, ComposeAssistService } from '../../core/services/compose-assist.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-tutor-inbox',
  templateUrl: './tutor-inbox.component.html',
  styleUrls: ['./tutor-inbox.component.css']
})
export class TutorInboxComponent implements OnInit, OnDestroy {
  tutorUserId = 12;
  tutorProfiles: UserProfile[] = [];
  userById: { [id: number]: UserProfile } = {};

  desktopNotifAvailable = false;
  desktopNotifPermission: NotificationPermission = 'default';

  inboxTutor: DirectConversationSummary[] = [];
  search = '';

  selected: DirectConversationSummary | null = null;
  thread: DirectMessage[] = [];
  draft = '';
  loading = false;

  private pollHandle: ReturnType<typeof setInterval> | null = null;
  private tutorSub?: Subscription;

  constructor(
    private dm: DirectMessageService,
    private users: UserProfileService,
    private tutorIdentity: BackofficeTutorIdentityService,
    private readPtr: DmReadPointerService,
    private messagingUnread: MessagingUnreadService,
    private snackBar: MatSnackBar,
    private composeAssist: ComposeAssistService,
    @Inject(PLATFORM_ID) private platformId: object
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
    if (isPlatformBrowser(this.platformId) && typeof Notification !== 'undefined') {
      this.desktopNotifAvailable = true;
      this.desktopNotifPermission = Notification.permission;
    }
    this.tutorUserId = this.tutorIdentity.getTutorId();
    this.loadAllUsersMap();
    this.loadTutorList();
    this.refreshInbox();
    this.tutorSub = this.tutorIdentity.tutorId$.subscribe(id => {
      if (id && id !== this.tutorUserId) {
        this.tutorUserId = id;
        this.selected = null;
        this.thread = [];
        this.refreshInbox();
      }
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.tutorSub?.unsubscribe();
  }

  private loadAllUsersMap(): void {
    this.users.getAll().subscribe({
      next: list => {
        const all = Array.isArray(list) ? list : [];
        this.userById = {};
        all.forEach(u => {
          if (u.id != null) {
            this.userById[u.id] = u;
          }
        });
      },
      error: () => (this.userById = {})
    });
  }

  loadTutorList(): void {
    this.users.getByRole('TUTOR').subscribe({
      next: list => {
        this.tutorProfiles = (Array.isArray(list) ? list : []).filter(t => t.id != null && t.id !== 1);
        const valid = new Set(this.tutorProfiles.map(t => t.id));
        if (!valid.has(this.tutorUserId)) {
          const first = this.tutorProfiles[0]?.id;
          if (first != null) {
            this.setTutorId(first);
          }
        }
      },
      error: () => (this.tutorProfiles = [])
    });
  }

  setTutorId(id: number): void {
    const n = Number(id);
    if (!n || Number.isNaN(n)) {
      return;
    }
    this.tutorUserId = n;
    this.tutorIdentity.setTutorId(n);
    this.selected = null;
    this.thread = [];
    this.refreshInbox();
  }

  refreshInbox(): void {
    this.dm.inbox(this.tutorUserId).subscribe({
      next: rows => {
        const all = Array.isArray(rows) ? rows : [];
        this.inboxTutor = all.filter(r => r.kind === 'WITH_TUTOR');
      },
      error: () =>
        this.snackBar.open('Impossible de charger les conversations (forum 8040).', 'OK', { duration: 4000 })
    });
  }

  filteredInbox(): DirectConversationSummary[] {
    const q = this.search.trim().toLowerCase();
    if (!q) {
      return this.inboxTutor;
    }
    return this.inboxTutor.filter(row => {
      const name = this.userName(row.otherUserId).toLowerCase();
      return name.includes(q);
    });
  }

  openSummary(row: DirectConversationSummary): void {
    this.selected = row;
    this.loadThread();
    this.startPolling();
  }

  loadThread(): void {
    if (this.selected == null) {
      return;
    }
    const convId = this.selected.id;
    this.dm.getMessages(convId, this.tutorUserId).subscribe({
      next: msgs => {
        this.thread = Array.isArray(msgs) ? msgs : [];
        const maxId = this.thread.reduce((acc, m) => Math.max(acc, m.id), 0);
        this.readPtr.markConversationRead(this.tutorUserId, convId, maxId);
        this.messagingUnread.refreshTutorViewer(this.tutorUserId);
      },
      error: () => this.snackBar.open('Impossible de charger les messages.', 'OK', { duration: 3000 })
    });
  }

  rowUnread(row: DirectConversationSummary): boolean {
    return this.messagingUnread.isUnreadForViewer(this.tutorUserId, row);
  }

  private startPolling(): void {
    this.stopPolling();
    if (!isPlatformBrowser(this.platformId) || this.selected == null) {
      return;
    }
    this.pollHandle = globalThis.setInterval(() => this.loadThread(), 4000);
  }

  private stopPolling(): void {
    if (this.pollHandle != null && isPlatformBrowser(this.platformId)) {
      globalThis.clearInterval(this.pollHandle);
    }
    this.pollHandle = null;
  }

  send(): void {
    const text = this.draft.trim();
    if (!this.selected || !text) {
      return;
    }
    this.dm.send(this.selected.id, this.tutorUserId, text).subscribe({
      next: m => {
        this.thread = [...this.thread, m];
        this.draft = '';
        this.refreshInbox();
      },
      error: err =>
        this.snackBar.open(apiErrorMessage(err, 'Envoi impossible.'), 'OK', { duration: 6000 })
    });
  }

  isOwn(m: DirectMessage): boolean {
    return m.senderId === this.tutorUserId;
  }

  userName(userId: number): string {
    const u = this.userById[userId];
    if (u?.firstName || u?.lastName) {
      return [u.firstName, u.lastName].filter(Boolean).join(' ');
    }
    return 'Apprenant';
  }

  studentBadge(userId: number): string {
    const r = (this.userById[userId]?.accountRole || 'STUDENT').toUpperCase();
    return r === 'TUTOR' ? 'Tuteur' : 'Étudiant';
  }

  requestDesktopNotifications(): void {
    if (!isPlatformBrowser(this.platformId) || typeof Notification === 'undefined') {
      return;
    }
    void Notification.requestPermission().then(p => {
      this.desktopNotifPermission = p;
      if (p === 'granted') {
        this.snackBar.open('Vous recevrez une alerte lorsque la page n’est pas au premier plan.', 'OK', {
          duration: 4000
        });
      }
    });
  }
}
