import { ChangeDetectorRef, Component, Inject, OnInit, PLATFORM_ID, Type } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ChapterService } from '../../../backoffice/courses-management/chapter.service';
import { UserContextService } from '../../../services/user-context.service';
import { Chapter } from '../../../models/chapter.model';

@Component({
  selector: 'app-pdf-chapter-viewer',
  templateUrl: './pdf-chapter-viewer.component.html',
  styleUrls: ['./pdf-chapter-viewer.component.css']
})
export class PdfChapterViewerComponent implements OnInit {
  pdfSrc: string | null = null;
  chapter: Chapter | null = null;
  chapterId!: number;
  courseId!: number;
  userId!: number;
  loading = true;
  error: string | null = null;

  /** Composant PDF (ng2-pdf-viewer) — chargé uniquement côté navigateur. */
  pdfEmbedComponentType: Type<unknown> | null = null;
  pdfEmbedInputs: Record<string, unknown> = {};

  constructor(
    private route: ActivatedRoute,
    private chapterService: ChapterService,
    private userContext: UserContextService,
    @Inject(PLATFORM_ID) private platformId: object,
    private cdr: ChangeDetectorRef
  ) {
    if (isPlatformBrowser(this.platformId)) {
      void import('../pdf-chapter-embed/pdf-chapter-embed.component').then((m) => {
        this.pdfEmbedComponentType = m.PdfChapterEmbedComponent;
        this.syncPdfEmbedInputs();
        this.cdr.markForCheck();
      });
    }
  }

  get isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
    this.chapterId = Number(this.route.snapshot.paramMap.get('chapterId'));
    this.courseId = Number(this.route.snapshot.paramMap.get('courseId'));
    this.userId = this.userContext.getCurrentUserId();
    if (this.isBrowser) {
      localStorage.setItem('lastChapterId', String(this.chapterId));
      localStorage.setItem('lastCourseId', String(this.courseId));
    }

    this.chapterService.getById(this.chapterId).subscribe({
      next: (ch: Chapter) => {
        this.chapter = ch;
        if (ch.pdfSupport) {
          if (ch.pdfSupport.startsWith('data:application/pdf')) {
            this.pdfSrc = ch.pdfSupport;
          } else {
            this.pdfSrc = `data:application/pdf;base64,${ch.pdfSupport}`;
          }
        } else {
          this.error = 'Aucun support PDF pour ce chapitre.';
        }
        this.loading = false;
        this.syncPdfEmbedInputs();
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Impossible de charger le chapitre.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private syncPdfEmbedInputs(): void {
    if (this.pdfSrc) {
      this.pdfEmbedInputs = {
        pdfSrc: this.pdfSrc,
        chapterId: this.chapterId,
        userId: this.userId
      };
    }
  }
}
