import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LoyaltyService, LoyaltySummary } from '../../core/services/loyalty.service';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  name: string;
  email: string;
  phone: string;
  address: string;
  role: string;
  avatar?: string;
}

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  user: User = {
    id: 1,
    firstName: 'John',
    lastName: 'Doe',
    name: 'John Doe',
    email: 'john.doe@example.com',
    phone: '+1 (555) 123-4567',
    address: '123 Main St, Anytown, USA 12345',
    role: 'Student',
    avatar: ''
  };

  passwordData = {
    current: '',
    new: '',
    confirm: ''
  };

  loyaltySummary: LoyaltySummary | null = null;
  loyaltyLoading = false;

  constructor(
    private loyaltyService: LoyaltyService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadLoyalty();
  }

  loadLoyalty(): void {
    this.loyaltyLoading = true;
    this.loyaltyService.getSummary(this.user.id).subscribe({
      next: (s) => {
        this.loyaltySummary = s;
        this.loyaltyLoading = false;
      },
      error: () => {
        this.loyaltyLoading = false;
        this.loyaltySummary = null;
      }
    });
  }

  get loyaltyProgressPercent(): number {
    if (!this.loyaltySummary) return 0;
    const pts = this.loyaltySummary.lifetimePoints || 0;
    const tier = this.loyaltySummary.tier || 'BRONZE';
    if (tier === 'GOLD') return 100;
    if (tier === 'SILVER') return Math.min(100, ((pts - 5000) / 10000) * 100);
    return Math.min(100, (pts / 5000) * 100);
  }

  get nextTierLabel(): string {
    if (!this.loyaltySummary) return '';
    const tier = this.loyaltySummary.tier || 'BRONZE';
    if (tier === 'GOLD') return 'Max tier';
    if (tier === 'SILVER') return '15,000 pts for GOLD';
    return '5,000 pts for SILVER';
  }

  goToCheckout(): void {
    this.router.navigate(['/frontoffice/inscription/cart']);
  }

  viewRewards(): void {
    this.router.navigate(['/frontoffice/inscription/offers']);
  }

  saveProfile() {
    this.user.name = `${this.user.firstName} ${this.user.lastName}`;
    console.log('Profile saved:', this.user);
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
