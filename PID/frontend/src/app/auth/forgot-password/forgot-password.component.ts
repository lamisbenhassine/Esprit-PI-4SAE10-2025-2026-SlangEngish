import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  email = '';
  isSubmitting = false;
  success = false;
  error: string | null = null;

  constructor(private authService: AuthService) {}

  isValidEmail(email: string): boolean {
    if (!email || !email.trim()) return false;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email.trim());
  }

  onSubmit(): void {
    this.error = null;
    if (!this.email || !this.email.trim()) {
      this.error = 'Veuillez entrer votre adresse email.';
      return;
    }
    if (!this.isValidEmail(this.email)) {
      this.error = 'Veuillez entrer une adresse email valide.';
      return;
    }
    if (this.isSubmitting) return;

    this.isSubmitting = true;
    this.authService.forgotPassword({ email: this.email.trim() }).subscribe({
      next: () => {
        this.success = true;
        this.isSubmitting = false;
      },
      error: () => {
        this.isSubmitting = false;
        this.error = 'Une erreur est survenue. Réessayez plus tard.';
      }
    });
  }
}

