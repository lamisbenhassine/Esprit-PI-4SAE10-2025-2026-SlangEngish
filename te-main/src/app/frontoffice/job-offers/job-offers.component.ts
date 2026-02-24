import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { JobOffer } from '../../models/job-offer.model';
import { JobOfferService } from '../../services/job-offer.service';
import { NewOfferNotificationService } from '../../services/new-offer-notification.service';

@Component({
  selector: 'app-job-offers',
  templateUrl: './job-offers.component.html',
  styleUrls: ['./job-offers.component.css']
})
export class JobOffersComponent implements OnInit, OnDestroy {
  jobOffers: JobOffer[] = [];
  filteredOffers: JobOffer[] = [];
  loading = false;
  searchTerm = '';
  selectedContractType = 'ALL';
  private newOfferSub: Subscription | null = null;

  contractTypes = ['ALL', 'CDI', 'CDD', 'STAGE', 'ALTERNANCE', 'FREELANCE'];

  constructor(
    private jobOfferService: JobOfferService,
    private router: Router,
    private newOfferNotification: NewOfferNotificationService
  ) {}

  ngOnInit(): void {
    this.loadJobOffers();
    try {
      this.newOfferSub = this.newOfferNotification.onNewOffer.subscribe(() => this.loadJobOffers());
    } catch {
      // ignore
    }
  }

  ngOnDestroy(): void {
    try {
      this.newOfferSub?.unsubscribe();
      this.newOfferSub = null;
    } catch {
      // ignore
    }
  }

  loadJobOffers(): void {
    this.loading = true;
    this.jobOfferService.findAll().subscribe({
      next: (data: JobOffer[]) => {
        this.jobOffers = Array.isArray(data)
          ? data.filter((offer: JobOffer) => offer.active !== false)
          : [];
        this.applyFilters();
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement offres:', err);
        this.jobOffers = [];
        this.filteredOffers = [];
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let filtered = this.jobOffers || [];

    if (this.selectedContractType !== 'ALL') {
      filtered = filtered.filter(o => o.contractType === this.selectedContractType);
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(o =>
        (o.title?.toLowerCase() ?? '').includes(term) ||
        (o.company?.toLowerCase() ?? '').includes(term) ||
        (o.location?.toLowerCase() ?? '').includes(term)
      );
    }

    this.filteredOffers = filtered;
  }

  viewDetails(id: number): void {
    this.router.navigate(['/frontoffice/job-details', id]);
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onContractTypeChange(): void {
    this.applyFilters();
  }
}