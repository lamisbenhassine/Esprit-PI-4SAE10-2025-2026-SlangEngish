import { Inject, Injectable, NgZone, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject } from 'rxjs';
import { DirectMessageService, DirectConversationSummary } from './direct-message.service';
import { DmReadPointerService } from './dm-read-pointer.service';

/**
 * Compte les conversations avec au moins un message non lu (dernier message de l’autre partie,
 * id &gt; dernier id lu stocké localement). Nécessite lastMessageId / lastMessageSenderId (API forum).
 */
@Injectable({ providedIn: 'root' })
export class MessagingUnreadService {
  readonly tutorUnread$ = new BehaviorSubject(0);
  readonly studentUnread$ = new BehaviorSubject(0);

  private studentPoll: ReturnType<typeof setInterval> | null = null;

  constructor(
    private dm: DirectMessageService,
    private readPtr: DmReadPointerService,
    private ngZone: NgZone,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  startStudentInboxWatch(getUserId: () => number): void {
    this.stopStudentInboxWatch();
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const tick = (): void => {
      const uid = getUserId();
      if (!uid) {
        return;
      }
      this.dm.inbox(uid).subscribe({
        next: rows => {
          const list = Array.isArray(rows) ? rows : [];
          this.ngZone.run(() => this.recomputeStudentRows(uid, list));
        },
        error: () => {}
      });
    };
    tick();
    // Hydration: keep periodic polling outside Angular zone.
    this.ngZone.runOutsideAngular(() => {
      this.studentPoll = globalThis.setInterval(tick, 12000);
    });
  }

  stopStudentInboxWatch(): void {
    if (this.studentPoll != null && isPlatformBrowser(this.platformId)) {
      globalThis.clearInterval(this.studentPoll);
    }
    this.studentPoll = null;
  }

  recomputeTutorRows(tutorId: number, rows: DirectConversationSummary[]): void {
    const n = rows.filter(r => this.isUnreadForViewer(tutorId, r)).length;
    this.tutorUnread$.next(n);
  }

  recomputeStudentRows(studentId: number, rows: DirectConversationSummary[]): void {
    const n = rows.filter(r => this.isUnreadForViewer(studentId, r)).length;
    this.studentUnread$.next(n);
  }

  /** Après marquage lu : recharge l’inbox et met à jour le compteur tuteur. */
  refreshTutorViewer(tutorId: number): void {
    this.dm.inbox(tutorId).subscribe({
      next: rows => {
        const list = Array.isArray(rows) ? rows.filter(r => r.kind === 'WITH_TUTOR') : [];
        this.recomputeTutorRows(tutorId, list);
      },
      error: () => {}
    });
  }

  refreshStudentViewer(studentId: number): void {
    this.dm.inbox(studentId).subscribe({
      next: rows => {
        const list = Array.isArray(rows) ? rows : [];
        this.recomputeStudentRows(studentId, list);
      },
      error: () => {}
    });
  }

  isUnreadForViewer(viewerId: number, row: DirectConversationSummary): boolean {
    if (row.lastMessageSenderId == null || row.lastMessageId == null) {
      return false;
    }
    if (row.lastMessageSenderId === viewerId) {
      return false;
    }
    const readUp = this.readPtr.getReadUpTo(viewerId, row.id);
    return row.lastMessageId > readUp;
  }
}
