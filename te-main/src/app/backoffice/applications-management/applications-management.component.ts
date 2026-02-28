import { Component, OnInit } from '@angular/core';
import { ApplicationService } from '../../services/application.service';
import { JobOfferService } from '../../services/job-offer.service';
import { Application, JobOffer } from '../../models/job-offer.model';

@Component({
  selector: 'app-applications-management',
  templateUrl: './applications-management.component.html',
  styleUrls: ['./applications-management.component.css']
})
export class ApplicationsManagementComponent implements OnInit {

  applications: Application[] = [];
  filteredApplications: Application[] = [];
  jobOffers: JobOffer[] = [];
  loading = false;

  // Filtres
  selectedJobOfferId = 'ALL';
  selectedStatus = 'ALL';
  selectedContractType = 'ALL';
  searchTerm = '';

  statuses = ['ALL', 'PENDING', 'REVIEWED', 'ACCEPTED', 'REJECTED'];
  contractTypes = ['ALL', 'CDI', 'CDD', 'STAGE', 'ALTERNANCE', 'FREELANCE'];

  pageSize = 10;
  currentPage = 1;

  constructor(
    private applicationService: ApplicationService,
    private jobOfferService: JobOfferService
  ) {}

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    this.jobOfferService.findAll().subscribe({
      next: (offers) => { this.jobOffers = offers; },
      error: () => {}
    });
    this.applicationService.findAll().subscribe({
      next: (apps) => {
        this.applications = apps;
        this.applyFilters();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  applyFilters(): void {
    let filtered = [...this.applications];

    if (this.selectedJobOfferId !== 'ALL') {
      filtered = filtered.filter(a => a.jobOfferId === +this.selectedJobOfferId);
    }

    if (this.selectedStatus !== 'ALL') {
      filtered = filtered.filter(a => a.status === this.selectedStatus);
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(a =>
        a.applicantName?.toLowerCase().includes(term) ||
        a.applicantEmail?.toLowerCase().includes(term)
      );
    }

    this.filteredApplications = filtered;
    this.currentPage = 1;
  }

  get paginatedApplications(): Application[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredApplications.slice(start, start + this.pageSize);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 1;
  }

  getJobTitle(jobOfferId: number): string {
    const offer = this.jobOffers.find(o => o.id === jobOfferId);
    return offer ? offer.title : 'Offre inconnue';
  }

  getJobContractType(jobOfferId: number): string {
    const offer = this.jobOffers.find(o => o.id === jobOfferId);
    return offer ? offer.contractType : '';
  }

  openFile(url: string): void {
    window.open(url, '_blank');
  }

  updateStatus(app: Application, status: string): void {
    const updated = { ...app, status: status as any };
    this.applicationService.update(app.id!, updated).subscribe({
      next: () => { app.status = status as any; },
      error: () => {}
    });
  }


  getStatusColor(status: string): string {
  switch (status) {
    case 'PENDING': return 'orange';
    case 'INTERVIEW': return 'blue';
    case 'ACCEPTED': return 'green';
    case 'REJECTED': return 'red';
    case 'CANCELLED': return 'gray';
    default: return 'gray';
  }
}

getStatusLabel(status: string): string {
  switch (status) {
    case 'ALL': return 'Tous les statuts';
    case 'PENDING': return '⏳ En attente';
    case 'INTERVIEW': return '📅 Entretien';
    case 'ACCEPTED': return '✅ Accepté';
    case 'REJECTED': return '❌ Refusé';
    case 'CANCELLED': return '🚫 Annulé';
    default: return status;
  }
}

  
}