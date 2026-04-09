import { Component, OnInit } from '@angular/core';
import { Reclamation, ReclamationService } from '../../services/reclamation.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reclamations',
  templateUrl: './reclamations.component.html',
  styleUrls: ['./reclamations.component.css']
})
export class ReclamationsComponent implements OnInit {
  reclamations: Reclamation[] = [];
  formModel: Reclamation = {
    sujet: '',
    description: ''
  };
  studentId: number | null = null;
  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private reclamationService: ReclamationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser || currentUser.role !== 'STUDENT') {
      this.errorMessage = 'This page is only available for students.';
      return;
    }
    this.studentId = currentUser.id;
    this.loadMyReclamations();
  }

  loadMyReclamations(): void {
    if (!this.studentId) return;
    this.loading = true;
    this.reclamationService.getByStudent(this.studentId).subscribe({
      next: (data) => {
        this.reclamations = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  submit(): void {
    if (!this.studentId) return;
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.formModel.sujet?.trim() || !this.formModel.description?.trim()) {
      this.errorMessage = 'Subject and description are required.';
      return;
    }

    const payload: Reclamation = {
      sujet: this.formModel.sujet.trim(),
      description: this.formModel.description.trim(),
      studentId: this.studentId
    };

    this.reclamationService.create(payload).subscribe({
      next: () => {
        this.successMessage = 'Reclamation created successfully.';
        this.resetForm();
        this.loadMyReclamations();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'An error occurred while creating the reclamation.';
      }
    });
  }

  resetForm(): void {
    this.formModel = {
      sujet: '',
      description: ''
    };
  }

  getStatusLabel(status?: string): string {
    if (status === 'RESOLUE') return 'Processed';
    if (status === 'EN_COURS' || status === 'IN_PROGRESS') return 'In Progress';
    return 'Pending';
  }

  getStatusClass(status?: string): string {
    if (status === 'RESOLUE') return 'status-processed';
    if (status === 'EN_COURS' || status === 'IN_PROGRESS') return 'status-progress';
    return 'status-pending';
  }
}
