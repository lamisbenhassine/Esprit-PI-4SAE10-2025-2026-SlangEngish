import { Component, OnInit } from '@angular/core';
import { UserService, User } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  isSaving = false;
  saveError: string | null = null;
  saveSuccess = false;

  passwordData = {
    current: '',
    new: '',
    confirm: ''
  };

  constructor(
    private userService: UserService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const current = this.authService.getCurrentUser();
    if (!current) {
      return;
    }

    this.userService.getById(current.id).subscribe({
      next: (u) => {
        this.user = { ...u };
      },
      error: (err) => {
        console.error('Failed to load profile user', err);
      }
    });
  }

  /** Retourne l'URL affichable pour la photo (Google/Facebook URL ou base64 signup). */
  getAvatarSrc(user: User): string {
    const p = user?.photoBase64;
    if (!p || !p.trim()) return '';
    if (p.startsWith('http://') || p.startsWith('https://')) return p;
    return `data:image/jpeg;base64,${p}`;
  }

  saveProfile() {
    if (!this.user) {
      return;
    }
    this.isSaving = true;
    this.saveError = null;
    this.saveSuccess = false;

    const payload = {
      firstName: this.user.firstName,
      lastName: this.user.lastName,
      email: this.user.email,
      phone: this.user.phone,
      address: this.user.address
    };

    this.userService.updateProfile(this.user.id!, payload).subscribe({
      next: (updated) => {
        this.user = { ...this.user!, ...updated };
        this.isSaving = false;
        this.saveSuccess = true;
      },
      error: (err) => {
        console.error('Failed to save profile', err);
        this.isSaving = false;
        this.saveError = 'Impossible d\'enregistrer le profil. Réessayez plus tard.';
      }
    });
  }

  cancelEdit() {
    console.log('Edit cancelled');
  }

  changePassword() {
    if (this.passwordData.new !== this.passwordData.confirm) {
      console.error('Passwords do not match');
      return;
    }
    console.log('Password changed');
    this.passwordData = { current: '', new: '', confirm: '' };
  }
}
