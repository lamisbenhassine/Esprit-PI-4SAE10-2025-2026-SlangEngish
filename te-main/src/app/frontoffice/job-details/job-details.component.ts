import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { JobOffer, Application } from '../../models/job-offer.model';
import { JobOfferService } from '../../services/job-offer.service';
import { ApplicationService } from '../../services/application.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-job-details',
  templateUrl: './job-details.component.html',
  styleUrls: ['./job-details.component.css']
})
export class JobDetailsComponent implements OnInit {
  jobOffer?: JobOffer;
  loading = false;
  applicationForm!: FormGroup;
  submitting = false;
  applicationSuccess = false;
  applicationError = '';

  // ✅ Fichiers PDF
  cvFile: File | null = null;
  coverLetterFile: File | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobOfferService: JobOfferService,
    private applicationService: ApplicationService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initApplicationForm();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadJobOffer(+id);
  }

  initApplicationForm(): void {
    this.applicationForm = this.fb.group({
      applicantName: ['', [Validators.required, Validators.minLength(3)]],
      applicantEmail: ['', [Validators.required, Validators.email]]
    });
  }

  loadJobOffer(id: number): void {
    this.loading = true;
    this.jobOfferService.findById(id).subscribe({
      next: (data: JobOffer) => { this.jobOffer = data; this.loading = false; },
      error: () => { this.loading = false; this.router.navigate(['/frontoffice/job-offers']); }
    });
  }

  // ✅ Sélection des fichiers
  onCvSelected(event: any): void {
    this.cvFile = event.target.files[0] || null;
  }

  onCoverLetterSelected(event: any): void {
    this.coverLetterFile = event.target.files[0] || null;
  }

  // ✅ Soumission avec upload
  submitApplication(): void {
    if (this.applicationForm.invalid) {
      this.applicationForm.markAllAsTouched();
      return;
    }
    if (!this.cvFile || !this.coverLetterFile) {
      this.applicationError = 'Veuillez uploader votre CV et votre lettre de motivation.';
      return;
    }

    this.submitting = true;
    this.applicationError = '';

    // Upload les 2 fichiers en parallèle puis envoie la candidature
    forkJoin({
      cvUrl: this.applicationService.uploadFile(this.cvFile),
      coverLetterUrl: this.applicationService.uploadFile(this.coverLetterFile)
    }).subscribe({
      next: ({ cvUrl, coverLetterUrl }) => {
        const application: any = {
          ...this.applicationForm.value,
          cvUrl,
          coverLetterUrl
        };
        this.applicationService.applyToOffer(this.jobOffer!.id!, application).subscribe({
          next: () => { 
            this.applicationSuccess = true; 
            this.submitting = false; 
          },
          error: () => { 
            this.applicationError = 'Erreur lors de la candidature.'; 
            this.submitting = false; 
          }
        });
      },
      error: () => { 
        this.applicationError = "Erreur lors de l'upload des fichiers."; 
        this.submitting = false; 
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/frontoffice/job-offers']);
  }

  isFieldInvalid(fieldName: string): boolean {
    const control = this.applicationForm.get(fieldName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}