import { Injectable } from '@angular/core';
import { PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * Identifiant utilisateur aligné sur la session navigateur (sessionStorage),
 * rempli par AuthService à la connexion — pas de persistance cross-onglet / cross-visite.
 */
@Injectable({
  providedIn: 'root'
})
export class UserContextService {

  constructor(@Inject(PLATFORM_ID) private platformId: object) {}

  getCurrentUserId(): number {
    if (!isPlatformBrowser(this.platformId)) {
      return 1;
    }
    const stored = sessionStorage.getItem('userId');
    if (stored !== null && stored !== '') {
      const n = Number(stored);
      if (!Number.isNaN(n) && n > 0) {
        return n;
      }
    }
    return 1;
  }
}
