import { Component, OnInit, OnDestroy, AfterViewInit, inject, Injector, afterNextRender, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { Subject, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { JobOffer, SavedOffer } from '../../models/job-offer.model';
import { JobOfferService } from '../../services/job-offer.service';
import { NewOfferNotificationService } from '../../services/new-offer-notification.service';
import { SavedOfferService } from '../../services/saved-offer.service';
import { MatchingService, MatchingResult } from '../../services/matching.service';


@Component({
  selector: 'app-job-offers',
  templateUrl: './job-offers.component.html',
  styleUrls: ['./job-offers.component.css']
})
export class JobOffersComponent implements OnInit, OnDestroy, AfterViewInit {
  jobOffers: JobOffer[] = [];
  filteredOffers: JobOffer[] = [];
  loading = false;
  /** True après la première réponse du backend (évite d'afficher "Aucune offre" avant le chargement). */
  loadCompleted = false;
  searchTerm = '';
  selectedContractType = 'ALL';
  showMap = false;
  private readonly destroy$ = new Subject<void>();
  private map: any;
  private L: any | null = null;
  private iconDefault: any | null = null;
  private readonly platformId = inject(PLATFORM_ID);
  private readonly injector = inject(Injector);

  contractTypes = ['ALL', 'CDI', 'CDD', 'STAGE', 'ALTERNANCE', 'FREELANCE'];

  savedOffers: SavedOffer[] = [];
  studentId = 1;
  matchingScores: Map<number, MatchingResult> = new Map();

  

  // ✅ Pagination
  currentPage = 1;
  pageSize = 6;

  get paginatedOffers(): JobOffer[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredOffers.slice(start, start + this.pageSize);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 1;
  }

  /** Timer d'expiration : basé uniquement sur expirationDate (null = pas d'expiration). */
  expiryTimers: Map<number, string> = new Map();
  expiryColors: Map<number, string> = new Map();
  private expiredOfferIds: Set<number> = new Set();

  constructor(
    private jobOfferService: JobOfferService,
    private router: Router,
    private newOfferNotification: NewOfferNotificationService,
    private savedOfferService: SavedOfferService,
    private matchingService: MatchingService // ✅ ajoute

  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.loadJobOffers();
    this.loadSavedOffers();
    this.startExpiryTimers();
    this.startSyncInterval();
    this.loadMatchingScores();


    this.newOfferNotification.onNewOffer
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.jobOfferService.findAll().subscribe({
          next: (data: JobOffer[]) => {
            const newOffers = data.filter((o: JobOffer) => o.active !== false);
            newOffers.forEach(offer => {
              if (!this.jobOffers.some(o => o.id === offer.id)) {
                this.jobOffers.unshift(offer);
              }
            });
            this.applyFilters();
          }
        });
      });
  }

  ngAfterViewInit(): void { }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.map) { this.map.remove(); this.map = null; }
  }

  // ✅ Recharge depuis le backend toutes les 30 secondes (RxJS timer)
  startSyncInterval(): void {
    timer(30000, 30000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadJobOffers());
  }

  loadJobOffers(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.loading = true;
    this.jobOfferService.findAll().subscribe({
      next: (data: JobOffer[]) => {
        const list = Array.isArray(data) ? data : [];
        this.jobOffers = list.filter((offer: JobOffer) => offer.active !== false);
        this.applyFilters();
        this.loading = false;
        this.loadCompleted = true;
      },
      error: () => {
        this.jobOffers = [];
        this.filteredOffers = [];
        this.loading = false;
        this.loadCompleted = true;
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
    this.currentPage = 1; // ✅ reset pagination
    if (this.showMap && isPlatformBrowser(this.platformId) && this.L) {
      afterNextRender(() => this.initMapWithInvalidateSize(), { injector: this.injector });
    }
  }

  /** Timer d'expiration : uniquement si expirationDate est définie ; sinon "Sans expiration". */
  startExpiryTimers(): void {
    timer(0, 1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.updateTimers());
  }

  updateTimers(): void {
    let needsReload = false;

    this.filteredOffers.forEach(offer => {
      if (!offer.id) return;
      if (!offer.expirationDate) {
        this.expiryTimers.set(offer.id, 'Sans expiration');
        this.expiryColors.set(offer.id, 'green');
        return;
      }

      const expiryAt = new Date(offer.expirationDate).getTime();
      const now = Date.now();
      const remaining = expiryAt - now;

      let newTimer: string;
      let newColor: string;

      if (remaining <= 0) {
        newTimer = 'Expirée';
        newColor = 'red';
        if (!this.expiredOfferIds.has(offer.id)) {
          this.expiredOfferIds.add(offer.id);
          needsReload = true;
        }
      } else {
        const minutes = Math.floor(remaining / 60000);
        const seconds = Math.floor((remaining % 60000) / 1000);
        newTimer = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
        newColor = remaining <= 60000 ? 'red' : 'green';
      }

      if (this.expiryTimers.get(offer.id) !== newTimer) {
        this.expiryTimers.set(offer.id, newTimer);
      }
      if (this.expiryColors.get(offer.id) !== newColor) {
        this.expiryColors.set(offer.id, newColor);
      }
    });

    if (needsReload) {
      timer(5000).pipe(takeUntil(this.destroy$)).subscribe(() => this.loadJobOffers());
    }
  }

  getExpiryTimer(offerId: number): string {
    return this.expiryTimers.get(offerId) ?? '--:--';
  }

  getExpiryColor(offerId: number): string {
    return this.expiryColors.get(offerId) ?? 'green';
  }

  toggleView(): void {
    this.showMap = !this.showMap;
    if (this.showMap) {
      if (isPlatformBrowser(this.platformId)) {
        if (!this.L) {
          import('leaflet').then((leafletModule) => {
            this.L = leafletModule;
            this.iconDefault = this.L.icon({
              iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
              shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
              iconSize: [25, 41],
              iconAnchor: [12, 41],
              popupAnchor: [1, -34],
            });
            afterNextRender(() => this.initMapWithInvalidateSize(), { injector: this.injector });
          }).catch(() => {});
        } else {
          afterNextRender(() => this.initMapWithInvalidateSize(), { injector: this.injector });
        }
      }
    } else {
      if (this.map) { this.map.remove(); this.map = null; }
    }
  }

  private initMapWithInvalidateSize(): void {
    this.initMap();
    if (this.map) { this.map.invalidateSize(); }
  }

  initMap(): void {
    if (!this.L) return;
    if (this.map) { this.map.remove(); this.map = null; }

    const offersWithCoords = this.filteredOffers.filter(
      o => o.latitude != null && o.longitude != null
    );
    if (offersWithCoords.length === 0) return;

    this.map = this.L.map('offers-map', {
      center: [offersWithCoords[0].latitude!, offersWithCoords[0].longitude!],
      zoom: 6
    });

    this.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap'
    }).addTo(this.map);

    offersWithCoords.forEach(offer => {
      const color = this.getMarkerColor(offer.contractType);
      const marker = this.L.marker([offer.latitude!, offer.longitude!], { icon: this.iconDefault });

      const popupHtml = `
        <div style="min-width:200px">
          <h4 style="margin:0 0 6px;color:#1877f2">${offer.title}</h4>
          <p style="margin:2px 0"><b>🏢</b> ${offer.company}</p>
          <p style="margin:2px 0"><b>📍</b> ${offer.location}</p>
          <p style="margin:4px 0">
            <span style="background:${color};color:white;padding:2px 8px;
                         border-radius:10px;font-size:12px">
              ${offer.contractType}
            </span>
          </p>
          ${offer.salary ? `<p style="margin:4px 0"><b>💰</b> ${offer.salary} €/an</p>` : ''}
          <button type="button" data-offer-id="${offer.id}"
            style="margin-top:8px;background:#1877f2;color:white;border:none;
                   padding:6px 12px;border-radius:6px;cursor:pointer;width:100%">
            Postuler
          </button>
        </div>
      `;

      marker.addTo(this.map).bindPopup(popupHtml);

      marker.on('popupopen', (event: any) => {
        const popupEl = event.popup?.getElement() as HTMLElement | null;
        if (!popupEl) return;
        const button = popupEl.querySelector('button[data-offer-id]') as HTMLButtonElement | null;
        if (!button) return;
        button.onclick = () => {
          this.router.navigate(['/frontoffice/job-details', offer.id]);
        };
      });
    });

    const bounds = this.L.latLngBounds(
      offersWithCoords.map(o => [o.latitude!, o.longitude!] as [number, number])
    );
    this.map.fitBounds(bounds, { padding: [50, 50] });
  }

  getMarkerColor(contractType: string): string {
    switch (contractType) {
      case 'CDI': return '#4CAF50';
      case 'CDD': return '#FF9800';
      case 'STAGE': return '#2196F3';
      case 'ALTERNANCE': return '#9C27B0';
      case 'FREELANCE': return '#F44336';
      default: return '#607D8B';
    }
  }

  hasOffersWithCoords(): boolean {
    return this.filteredOffers.some(o => o.latitude != null && o.longitude != null);
  }

  viewDetails(id: number): void {
    this.router.navigate(['/frontoffice/job-details', id]);
  }

  onSearchChange(): void { this.applyFilters(); }
  onContractTypeChange(): void { this.applyFilters(); }

  loadSavedOffers(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.savedOfferService.findAll().subscribe({
      next: (data: SavedOffer[]) => { this.savedOffers = data; },
      error: () => {}
    });
  }

  isSaved(offerId: number): boolean {
    return this.savedOffers.some(s => s.jobOfferId === offerId);
  }

  getSavedId(offerId: number): number | undefined {
    return this.savedOffers.find(s => s.jobOfferId === offerId)?.id;
  }

  toggleSave(event: Event, offer: JobOffer): void {
    event.stopPropagation();
    if (this.isSaved(offer.id!)) {
      const savedId = this.getSavedId(offer.id!);
      if (savedId) {
        this.savedOfferService.unsave(savedId).subscribe({
          next: () => {
            this.savedOffers = this.savedOffers.filter(s => s.id !== savedId);
          }
        });
      }
    } else {
      this.savedOfferService.save(offer.id!, this.studentId).subscribe({
        next: (saved: SavedOffer) => {
          this.savedOffers.push(saved);
        }
      });
    }
  }


  loadMatchingScores(): void {
  if (!isPlatformBrowser(this.platformId)) return;
  this.matchingService.getMatchingOffers(this.studentId).subscribe({
    next: (results: MatchingResult[]) => {
      results.forEach(r => {
        this.matchingScores.set(r.offerId, r);
      });
    },
    error: () => {}
  });
}

getMatchPercent(offerId: number): number {
  return this.matchingScores.get(offerId)?.matchPercent ?? 50;
}

getMatchLevel(offerId: number): string {
  return this.matchingScores.get(offerId)?.matchLevel ?? 'Moyen';
}

getMatchColor(offerId: number): string {
  const percent = this.getMatchPercent(offerId);
  if (percent >= 75) return '#4CAF50'; // vert
  if (percent >= 50) return '#FF9800'; // orange
  return '#F44336';                    // rouge
}

getMatchBgColor(offerId: number): string {
  const percent = this.getMatchPercent(offerId);
  if (percent >= 75) return '#E8F5E9';
  if (percent >= 50) return '#FFF3E0';
  return '#FFEBEE';
}
}