import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PdfViewerModule } from 'ng2-pdf-viewer';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ProgressionService } from '../../../backoffice/courses-management/progression.service';

/**
 * Composant isolé pour ng2-pdf-viewer : chargé uniquement dans le navigateur
 * (import dynamique depuis PdfChapterViewerComponent) afin d'éviter l'accès à
 * `document` pendant le SSR / le pré-rendu.
 */
@Component({
  selector: 'app-pdf-chapter-embed',
  standalone: true,
  imports: [CommonModule, PdfViewerModule, MatProgressBarModule],
  templateUrl: './pdf-chapter-embed.component.html',
  styleUrls: ['./pdf-chapter-embed.component.css']
})
export class PdfChapterEmbedComponent implements OnInit {
  @Input({ required: true }) pdfSrc!: string;
  @Input({ required: true }) chapterId!: number;
  @Input({ required: true }) userId!: number;

  totalPages = 0;
  currentPage = 1;
  progressPercentage = 0;

  constructor(private progressionService: ProgressionService) {}

  ngOnInit(): void {
    this.loadSavedProgress();
  }

  private loadSavedProgress(): void {
    this.progressionService.getChapterProgress(this.userId, this.chapterId).subscribe({
      next: (p: { lastPage?: number; progressPercentage?: number } | null) => {
        if (p?.lastPage != null) {
          this.currentPage = p.lastPage;
          if (p.progressPercentage != null) {
            this.progressPercentage = p.progressPercentage;
          }
        }
      }
    });
  }

  onPdfLoaded(pdf: { numPages: number }): void {
    this.totalPages = pdf.numPages;
    this.updateProgressAndPersist();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.updateProgressAndPersist();
  }

  private updateProgressAndPersist(): void {
    if (!this.totalPages) return;
    this.progressPercentage = Math.round((this.currentPage / this.totalPages) * 10000) / 100;
    this.progressionService
      .updateChapterProgress({
        userId: this.userId,
        chapterId: this.chapterId,
        lastPage: this.currentPage,
        progressPercentage: this.progressPercentage
      })
      .subscribe();
  }
}
