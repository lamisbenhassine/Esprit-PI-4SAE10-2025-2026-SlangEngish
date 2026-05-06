import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

import { API_URL } from '../api.config';
import type { Role } from './user.service';

export interface AuthUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  photoBase64?: string;
}

export interface SignupPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: Role;
  phone?: string;
  address?: string;
  photoBase64?: string;
  /** 128 floats — face-api.js ; optionnel */
  faceDescriptor?: number[];
  recaptchaToken: string;
}

export interface SigninPayload {
  email: string;
  password: string;
}

export interface SigninFacePayload {
  email: string;
  faceDescriptor: number[];
}

export interface SigninFaceOnlyPayload {
  faceDescriptor: number[];
}

export interface ForgotPasswordPayload {
  email?: string;
  phone?: string;
  /** Canal : 'EMAIL' ou 'WHATSAPP' (optionnel, défaut EMAIL) */
  channel?: 'EMAIL' | 'WHATSAPP';
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}

export interface GoogleSigninPayload {
  idToken: string;
}

export interface FacebookSigninPayload {
  accessToken: string;
}


@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly apiUrl = `${API_URL}/auth`;

  /**
   * Profil de session (onglet courant uniquement — pas de localStorage : évite d’entrer dans le frontoffice
   * en collant une URL dans un nouvel onglet). Survit au retour Stripe / F5 dans le **même** onglet.
   */
  private static readonly AUTH_USER_KEY = 'slang.authUser';
  private static readonly USER_ID_KEY = 'userId';

  private currentUserSubject = new BehaviorSubject<AuthUser | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: object
  ) {
    this.hydrateFromStorage();
  }

  /** Après redirection externe (Stripe), même onglet : sessionStorage reste disponible. */
  private hydrateFromStorage(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    try {
      // Ancienne persistance localStorage : ne plus l’utiliser pour l’accès (évite URL collée = connecté).
      localStorage.removeItem(AuthService.AUTH_USER_KEY);
    } catch {
      /* ignore */
    }
    try {
      const raw = sessionStorage.getItem(AuthService.AUTH_USER_KEY);
      if (!raw) {
        return;
      }
      const user = JSON.parse(raw) as AuthUser;
      if (
        user &&
        typeof user.id === 'number' &&
        user.id > 0 &&
        typeof user.email === 'string' &&
        user.email.length > 0
      ) {
        this.currentUserSubject.next(user);
      } else {
        sessionStorage.removeItem(AuthService.AUTH_USER_KEY);
        sessionStorage.removeItem(AuthService.USER_ID_KEY);
      }
    } catch {
      sessionStorage.removeItem(AuthService.AUTH_USER_KEY);
      sessionStorage.removeItem(AuthService.USER_ID_KEY);
    }
  }

  private setCurrentUser(user: AuthUser | null): void {
    if (isPlatformBrowser(this.platformId)) {
      if (user?.id != null) {
        sessionStorage.setItem(AuthService.USER_ID_KEY, String(user.id));
        sessionStorage.setItem(AuthService.AUTH_USER_KEY, JSON.stringify(user));
        // Ne plus persister l’id en localStorage : sinon accès « fantôme » sans passer par la connexion.
        localStorage.removeItem(AuthService.USER_ID_KEY);
      } else {
        sessionStorage.removeItem(AuthService.USER_ID_KEY);
        sessionStorage.removeItem(AuthService.AUTH_USER_KEY);
        localStorage.removeItem(AuthService.USER_ID_KEY);
      }
    }
    this.currentUserSubject.next(user);
  }

  signup(payload: SignupPayload): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.apiUrl}/signup`, payload).pipe(
      tap(user => this.setCurrentUser(user))
    );
  }

  signin(payload: SigninPayload): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.apiUrl}/signin`, payload).pipe(
      tap(user => this.setCurrentUser(user))
    );
  }

  signinFace(payload: SigninFacePayload): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.apiUrl}/signin-face`, payload).pipe(
      tap(user => this.setCurrentUser(user))
    );
  }

  signinFaceOnly(payload: SigninFaceOnlyPayload): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.apiUrl}/signin-face-only`, payload).pipe(
      tap(user => this.setCurrentUser(user))
    );
  }

  forgotPassword(payload: ForgotPasswordPayload): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/forgot-password`, payload);
  }

  resetPassword(payload: ResetPasswordPayload): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/reset-password`, payload);
  }

  googleSignin(idToken: string): Observable<AuthUser> {
    const body: GoogleSigninPayload = { idToken };
    return this.http.post<AuthUser>(`${this.apiUrl}/google-signin`, body).pipe(
      tap(user => this.setCurrentUser(user))
    );
  }

  facebookSignin(accessToken: string): Observable<AuthUser> {
    const body: FacebookSigninPayload = { accessToken };
    return this.http.post<AuthUser>(`${this.apiUrl}/facebook-signin`, body).pipe(
      tap(user => this.setCurrentUser(user))
    );
  }

  signout(): void {
    this.setCurrentUser(null);
  }

  getCurrentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  isLoggedIn(): boolean {
    return this.getCurrentUser() !== null;
  }

  getCurrentUserId(): number | null {
    return this.getCurrentUser()?.id ?? null;
  }

  getDisplayName(): string {
    const user = this.getCurrentUser();
    if (!user) return '';
    return `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
  }

  getLoginTime(): string | null {
    return null;
  }
}

