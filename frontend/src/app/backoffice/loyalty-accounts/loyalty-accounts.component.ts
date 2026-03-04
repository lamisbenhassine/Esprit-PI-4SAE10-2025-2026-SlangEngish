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
  selected: LoyaltyAccountView | null = null;
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
        if (!this.accounts || this.accounts.length < 4) {
          // Aucun compte trouvé : créer des comptes de démo pour visualiser les paliers
          this.loyaltyService.seedDemoAccounts().subscribe({
            next: (seeded) => {
              this.accounts = (seeded || []).map(acc => ({
                ...acc,
                tierColor: this.mapTierColor(acc.tier)
              }));
              this.selected = this.accounts[0] || null;
              this.isLoading = false;
            },
            error: () => {
              this.isLoading = false;
              this.error = 'Impossible de créer les comptes fidélité de démonstration.';
            }
          });
        } else {
          if (!this.selected && this.accounts.length > 0) {
            this.selected = this.accounts[0];
          }
          this.isLoading = false;
        }
      },
      error: () => {
        this.isLoading = false;
        this.error = 'Impossible de charger les comptes fidélité. Vérifiez que le microservice Inscription est démarré.';
      }
    });
  }

  selectAccount(row: LoyaltyAccountView): void {
    this.selected = row;
  }

  private mapTierColor(tier?: string | null): 'bronze' | 'silver' | 'gold' {
    const t = (tier || 'BRONZE').toUpperCase();
    if (t === 'GOLD') return 'gold';
    if (t === 'SILVER') return 'silver';
    return 'bronze';
  }
}


