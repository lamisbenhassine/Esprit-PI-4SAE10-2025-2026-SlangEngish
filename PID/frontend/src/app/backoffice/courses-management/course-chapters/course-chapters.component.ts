import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { Chapter } from '../../../models/chapter.model';
import { ChapterService } from '../chapter.service';

@Component({
  selector: 'app-course-chapters',
  templateUrl: './course-chapters.component.html',
  styleUrls: ['./course-chapters.component.css']
})
export class CourseChaptersComponent implements OnChanges {
  @Input() courseId!: number;
  @Input() courseName = '';
  @Output() close = new EventEmitter<void>();

  chapters: Chapter[] = [];
  formChapter: Chapter | null = null;
  isCreating = false;
  isEditing = false;
  selectedPdfName: string | null = null;
  // Limite côté front (~15MB) pour rester cohérent avec la limite backend
  private readonly maxPdfBytes = 15_000_000;

  constructor(private chapterService: ChapterService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['courseId'] && this.courseId) {
      this.loadChapters();
    }
  }

  loadChapters(): void {
    if (!this.courseId) {
      return;
    }
    this.chapterService.getByCourse(this.courseId).subscribe({
      next: (data) => (this.chapters = data),
      error: (err) => console.error('Erreur chargement chapitres', err)
    });
  }

  startCreate(): void {
    this.isCreating = true;
    this.isEditing = false;
    this.formChapter = {
      name: '',
      description: '',
      pdfSupport: null,
      videoSupport: null
    };
  }

  startEdit(chapter: Chapter): void {
    this.isEditing = true;
    this.isCreating = false;
    this.formChapter = { ...chapter };
  }

  saveChapter(): void {
    if (!this.formChapter) {
      return;
    }

    if (this.isCreating) {
      this.chapterService.addChapter(this.courseId, this.formChapter).subscribe({
        next: () => {
          this.loadChapters();
          this.cancelForm();
        },
        error: (err) => console.error('Erreur ajout chapitre', err)
      });
      return;
    }

    if (this.isEditing && this.formChapter.idChapter) {
      this.chapterService.updateChapter(this.formChapter.idChapter, this.formChapter).subscribe({
        next: () => {
          this.loadChapters();
          this.cancelForm();
        },
        error: (err) => console.error('Erreur mise à jour chapitre', err)
      });
    }
  }

  deleteChapter(chapter: Chapter): void {
    if (!chapter.idChapter) {
      return;
    }
    if (!confirm('Supprimer ce chapitre ?')) {
      return;
    }
    this.chapterService.deleteChapter(chapter.idChapter).subscribe({
      next: () => (this.chapters = this.chapters.filter((c) => c.idChapter !== chapter.idChapter)),
      error: (err) => console.error('Erreur suppression chapitre', err)
    });
  }

  cancelForm(): void {
    this.isCreating = false;
    this.isEditing = false;
    this.formChapter = null;
    this.selectedPdfName = null;
  }

  closePanel(): void {
    this.close.emit();
  }

  onPdfSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.formChapter) {
      return;
    }
    if (file.size > this.maxPdfBytes) {
      alert('PDF trop grand. Choisis un PDF <= 15MB.');
      input.value = '';
      this.selectedPdfName = null;
      this.formChapter.pdfSupport = null;
      return;
    }
    this.selectedPdfName = file.name;
    const reader = new FileReader();
    reader.onload = () => {
      if (this.formChapter) {
        this.formChapter.pdfSupport = reader.result as string;
      }
    };
    reader.readAsDataURL(file);
  }

  openPdf(chapter: Chapter): void {
    if (!chapter.pdfSupport) {
      return;
    }

    const support = chapter.pdfSupport;
    if (support.startsWith('data:application/pdf')) {
      const blob = this.dataUrlToBlob(support);
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener,noreferrer');
      // laisser le navigateur gérer la durée de vie du blob URL
      return;
    }

    window.open(support, '_blank', 'noopener,noreferrer');
  }

  private dataUrlToBlob(dataUrl: string): Blob {
    const commaIndex = dataUrl.indexOf(',');
    const header = dataUrl.substring(0, commaIndex);
    const base64 = dataUrl.substring(commaIndex + 1);
    const mimeMatch = header.match(/data:(.*?);base64/);
    const mime = mimeMatch?.[1] ?? 'application/octet-stream';

    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return new Blob([bytes], { type: mime });
  }
}

