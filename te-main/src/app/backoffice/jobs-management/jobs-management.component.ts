import { Component, OnInit, AfterViewInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { JobOffer } from '../../models/job-offer.model';
import { JobOfferService } from '../../services/job-offer.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-jobs-management',
  templateUrl: './jobs-management.component.html',
  styleUrls: ['./jobs-management.component.css']
})
export class JobsManagementComponent implements OnInit, AfterViewInit {
  jobOffers: JobOffer[] = [];
  dataSource = new MatTableDataSource<JobOffer>([]);
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  displayedColumns: string[] = ['title', 'company', 'contractType', 'location', 'salary', 'active', 'actions'];
  loading = false;
  showForm = false;
  isEditMode = false;
  jobForm!: FormGroup;
  contractTypes = ['CDI', 'CDD', 'STAGE', 'ALTERNANCE', 'FREELANCE'];

  // ✅ Autocomplétion lieu
  locationSuggestions: any[] = [];
  showSuggestions = false;
  private locationSearch$ = new Subject<string>();

  constructor(
    private jobOfferService: JobOfferService,
    private router: Router,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadJobOffers();

    // ✅ Autocomplétion avec debounce
    this.locationSearch$.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      switchMap(query => query.length > 2 
        ? this.jobOfferService.searchLocations(query) 
        : []
      )
    ).subscribe(results => {
      this.locationSuggestions = results;
      this.showSuggestions = results.length > 0;
    });
  }

  initForm(): void {
    this.jobForm = this.fb.group({
      id: [null],
      title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      company: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      contractType: ['CDI', Validators.required],
      location: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      latitude: [null],   // ✅ nouveau
      longitude: [null],  // ✅ nouveau
      salary: [null, [Validators.min(0), Validators.max(999999)]],
      description: ['', [Validators.maxLength(2000)]],
      active: [true],
      expirationDate: [null] // ✅ optionnel

    });
  }

  // ✅ Recherche lieu en temps réel
  onLocationInput(event: any): void {
    const query = event.target.value;
    this.locationSearch$.next(query);
  }

  // ✅ Sélection d'une ville depuis les suggestions
  selectLocation(suggestion: any): void {
    const displayName = suggestion.display_name.split(',').slice(0, 2).join(',').trim();
    this.jobForm.patchValue({
      location: displayName,
      latitude: parseFloat(suggestion.lat),
      longitude: parseFloat(suggestion.lon)
    });
    this.showSuggestions = false;
    this.locationSuggestions = [];
  }

  hideSuggestions(): void {
    setTimeout(() => { this.showSuggestions = false; }, 200);
  }

  loadJobOffers(): void {
    this.loading = true;
    this.jobOfferService.findAll().subscribe({
      next: (data: JobOffer[]) => {
        this.jobOffers = data;
        this.dataSource.data = data;
        this.dataSource.paginator = this.paginator;
        this.loading = false;
      },
      error: (err: any) => { console.error(err); this.loading = false; }
    });
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  openCreateForm(): void {
    this.showForm = true;
    this.isEditMode = false;
    this.jobForm.reset({ id: null, contractType: 'CDI', active: true });
    this.showSuggestions = false;
  }

  openEditForm(offer: JobOffer): void {
    this.showForm = true;
    this.isEditMode = true;
    this.jobForm.patchValue(offer);
    this.showSuggestions = false;
  }

  closeForm(): void {
    this.showForm = false;
    this.jobForm.reset();
    this.showSuggestions = false;
  }

 
  saveJobOffer(): void {
  if (this.jobForm.invalid) {
    this.jobForm.markAllAsTouched();
    return;
  }
  const jobData: JobOffer = { ...this.jobForm.value };

  // ✅ Si expirationDate vide → null explicite
  if (!jobData.expirationDate || jobData.expirationDate === '') {
    jobData.expirationDate = undefined;
  }

  if (this.isEditMode) {
    this.jobOfferService.update(jobData.id!, jobData).subscribe({
      next: () => { this.loadJobOffers(); this.closeForm(); },
      error: (err: any) => console.error(err)
    });
  } else {
    this.jobOfferService.create(jobData).subscribe({
      next: () => { this.loadJobOffers(); this.closeForm(); },
      error: (err: any) => console.error(err)
    });
  }
}

  deleteJobOffer(id: number): void {
    if (confirm('Supprimer cette offre ?')) {
      this.jobOfferService.delete(id).subscribe({
        next: () => this.loadJobOffers(),
        error: (err: any) => console.error(err)
      });
    }
  }

  toggleActive(offer: JobOffer): void {
    const updated = { ...offer, active: !offer.active };
    this.jobOfferService.update(offer.id!, updated).subscribe({
      next: () => this.loadJobOffers(),
      error: (err: any) => console.error(err)
    });
  }

  getErrorMessage(fieldName: string): string {
    const control = this.jobForm.get(fieldName);
    if (!control) return '';
    if (control.hasError('required')) return 'Ce champ est obligatoire';
    if (control.hasError('minlength')) return `Minimum ${control.errors?.['minlength'].requiredLength} caractères`;
    if (control.hasError('maxlength')) return `Maximum ${control.errors?.['maxlength'].requiredLength} caractères`;
    if (control.hasError('min')) return 'Le salaire doit être positif';
    if (control.hasError('max')) return 'Le salaire ne peut pas dépasser 999 999 €';
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const control = this.jobForm.get(fieldName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}