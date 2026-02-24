import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { JobOffer } from '../../models/job-offer.model';
import { JobOfferService } from '../../services/job-offer.service';

@Component({
  selector: 'app-jobs-management',
  templateUrl: './jobs-management.component.html',
  styleUrls: ['./jobs-management.component.css']
})
export class JobsManagementComponent implements OnInit {
  jobOffers: JobOffer[] = [];
  displayedColumns: string[] = ['title', 'company', 'contractType', 'location', 'salary', 'active', 'actions'];
  loading = false;
  
  showForm = false;
  isEditMode = false;
  jobForm!: FormGroup;  // ✅ Formulaire réactif
  
  contractTypes = ['CDI', 'CDD', 'STAGE', 'ALTERNANCE', 'FREELANCE'];

  constructor(
    private jobOfferService: JobOfferService,
    private router: Router,
    private fb: FormBuilder  // ✅ Injection du FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();  // ✅ Initialiser le formulaire
    this.loadJobOffers();
  }

  // ✅ Initialisation du formulaire avec validations
  initForm(): void {
    this.jobForm = this.fb.group({
      id: [null],
      title: ['', [
        Validators.required, 
        Validators.minLength(3),
        Validators.maxLength(100)
      ]],
      company: ['', [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(100)
      ]],
      contractType: ['CDI', Validators.required],
      location: ['', [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(100)
      ]],
      salary: [null, [
        Validators.min(0),
        Validators.max(999999)
      ]],
      description: ['', [
        Validators.maxLength(2000)
      ]],
      active: [true]
    });
  }

  loadJobOffers(): void {
    this.loading = true;
    this.jobOfferService.findAll().subscribe({
      next: (data: JobOffer[]) => {
        this.jobOffers = data;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement offres:', err);
        this.loading = false;
      }
    });
  }

  openCreateForm(): void {
    this.showForm = true;
    this.isEditMode = false;
    this.jobForm.reset({
      id: null,
      title: '',
      company: '',
      contractType: 'CDI',
      location: '',
      salary: null,
      description: '',
      active: true
    });
  }

  openEditForm(offer: JobOffer): void {
    this.showForm = true;
    this.isEditMode = true;
    this.jobForm.patchValue(offer);  // ✅ Remplir le formulaire avec les données
  }

  closeForm(): void {
    this.showForm = false;
    this.jobForm.reset();
  }

  // ✅ Sauvegarde avec validation
  saveJobOffer(): void {
    // Vérifier si le formulaire est valide
    if (this.jobForm.invalid) {
      this.jobForm.markAllAsTouched();  // Afficher toutes les erreurs
      return;
    }

    const jobData: JobOffer = this.jobForm.value;

    if (this.isEditMode) {
      this.jobOfferService.update(jobData.id!, jobData).subscribe({
        next: () => {
          this.loadJobOffers();
          this.closeForm();
        },
        error: (err: any) => console.error('Erreur modification:', err)
      });
    } else {
      this.jobOfferService.create(jobData).subscribe({
        next: () => {
          this.loadJobOffers();
          this.closeForm();
        },
        error: (err: any) => console.error('Erreur création:', err)
      });
    }
  }

  deleteJobOffer(id: number): void {
    if (confirm('Supprimer cette offre ?')) {
      this.jobOfferService.delete(id).subscribe({
        next: () => this.loadJobOffers(),
        error: (err: any) => console.error('Erreur suppression:', err)
      });
    }
  }

  toggleActive(offer: JobOffer): void {
    const updated = { ...offer, active: !offer.active };
    this.jobOfferService.update(offer.id!, updated).subscribe({
      next: () => this.loadJobOffers(),
      error: (err: any) => console.error('Erreur changement statut:', err)
    });
  }

  // ✅ Méthode pour obtenir les messages d'erreur
  getErrorMessage(fieldName: string): string {
    const control = this.jobForm.get(fieldName);
    
    if (!control) return '';

    if (control.hasError('required')) {
      return 'Ce champ est obligatoire';
    }
    if (control.hasError('minlength')) {
      const minLength = control.errors?.['minlength'].requiredLength;
      return `Minimum ${minLength} caractères`;
    }
    if (control.hasError('maxlength')) {
      const maxLength = control.errors?.['maxlength'].requiredLength;
      return `Maximum ${maxLength} caractères`;
    }
    if (control.hasError('min')) {
      return `Le salaire doit être positif`;
    }
    if (control.hasError('max')) {
      return `Le salaire ne peut pas dépasser 999 999 €`;
    }
    
    return '';
  }

  // ✅ Méthode helper pour vérifier si un champ est invalide
  isFieldInvalid(fieldName: string): boolean {
    const control = this.jobForm.get(fieldName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}