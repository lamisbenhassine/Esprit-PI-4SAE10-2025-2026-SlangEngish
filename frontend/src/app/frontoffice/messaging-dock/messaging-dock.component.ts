import { Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import {
  DirectMessageService,
  DirectConversation,
  DirectMessage,
  DirectKind
} from '../../core/services/direct-message.service';
import { UserProfile, UserProfileService } from '../../core/services/user-profile.service';
import { FrontofficeIdentityService } from '../../core/services/frontoffice-identity.service';
import { DmReadPointerService } from '../../core/services/dm-read-pointer.service';
import { MessagingUnreadService } from '../../core/services/messaging-unread.service';
import { MatSnackBar } from '@angular/material/snack-bar';

type DockView = 'list' | 'thread';

@Component({
  selector: 'app-messaging-dock',
  templateUrl: './messaging-dock.component.html',
  styleUrls: ['./messaging-dock.component.css']
})
export class MessagingDockComponent implements OnInit, OnDestroy {
  expanded = false;
  view: DockView = 'list';
  search = '';

  currentUserId = 2;
  users: UserProfile[] = [];
  userById: { [id: number]: UserProfile } = {};

  /** Tutors (student view) or students (tutor view), filtered to “online” demo. */
  roster: UserProfile[] = [];

  selectedPeer: UserProfile | null = null;
  conversationId: number | null = null;
  conversationKind: DirectKind | null = null;
  thread: DirectMessage[] = [];
  draft = '';
  loading = false;

  private pollHandle: ReturnType<typeof setInterval> | null = null;
  private identitySub?: Subscription;
  private unreadSub?: Subscription;
  dockUnread = 0;

  constructor(
    private dm: DirectMessageService,
    private usersApi: UserProfileService,
    private identity: FrontofficeIdentityService,
    private readPtr: DmReadPointerService,
    private messagingUnread: MessagingUnreadService,
    private router: Router,
    private snackBar: MatSnackBar,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.identity.getCurrentUserId();
    this.loadUsers();
    this.identitySub = this.identity.userId$.subscribe(id => {
      if (id && id !== this.currentUserId) {
        this.currentUserId = id;
        this.closeThread();
        this.loadUsers();
      }
    });
    this.unreadSub = this.messagingUnread.studentUnread$.subscribe(n => {
      this.dockUnread = n;
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.identitySub?.unsubscribe();
    this.unreadSub?.unsubscribe();
  }

  private loadUsers(): void {
    this.usersApi.getAll().subscribe({
      next: list => {
        const all = Array.isArray(list) ? list : [];
        this.users = all.filter(u => u.id != null && u.id !== 1);
        this.userById = {};
        this.users.forEach(u => {
          if (u.id != null) {
            this.userById[u.id] = u;
          }
        });
        const me = this.userById[this.currentUserId];
        if (me && (me.accountRole || 'STUDENT').toUpperCase() === 'TUTOR') {
          const st = this.users.find(
            u => (u.accountRole || 'STUDENT').toUpperCase() === 'STUDENT'
          );
          if (st?.id != null) {
            this.currentUserId = st.id;
            this.identity.setCurrentUserId(st.id);
            this.closeThread();
          }
        }
        this.rebuildRoster();
      },
      error: () => {
        this.users = [];
        this.userById = {};
        this.roster = [];
      }
    });
  }

  private rebuildRoster(): void {
    const me = this.userById[this.currentUserId];
    const myRole = (me?.accountRole || 'STUDENT').toUpperCase();
    const wantTutors = myRole === 'STUDENT';
    this.roster = this.users.filter(u => {
      if (u.id === this.currentUserId) {
        return false;
      }
      if (!this.isOnline(u)) {
        return false;
      }
      const r = (u.accountRole || 'STUDENT').toUpperCase();
      if (wantTutors) {
        return r === 'TUTOR';
      }
      return r === 'STUDENT';
    });
  }

  /** Demo presence: deterministic “online” (same rule as layout contacts). */
  isOnline(u: UserProfile): boolean {
    if (u.id == null) {
      return false;
    }
    return u.id % 2 === 0;
  }

  rosterTitle(): string {
    const me = this.userById[this.currentUserId];
    const myRole = (me?.accountRole || 'STUDENT').toUpperCase();
    if (myRole === 'STUDENT') {
      return 'Tuteurs connectés';
    }
    return 'Étudiants connectés';
  }

  currentUserLabel(): string {
    const u = this.userById[this.currentUserId];
    if (!u) {
      return 'Profil';
    }
    return `${u.firstName} ${u.lastName}`.trim();
  }

  currentRoleLabel(): string {
    const me = this.userById[this.currentUserId];
    const r = (me?.accountRole || 'STUDENT').toUpperCase();
    if (r === 'TUTOR') {
      return 'Tuteur';
    }
    if (r === 'ADMIN') {
      return 'Équipe';
    }
    return 'Étudiant';
  }

  filteredRoster(): UserProfile[] {
    const q = this.search.trim().toLowerCase();
    if (!q) {
      return this.roster;
    }
    return this.roster.filter(u =>
      `${u.firstName || ''} ${u.lastName || ''}`.toLowerCase().includes(q)
    );
  }

  toggleExpand(): void {
    this.expanded = !this.expanded;
    if (!this.expanded) {
      this.stopPolling();
    } else if (this.view === 'thread' && this.conversationId != null) {
      this.startPolling();
    }
  }

  /** Ouvre le panneau sans perdre la conversation en cours. */
  expandDock(): void {
    this.expanded = true;
    if (this.view === 'thread' && this.conversationId != null) {
      this.startPolling();
    }
  }

  /** Liste des contacts (icône composer). */
  expandAndShowList(): void {
    this.expanded = true;
    this.view = 'list';
    this.rebuildRoster();
  }

  openPeer(peer: UserProfile): void {
    if (!peer?.id || peer.id === this.currentUserId) {
      return;
    }
    const me = this.userById[this.currentUserId];
    const myRole = (me?.accountRole || 'STUDENT').toUpperCase();
    const otherRole = (peer.accountRole || 'STUDENT').toUpperCase();
    const selfTutor = myRole === 'TUTOR';
    const otherTutor = otherRole === 'TUTOR';
    const kind: DirectKind = selfTutor !== otherTutor ? 'WITH_TUTOR' : 'PEER';

    this.loading = true;
    this.dm.open(this.currentUserId, peer.id, kind).subscribe({
      next: (c: DirectConversation) => {
        this.loading = false;
        this.selectedPeer = peer;
        this.conversationId = c.id;
        this.conversationKind = c.kind;
        this.view = 'thread';
        this.loadThread();
        this.startPolling();
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Impossible d’ouvrir la conversation.', 'OK', { duration: 4000 });
      }
    });
  }

  loadThread(): void {
    if (this.conversationId == null) {
      return;
    }
    const cid = this.conversationId;
    this.dm.getMessages(cid, this.currentUserId).subscribe({
      next: msgs => {
        this.thread = Array.isArray(msgs) ? msgs : [];
        const maxId = this.thread.reduce((acc, m) => Math.max(acc, m.id), 0);
        this.readPtr.markConversationRead(this.currentUserId, cid, maxId);
        this.messagingUnread.refreshStudentViewer(this.currentUserId);
      },
      error: () => {}
    });
  }

  private startPolling(): void {
    this.stopPolling();
    if (!isPlatformBrowser(this.platformId) || this.conversationId == null) {
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
    if (!text || this.conversationId == null) {
      return;
    }
    this.dm.send(this.conversationId, this.currentUserId, text).subscribe({
      next: m => {
        this.thread = [...this.thread, m];
        this.draft = '';
        this.loadThread();
      },
      error: () =>
        this.snackBar.open('Envoi impossible.', 'OK', { duration: 3000 })
    });
  }

  isOwn(m: DirectMessage): boolean {
    return m.senderId === this.currentUserId;
  }

  displayName(userId: number): string {
    const u = this.userById[userId];
    if (u?.firstName || u?.lastName) {
      return [u.firstName, u.lastName].filter(Boolean).join(' ');
    }
    return 'Membre';
  }

  backToList(): void {
    this.closeThread();
    this.view = 'list';
  }

  private closeThread(): void {
    this.stopPolling();
    this.selectedPeer = null;
    this.conversationId = null;
    this.conversationKind = null;
    this.thread = [];
    this.draft = '';
  }

  initials(u: UserProfile | null): string {
    if (!u) {
      return '?';
    }
    const a = (u.firstName || '').charAt(0);
    const b = (u.lastName || '').charAt(0);
    return (a + b).toUpperCase() || '?';
  }

  openFullMessages(): void {
    const q: Record<string, string> = {};
    if (this.selectedPeer?.id) {
      q['withUserId'] = String(this.selectedPeer.id);
    }
    this.router.navigate(['/frontoffice/messages'], { queryParams: Object.keys(q).length ? q : undefined });
  }
}
