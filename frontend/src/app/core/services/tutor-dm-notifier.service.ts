import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { DirectMessageService } from './direct-message.service';
import { BackofficeTutorIdentityService } from './backoffice-tutor-identity.service';
import { MessagingUnreadService } from './messaging-unread.service';
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * Alerte les tuteurs (back-office) lorsque la boîte de réception change (nouveau message d’un étudiant).
 * Fonctionne par polling — pas de WebSocket côté forum dans cette version.
 */
@Injectable({ providedIn: 'root' })
export class TutorDmNotifierService {
  private tutorSub?: Subscription;
  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private inboxSnapshot = '';
  private primed = false;

  constructor(
    private dm: DirectMessageService,
    private tutorIdentity: BackofficeTutorIdentityService,
    private messagingUnread: MessagingUnreadService,
    private snackBar: MatSnackBar,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  /** À appeler depuis le layout back-office au montage. */
  startBackofficeWatch(): void {
    this.stop();
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.tutorSub = this.tutorIdentity.tutorId$.subscribe(() => {
      this.inboxSnapshot = '';
      this.primed = false;
    });
    this.pollOnce();
    this.pollTimer = globalThis.setInterval(() => this.pollOnce(), 12000);
  }

  stop(): void {
    this.tutorSub?.unsubscribe();
    this.tutorSub = undefined;
    if (this.pollTimer != null) {
      globalThis.clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
    this.primed = false;
    this.inboxSnapshot = '';
  }

  private pollOnce(): void {
    const tid = this.tutorIdentity.getTutorId();
    this.dm.inbox(tid).subscribe({
      next: rows => {
        const list = Array.isArray(rows) ? rows : [];
        const tutorRows = list.filter(r => r.kind === 'WITH_TUTOR');
        this.messagingUnread.recomputeTutorRows(tid, tutorRows);
        const snap = JSON.stringify(
          tutorRows.map(r => [r.id, r.lastMessagePreview ?? '', r.updatedAt ?? ''])
        );
        if (this.primed && snap !== this.inboxSnapshot && this.inboxSnapshot !== '') {
          this.notifyTutor();
        }
        this.inboxSnapshot = snap;
        this.primed = true;
      },
      error: () => {}
    });
  }

  private notifyTutor(): void {
    const ref = this.snackBar.open(
      'Nouveau message d’un apprenant',
      'Ouvrir',
      { duration: 8000, horizontalPosition: 'end', verticalPosition: 'top' }
    );
    ref.onAction().subscribe(() => {
      void this.router.navigate(['/backoffice/tutor-messages']);
    });

    if (
      typeof Notification !== 'undefined' &&
      Notification.permission === 'granted' &&
      typeof document !== 'undefined' &&
      document.hidden
    ) {
      try {
        new Notification('Slang-English', {
          body: 'Nouveau message dans votre messagerie tuteur.',
          tag: 'tutor-dm'
        });
      } catch {
        /* ignore */
      }
    }
  }
}
