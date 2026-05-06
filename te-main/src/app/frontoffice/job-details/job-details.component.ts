import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { JobOffer } from '../../models/job-offer.model';
import { JobOfferService } from '../../services/job-offer.service';
import { ApplicationService } from '../../services/application.service';
import { ToastService } from '../../services/toast.service';
import { SimilarityService, SimilarityResult }
    from '../../services/similarity.service';
import { SalaryService, SalaryPrediction }
    from '../../services/salary.service';
import { forkJoin, Subscription } from 'rxjs';

export interface QuizQuestion {
  number: number;
  question: string;
  choices: string[];
  correctAnswer: string;
  explanation: string;
  category: string;
}

export interface QuizAnswer {
  questionNumber: number;
  question: string;
  selectedAnswer: string;
  correctAnswer: string;
}

export interface QuizResult {
  cvScore: number;
  quizScore: number;
  globalScore: number;
  globalLevel: string;
  correctAnswers: number;
  totalQuestions: number;
  questionResults: any[];
  cvMatchedSkills: string[];
  cvMissingSkills: string[];
  qualified: boolean;
  message: string;
  scheduledInterview: string;
  tips: string[];
}

@Component({
  selector: 'app-job-details',
  templateUrl: './job-details.component.html',
  styleUrls: ['./job-details.component.css']
})
export class JobDetailsComponent implements OnInit, OnDestroy {

  jobOffer?: JobOffer;
  loading = false;
  applicationForm!: FormGroup;
  submitting = false;
  applicationSuccess = false;
  applicationError = '';

  similarOffers: SimilarityResult[] = [];
  loadingSimilar = false;

  cvFile: File | null = null;
  coverLetterFile: File | null = null;

  // ✅ Quiz State
  showQuizChoice = false;
  showQuiz = false;
  showQuizResult = false;
  loadingQuiz = false;
  submittingQuiz = false;
  quizQuestions: QuizQuestion[] = [];
  quizAnswers: QuizAnswer[] = [];
  quizResult: QuizResult | null = null;
  currentQuestion = 0;
  cvUrlForQuiz = '';
  applicantEmailForQuiz = '';

  // ✅ Salary Prediction
  salaryPrediction: SalaryPrediction | null = null;
  loadingSalary = false;
  showSalaryDetails = false;

