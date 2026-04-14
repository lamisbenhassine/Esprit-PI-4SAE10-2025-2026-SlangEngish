import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SubscriptionPlanService } from '../../../core/services/subscription-plan.service';
import { PlacementService, CertificateAnalysis } from '../../../core/services/placement.service';

@Component({
    selector: 'app-placement-form',
    templateUrl: './placement-form.component.html',
    styleUrls: ['./placement-form.component.css']
})
export class PlacementFormComponent implements OnInit {
    placementForm: FormGroup;
    levels = [
        {
            value: 'A1',
            label: 'Beginner (A1)',
            icon: 'child_care',
            description: 'Start from scratch with basic vocabulary and essential grammar.',
            examplePrice: '29.99 TND',
            benefits: ['Basic Greetings', 'Daily Nouns', 'Simple Sentences']
        },
        {
            value: 'A2',
            label: 'Elementary (A2)',
            icon: 'person',
            description: 'Communicate in simple, routine tasks and everyday situations.',
            examplePrice: '39.99 TND',
            benefits: ['Past Tense', 'Shopping & Travel', 'Family Descriptions']
        },
        {
            value: 'B1',
            label: 'Intermediate (B1)',
            icon: 'school',
            description: 'Handle most situations while traveling. Describe experiences & dreams.',
            examplePrice: '49.99 TND',
            benefits: ['Complex Dialogues', 'Work Vocabulary', 'Travel Fluency']
        },
        {
            value: 'B2',
            label: 'Upper Int. (B2)',
            icon: 'psychology',
            description: 'Understand main ideas of complex text. Interact with native speakers.',
            examplePrice: '59.99 TND',
            benefits: ['Abstract Topics', 'Business English', 'Fluent Debate']
        },
        {
            value: 'C1',
            label: 'Advanced (C1)',
            icon: 'auto_stories',
            description: 'Express ideas fluently without much obvious searching for expressions.',
            examplePrice: '79.99 TND',
            benefits: ['Professional Writing', 'Subtle Meanings', 'Academic Success']
        }
    ];

    selectedCertificate: File | null = null;
    analysisResult: CertificateAnalysis | null = null;

    constructor(
        private fb: FormBuilder,
        private router: Router,
        private subscriptionPlanService: SubscriptionPlanService,
        private placementService: PlacementService,
        private snackBar: MatSnackBar
    ) {
        this.placementForm = this.fb.group({
            firstName: ['', Validators.required],
            lastName: ['', Validators.required],
            email: ['', [Validators.required, Validators.email]],
            // estimatedLevel is filled automatically from certificate, not required directly from UI
            estimatedLevel: [''],
            goals: [''],
            certificate: [null]
        });
    }

    ngOnInit(): void { }

    onCertificateSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (!input.files || input.files.length === 0) {
            this.selectedCertificate = null;
            this.placementForm.get('certificate')?.setValue(null);
            return;
        }
        const file = input.files[0];
        if (file.size > 5 * 1024 * 1024) {
            this.snackBar.open('Certificate is too large (max 5 MB).', 'OK', { duration: 5000 });
            this.selectedCertificate = null;
            this.placementForm.get('certificate')?.setValue(null);
            return;
        }
        const lower = file.name.toLowerCase();
        if (!(lower.endsWith('.pdf') || lower.endsWith('.jpg') || lower.endsWith('.jpeg'))) {
            this.snackBar.open('Please upload a PDF or JPG certificate.', 'OK', { duration: 5000 });
            this.selectedCertificate = null;
            this.placementForm.get('certificate')?.setValue(null);
            return;
        }
        this.selectedCertificate = file;
        this.placementForm.get('certificate')?.setValue(file.name);
    }

    onSubmit(): void {
        if (this.placementForm.invalid) {
            this.placementForm.markAllAsTouched();
            return;
        }

        // If a certificate is provided, call backend to analyze content, with fallback on filename.
        if (this.selectedCertificate) {
            this.placementForm.disable();
            this.placementService.uploadCertificate(this.selectedCertificate).subscribe({
                next: (analysis) => {
                    this.analysisResult = analysis;
                    const detectedLevel = analysis.detectedLevel || this.detectLevelFromFileName(this.selectedCertificate!.name);
                    const baseParams: any = {};
                    if (detectedLevel) {
                        baseParams.level = detectedLevel;
                    }
                    if (analysis.recommendedPlanId) {
                        baseParams.planId = analysis.recommendedPlanId;
                    }
                    // If backend gave at least a level or plan, use it; otherwise, fall back to filename only.
                    if (Object.keys(baseParams).length > 0) {
                        this.router.navigate(['/frontoffice/inscription/offers'], { queryParams: baseParams });
                    } else {
                        const inferred = this.detectLevelFromFileName(this.selectedCertificate!.name);
                        if (inferred) {
                            this.router.navigate(['/frontoffice/inscription/offers'], {
                                queryParams: { level: inferred }
                            });
                        } else {
                            this.router.navigate(['/frontoffice/inscription/offers']);
                        }
                    }
                },
                error: () => {
                    // If upload fails, fall back to filename-only inference without showing an error snackbar.
                    const inferredLevel = this.detectLevelFromFileName(this.selectedCertificate!.name);
                    if (inferredLevel) {
                        this.subscriptionPlanService.getRecommendation({ level: inferredLevel }).subscribe({
                            next: (rec) => {
                                const queryParams: any = { level: inferredLevel };
                                if (rec.recommendedPlanId) {
                                    queryParams.planId = rec.recommendedPlanId;
                                }
                                this.router.navigate(['/frontoffice/inscription/offers'], { queryParams });
                            },
                            error: () => {
                                this.router.navigate(['/frontoffice/inscription/offers'], {
                                    queryParams: { level: inferredLevel }
                                });
                            }
                        });
                    } else {
                        this.router.navigate(['/frontoffice/inscription/offers']);
                    }
                }
            }).add(() => {
                this.placementForm.enable();
            });
            return;
        }

        // No certificate: just go to offers, user will pick a plan manually.
        this.router.navigate(['/frontoffice/inscription/offers']);
    }

    private detectLevelFromFileName(fileName: string): string | null {
        const upper = fileName.toUpperCase();
        if (upper.includes('C1')) return 'C1';
        if (upper.includes('B2')) return 'B2';
        if (upper.includes('B1')) return 'B1';
        if (upper.includes('A2')) return 'A2';
        if (upper.includes('A1')) return 'A1';
        return null;
    }
}
