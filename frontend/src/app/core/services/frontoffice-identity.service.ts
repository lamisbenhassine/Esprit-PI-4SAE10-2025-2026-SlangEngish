import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject } from 'rxjs';

const ACTIVE_USER_ID_KEY = 'frontoffice.activeUserId';

@Injectable({ providedIn: 'root' })
export class FrontofficeIdentityService {
  private readonly userIdSubject: BehaviorSubject<number>;
  readonly userId$;

  constructor(@Inject(PLATFORM_ID) private platformId: object) {
    let initial = 2;
    if (isPlatformBrowser(this.platformId)) {
      const stored = Number(localStorage.getItem(ACTIVE_USER_ID_KEY));
      // Frontoffice demo default: avoid hardcoded "user 1" identity.
      if (Number.isFinite(stored) && stored > 0) {
        initial = stored;
      }
    }
    this.userIdSubject = new BehaviorSubject<number>(initial);
    this.userId$ = this.userIdSubject.asObservable();
  }

  getCurrentUserId(): number {
    return this.userIdSubject.value;
  }

  setCurrentUserId(userId: number): void {
    if (!Number.isFinite(userId) || userId <= 0) {
      return;
    }
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(ACTIVE_USER_ID_KEY, String(userId));
    }
    this.userIdSubject.next(userId);
  }
}
