import {
  Component,
  Inject,
  Input,
  Injector,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  SimpleChanges,
  afterNextRender,
  inject
} from '@angular/core';
import { animate, style, transition, trigger } from '@angular/animations';
import { isPlatformBrowser } from '@angular/common';
import {
  ChapterPresentationDto,
  ChapterPresentationService,
  PresentationSlide
} from './chapter-presentation.service';

@Component({
  selector: 'app-presentation-player',
  templateUrl: './presentation-player.component.html',
  styleUrls: ['./presentation-player.component.css'],
  animations: [
    trigger('fadeSlide', [
      transition('* => *', [
        style({ opacity: 0, transform: 'translateY(12px)' }),
        animate('420ms ease-out', style({ opacity: 1, transform: 'none' }))
      ])
    ])
  ]
})
export class PresentationPlayerComponent implements OnChanges, OnDestroy, OnInit {
  @Input({ required: true }) chapterId!: number;

  private readonly injector = inject(Injector);

  readonly isBrowser: boolean;

  loadingCache = true;
  generating = false;
  error: string | null = null;

  presentation: ChapterPresentationDto | null = null;

  currentSlideIndex = 0;
  /** 0–1 progression globale */
  progress = 0;
  playing = false;

  private noAudioElapsedMs = 0;
  private noAudioTimer: ReturnType<typeof setInterval> | undefined;

  constructor(
    private readonly presentationApi: ChapterPresentationService,
    private readonly ngZone: NgZone,
    @Inject(PLATFORM_ID) platformId: object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    afterNextRender(
      () => {
        this.loadCached();
      },
      { injector: this.injector }
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['chapterId'] && !changes['chapterId'].firstChange) {
      this.resetUi();
      this.loadCached();
    }
  }

  ngOnDestroy(): void {
    this.clearNoAudioTimer();
  }

  private resetUi(): void {
    this.presentation = null;
    this.error = null;
    this.currentSlideIndex = 0;
    this.progress = 0;
    this.playing = false;
    this.clearNoAudioTimer();
  }

  loadCached(): void {
    this.loadingCache = true;
    this.error = null;
    this.presentationApi.getPresentation(this.chapterId).subscribe({
      next: (dto) => {
        this.presentation = dto;
        this.currentSlideIndex = 0;
        this.progress = 0;
      },
      error: () => {
        this.error = 'Impossible de charger la présentation.';
        this.presentation = null;
      },
      complete: () => {
        this.loadingCache = false;
      }
    });
  }

  get currentSlide(): PresentationSlide | null {
    const slides = this.presentation?.slides;
    if (!slides?.length) {
      return null;
    }
    return slides[Math.min(this.currentSlideIndex, slides.length - 1)] ?? null;
  }

  get slideCount(): number {
    return this.presentation?.slides?.length ?? 0;
  }

  onFilePicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      this.uploadAndGenerate(file, false);
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0];
    if (file && file.name.toLowerCase().endsWith('.pdf')) {
      this.uploadAndGenerate(file, false);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  regenerate(): void {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'application/pdf,.pdf';
    input.onchange = () => {
      const file = input.files?.[0];
      if (file) {
        this.uploadAndGenerate(file, true);
      }
    };
    input.click();
  }

  private uploadAndGenerate(file: File, regenerate: boolean): void {
    this.generating = true;
    this.error = null;
    this.presentationApi.generatePresentation(this.chapterId, file, regenerate).subscribe({
      next: (dto) => {
        this.presentation = dto;
        this.currentSlideIndex = 0;
        this.progress = 0;
        this.playing = false;
        this.clearNoAudioTimer();
      },
      error: (err) => {
        const msg =
          err?.error?.message ??
          err?.error?.error ??
          (typeof err?.error === 'string' ? err.error : null) ??
          'La génération a échoué.';
        this.error = msg;
      },
      complete: () => {
        this.generating = false;
      }
    });
  }

  startPresentation(): void {
    if (!this.presentation?.slides?.length) {
      return;
    }
    this.playing = true;
    this.currentSlideIndex = 0;
    this.progress = 0;
    this.clearNoAudioTimer();
    this.startWithoutAudio();
  }

  private startWithoutAudio(): void {
    const n = this.slideCount;
    if (n <= 0) {
      return;
    }
    const slideMs = 8000;
    const totalMs = n * slideMs;
    this.noAudioElapsedMs = 0;
    this.clearNoAudioTimer();

    this.ngZone.runOutsideAngular(() => {
      this.noAudioTimer = setInterval(() => {
        this.ngZone.run(() => {
          this.noAudioElapsedMs += 250;
          this.progress = Math.min(1, this.noAudioElapsedMs / totalMs);
          this.currentSlideIndex = Math.min(n - 1, Math.floor(this.progress * n));
          if (this.noAudioElapsedMs >= totalMs) {
            this.clearNoAudioTimer();
            this.playing = false;
            this.progress = 1;
            this.currentSlideIndex = n - 1;
          }
        });
      }, 250);
    });
  }

  pausePresentation(): void {
    this.playing = false;
    this.clearNoAudioTimer();
  }

  private clearNoAudioTimer(): void {
    if (this.noAudioTimer != null) {
      clearInterval(this.noAudioTimer);
      this.noAudioTimer = undefined;
    }
  }

  prevSlide(): void {
    if (this.currentSlideIndex > 0) {
      this.currentSlideIndex--;
      this.seekToSlideFraction();
    }
  }

  nextSlide(): void {
    if (this.currentSlideIndex < this.slideCount - 1) {
      this.currentSlideIndex++;
      this.seekToSlideFraction();
    }
  }

  /** Recale la position audio (ou le timer sans audio) sur le début de la slide courante. */
  private seekToSlideFraction(): void {
    const n = this.slideCount;
    if (n <= 0) {
      return;
    }
    const t = this.currentSlideIndex / n;
    if (this.noAudioTimer != null && this.playing) {
      const slideMs = 8000;
      const totalMs = n * slideMs;
      this.noAudioElapsedMs = Math.round(t * totalMs);
      this.progress = t;
    } else {
      this.progress = (this.currentSlideIndex + 0.001) / n;
    }
  }
}