  // ✅ Subscription pour éviter les memory leaks
  private routeSub!: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobOfferService: JobOfferService,
    private applicationService: ApplicationService,
    private fb: FormBuilder,
    private toast: ToastService,
    private similarityService: SimilarityService,
    private http: HttpClient,
    private salaryService: SalaryService
  ) {}

  ngOnInit(): void {
    this.initApplicationForm();

    // ✅ Écoute les changements de route en temps réel
    this.routeSub = this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        // ✅ Reset complet à chaque changement d'offre
        this.resetState();
        this.loadJobOffer(+id);
        this.loadSimilarOffers(+id);
      }
    });
  }

  ngOnDestroy(): void {
    // ✅ Évite les memory leaks
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
  }

  // ✅ Reset tout l'état
  private resetState(): void {
    this.jobOffer = undefined;
    this.salaryPrediction = null;
    this.loadingSalary = false;
    this.showSalaryDetails = false;
    this.similarOffers = [];
    this.applicationSuccess = false;
    this.applicationError = '';
    this.showQuizChoice = false;
    this.showQuiz = false;
    this.showQuizResult = false;
    this.quizResult = null;
    this.cvFile = null;
    this.coverLetterFile = null;
    this.initApplicationForm();
  }

  initApplicationForm(): void {
    this.applicationForm = this.fb.group({
      applicantName: ['', [Validators.required,
          Validators.minLength(3)]],
      applicantEmail: ['', [Validators.required,
          Validators.email]]
    });
  }

  // ─── Load Job Offer ───────────────────────────────────────────────

  loadJobOffer(id: number): void {
    this.loading = true;
    this.jobOfferService.findById(id).subscribe({
      next: (data: JobOffer) => {
        this.jobOffer = data;
        this.loading = false;
        this.jobOfferService.incrementView(id).subscribe();

        // ✅ Charge la prédiction avec le TITRE de l'offre
        console.log('🎯 Loading salary for:', data.title);
        this.loadSalaryPredictionByTitle(data.title);
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/frontoffice/job-offers']);
      }
    });
  }

  loadSimilarOffers(id: number): void {
    this.loadingSimilar = true;
    this.similarityService.getSimilarOffers(id).subscribe({
      next: (data) => {
        this.similarOffers = data;
        this.loadingSimilar = false;
      },
      error: () => { this.loadingSimilar = false; }
    });
  }

  goToSimilarOffer(offerId: number): void {
    this.router.navigate(
        ['/frontoffice/job-details', offerId]);
    window.scrollTo(0, 0);
  }

  // ─── Salary Prediction ────────────────────────────────────────────

  // ✅ Prédit directement par titre → plus précis !
  loadSalaryPredictionByTitle(jobTitle: string): void {
    this.loadingSalary = true;
    this.salaryPrediction = null;

    // ✅ Désactive le cache HTTP
    const headers = new HttpHeaders({
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache'
    });

    this.http.get<SalaryPrediction>(
      `/api/salary/predict?jobTitle=${encodeURIComponent(jobTitle)}`,
      { headers }
    ).subscribe({
      next: (data) => {
        console.log('💰 Salary received:', data);
        this.salaryPrediction = data;
        this.loadingSalary = false;
      },
      error: () => {
        this.loadingSalary = false;
      }
    });
  }

  toggleSalaryDetails(): void {
    this.showSalaryDetails = !this.showSalaryDetails;
  }

  getSalaryLevel(salary: number): string {
    if (salary >= 4000) return 'Senior';
    if (salary >= 2500) return 'Mid-Level';
    if (salary >= 1500) return 'Junior';
    return 'Entry';
  }

  getSalaryColor(salary: number): string {
    if (salary >= 4000) return '#16a34a';
    if (salary >= 2500) return '#7c3aed';
    if (salary >= 1500) return '#ea580c';
    return '#94a3b8';
  }

  getSalaryBg(salary: number): string {
    if (salary >= 4000) return '#f0fdf4';
    if (salary >= 2500) return '#f3e8ff';
    if (salary >= 1500) return '#fff7ed';
    return '#f8fafc';
  }

  getConfidenceColor(confidence: number): string {
    if (confidence >= 80) return '#16a34a';
    if (confidence >= 60) return '#ea580c';
    return '#94a3b8';
  }

  // ─── Submit Application ───────────────────────────────────────────

  submitApplication(): void {
    if (this.applicationForm.invalid) {
      this.applicationForm.markAllAsTouched();
      return;
    }
    if (!this.cvFile || !this.coverLetterFile) {
      this.applicationError =
          'Please upload your CV and cover letter.';
      this.toast.error(
          'Please upload your CV and cover letter.');
      return;
    }

    this.submitting = true;
    this.applicationError = '';

    forkJoin({
      cvUrl: this.applicationService.uploadFile(this.cvFile),
      coverLetterUrl: this.applicationService
          .uploadFile(this.coverLetterFile)
    }).subscribe({
      next: ({ cvUrl, coverLetterUrl }) => {
        const application: any = {
          ...this.applicationForm.value,
          cvUrl,
          coverLetterUrl
        };

        this.applicationService.applyToOffer(
            this.jobOffer!.id!, application).subscribe({
          next: () => {
            this.submitting = false;
            this.toast.success('Application sent successfully!');
            this.cvUrlForQuiz = cvUrl;
            this.applicantEmailForQuiz =
                this.applicationForm.value.applicantEmail;
            this.showQuizChoice = true;
          },
          error: () => {
            this.applicationError =
                'Error submitting application.';
            this.submitting = false;
            this.toast.error('Error submitting application.');
          }
        });
      },
      error: () => {
        this.applicationError = 'Error uploading files.';
        this.submitting = false;
        this.toast.error('Error uploading files.');
      }
    });
  }

  // ─── Quiz Flow ────────────────────────────────────────────────────

  startQuiz(): void {
    this.showQuizChoice = false;
    this.loadingQuiz = true;

    const url = `/api/quiz/start/${this.jobOffer!.id}`
        + `?cvUrl=${encodeURIComponent(this.cvUrlForQuiz)}`
        + `&applicantEmail=${encodeURIComponent(
            this.applicantEmailForQuiz)}`;

    this.http.get<any>(url).subscribe({
      next: (data) => {
        this.quizQuestions = data.questions;
        this.quizAnswers = this.quizQuestions.map(q => ({
          questionNumber: q.number,
          question: q.question,
          selectedAnswer: '',
          correctAnswer: q.correctAnswer
        }));
        this.currentQuestion = 0;
        this.loadingQuiz = false;
        this.showQuiz = true;
      },
      error: () => {
        this.loadingQuiz = false;
        this.toast.error('Error loading quiz.');
        this.applicationSuccess = true;
      }
    });
  }

  skipQuiz(): void {
    this.showQuizChoice = false;
    this.applicationSuccess = true;
    this.toast.success(
        '✅ Application submitted! Status: Pending review.');
  }

  selectAnswer(letter: string): void {
    if (this.quizAnswers[this.currentQuestion]) {
      this.quizAnswers[this.currentQuestion]
          .selectedAnswer = letter;
    }
  }

  nextQuestion(): void {
    if (this.currentQuestion < this.quizQuestions.length - 1) {
      this.currentQuestion++;
    }
  }

  prevQuestion(): void {
    if (this.currentQuestion > 0) {
      this.currentQuestion--;
    }
  }

  submitQuiz(): void {
    this.submittingQuiz = true;
    this.showQuiz = false;

    const request = {
      jobOfferId: this.jobOffer!.id,
      cvUrl: this.cvUrlForQuiz,
      applicantName: this.applicationForm.value.applicantName,
      applicantEmail: this.applicantEmailForQuiz,
      answers: this.quizAnswers
    };

    this.http.post<QuizResult>(
        '/api/quiz/evaluate', request).subscribe({
      next: (result) => {
        this.quizResult = result;
        this.submittingQuiz = false;
        this.showQuizResult = true;
      },
      error: () => {
        this.submittingQuiz = false;
        this.applicationSuccess = true;
        this.toast.error('Error evaluating quiz.');
      }
    });
  }

  closeQuizResult(): void {
    this.showQuizResult = false;
    this.applicationSuccess = true;
  }

  // ─── Helpers ──────────────────────────────────────────────────────

  getSelectedAnswer(questionIndex: number): string {
    return this.quizAnswers[questionIndex]
        ?.selectedAnswer || '';
  }

  isAnswered(questionIndex: number): boolean {
    return !!this.quizAnswers[questionIndex]?.selectedAnswer;
  }

  get allAnswered(): boolean {
    return this.quizAnswers.every(a => a.selectedAnswer !== '');
  }

  get answeredCount(): number {
    return this.quizAnswers.filter(
        a => a.selectedAnswer !== '').length;
  }

  getScoreColor(score: number): string {
    if (score >= 75) return '#166534';
    if (score >= 60) return '#92400e';
    return '#991b1b';
  }

  getScoreBg(score: number): string {
    if (score >= 75) return '#dcfce7';
    if (score >= 60) return '#fef3c7';
    return '#fee2e2';
  }

  getSimilarityColor(percent: number): string {
    if (percent >= 70) return '#4CAF50';
    if (percent >= 40) return '#FF9800';
    return '#F44336';
  }

  getSimilarityBg(percent: number): string {
    if (percent >= 70) return '#E8F5E9';
    if (percent >= 40) return '#FFF3E0';
    return '#FFEBEE';
  }

  getSimilarityLabel(percent: number): string {
    if (percent >= 70) return 'Very similar';
    if (percent >= 40) return 'Similar';
    return 'Slightly similar';
  }

  getContractColor(type: string): string {
    switch (type) {
      case 'CDI':        return '#166534';
      case 'CDD':        return '#1e40af';
      case 'STAGE':      return '#9a3412';
      case 'ALTERNANCE': return '#6b21a8';
      case 'FREELANCE':  return '#991b1b';
      default:           return '#475569';
    }
  }

  getContractBg(type: string): string {
    switch (type) {
      case 'CDI':        return '#dcfce7';
      case 'CDD':        return '#dbeafe';
      case 'STAGE':      return '#ffedd5';
      case 'ALTERNANCE': return '#f3e8ff';
      case 'FREELANCE':  return '#fee2e2';
      default:           return '#f1f5f9';
    }
  }

  onCvSelected(event: any): void {
    this.cvFile = event.target.files[0] || null;
  }

  onCoverLetterSelected(event: any): void {
    this.coverLetterFile = event.target.files[0] || null;
  }

  goBack(): void {
    this.router.navigate(['/frontoffice/job-offers']);
  }

  isFieldInvalid(fieldName: string): boolean {
    const control = this.applicationForm.get(fieldName);
    return !!(control && control.invalid
        && (control.dirty || control.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const control = this.applicationForm.get(fieldName);
    return !!(control && control.valid
        && (control.dirty || control.touched));
  }
}