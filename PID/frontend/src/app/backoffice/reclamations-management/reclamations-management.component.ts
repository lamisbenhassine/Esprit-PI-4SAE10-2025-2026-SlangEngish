import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Reclamation, ReclamationService } from '../../services/reclamation.service';

@Component({
  selector: 'app-reclamations-management',
  templateUrl: './reclamations-management.component.html',
  styleUrls: ['./reclamations-management.component.css']
})
export class ReclamationsManagementComponent implements OnInit {
  reclamations: Reclamation[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  activeId: number | null = null;
  responseForm: { statut: string; reponseAdmin: string } = {
    statut: 'IN_PROGRESS',
    reponseAdmin: ''
  };

  constructor(
    private reclamationService: ReclamationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser || currentUser.role !== 'ADMIN') {
      this.errorMessage = 'This page is only available for admin.';
      return;
    }
    this.loadReclamations();
  }

  loadReclamations(): void {
    this.loading = true;
    this.errorMessage = '';
    this.reclamationService.getAll().subscribe({
      next: (data) => {
        this.reclamations = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'An error occurred while loading reclamations.';
        this.loading = false;
      }
    });
  }

  startTreatment(item: Reclamation): void {
    this.activeId = item.id ?? null;
    this.responseForm = {
      statut: item.statut === 'RESOLUE' ? 'RESOLUE' : (item.statut === 'IN_PROGRESS' ? 'IN_PROGRESS' : 'EN_COURS'),
      reponseAdmin: item.reponseAdmin || ''
    };
    this.errorMessage = '';
    this.successMessage = '';
  }

  submitTreatment(item: Reclamation): void {
    if (!item.id) return;
    if (!this.responseForm.reponseAdmin.trim()) {
      this.errorMessage = 'Admin response is required.';
      return;
    }

    this.reclamationService.traiterParAdmin(item.id, {
      statut: this.responseForm.statut,
      reponseAdmin: this.responseForm.reponseAdmin.trim()
    }).subscribe({
      next: () => {
        this.successMessage = 'Response sent to student successfully.';
        this.activeId = null;
        this.responseForm = { statut: 'IN_PROGRESS', reponseAdmin: '' };
        this.loadReclamations();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'An error occurred while sending the response.';
      }
    });
  }

  cancelTreatment(): void {
    this.activeId = null;
    this.responseForm = { statut: 'IN_PROGRESS', reponseAdmin: '' };
  }

  get inProgressCount(): number {
    return this.reclamations.filter(r => r.statut === 'EN_COURS' || r.statut === 'IN_PROGRESS').length;
  }

  get processedCount(): number {
    return this.reclamations.filter(r => r.statut === 'RESOLUE').length;
  }

  get pendingCount(): number {
    return this.reclamations.filter(r => r.statut === 'EN_ATTENTE').length;
  }

  get totalCount(): number {
    return this.reclamations.length;
  }

  get processedPercent(): number {
    return this.totalCount === 0 ? 0 : Math.round((this.processedCount * 100) / this.totalCount);
  }

  get inProgressPercent(): number {
    return this.totalCount === 0 ? 0 : Math.round((this.inProgressCount * 100) / this.totalCount);
  }

  get pendingPercent(): number {
    return this.totalCount === 0 ? 0 : Math.round((this.pendingCount * 100) / this.totalCount);
  }
}
