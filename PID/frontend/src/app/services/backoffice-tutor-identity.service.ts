import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject } from 'rxjs';

const STORAGE_KEY = 'backoffice.activeTutorId';

/** Profil tuteur utilisé dans le backoffice pour lire/répondre aux DM (séparé du front-office étudiant). */
@Injectable({ providedIn: 'root' })
export class BackofficeTutorIdentityService {
  private readonly subject: BehaviorSubject<number>;
  readonly tutorId$;

  constructor(@Inject(PLATFORM_ID) private platformId: object) {
    let initial = 12;
    if (isPlatformBrowser(this.platformId)) {
      const raw = Number(localStorage.getItem(STORAGE_KEY));
      if (Number.isFinite(raw) && raw > 0) {
        initial = raw;
      }
    }
    this.subject = new BehaviorSubject<number>(initial);
    this.tutorId$ = this.subject.asObservable();
  }

  getTutorId(): number {
    return this.subject.value;
  }

  setTutorId(id: number): void {
    if (!Number.isFinite(id) || id <= 0) {
      return;
    }
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(STORAGE_KEY, String(id));
    }
    this.subject.next(id);
  }
}
