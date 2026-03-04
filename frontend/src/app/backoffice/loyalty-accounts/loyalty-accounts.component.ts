import { Component, OnInit } from '@angular/core';
import { LoyaltyService, LoyaltySummary } from '../../core/services/loyalty.service';

interface LoyaltyAccountView extends LoyaltySummary {
  tierColor: 'bronze' | 'silver' | 'gold';
}

@Component({
  selector: 'app-loyalty-accounts',
  templateUrl: './loyalty-accounts.component.html',
  styleUrls: ['./loyalty-accounts.component.css']
})
export class LoyaltyAccountsComponent implements OnInit {
  displayedColumns: string[] = ['userId', 'tier', 'balancePoints', 'lifetimePoints'];
  accounts: LoyaltyAccountView[] = [];
  isLoading = false;
  error: string | null = null;

  constructor(private loyaltyService: LoyaltyService) {}

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.isLoading = true;
    this.error = null;
    this.loyaltyService.getAllAccounts().subscribe({
      next: (list) => {
        this.accounts = (list || []).map(acc => ({
          ...acc,
          tierColor: this.mapTierColor(acc.tier)
        }));
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.error = 'Impossible de charger les comptes fidélité. Vérifiez que le microservice Inscription est démarré.';
      }
    });
  }

  private mapTierColor(tier?: string | null): 'bronze' | 'silver' | 'gold' {
    const t = (tier || 'BRONZE').toUpperCase();
    if (t === 'GOLD') return 'gold';
    if (t === 'SILVER') return 'silver';
    return 'bronze';
  }
}

