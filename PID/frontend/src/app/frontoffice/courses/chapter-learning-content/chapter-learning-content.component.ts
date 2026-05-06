import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ChapterLearningResponse,
  ChapterLearningService,
  FlashcardItem
} from './chapter-learning.service';

@Component({
  selector: 'app-chapter-learning-content',
  templateUrl: './chapter-learning-content.component.html',
  styleUrls: ['./chapter-learning-content.component.css']
})
export class ChapterLearningContentComponent implements OnInit, OnChanges {
  @Input({ required: true }) chapterId!: number;

  content: ChapterLearningResponse | null = null;
  loadingCache = true;
  generating = false;
  error: string | null = null;
  flipped: Record<number, boolean> = {};

  constructor(private readonly learningApi: ChapterLearningService) {}

  ngOnInit(): void {
    this.loadCached();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['chapterId'] && !changes['chapterId'].firstChange) {
      this.content = null;
      this.flipped = {};
      this.error = null;
      this.loadCached();
    }
  }

  loadCached(): void {
    this.loadingCache = true;
    this.error = null;
    this.learningApi.getCached(this.chapterId).subscribe({
      next: (data) => {
        this.content = data;
      },
      error: () => {
        this.error = 'Impossible de charger le contenu pedagogique.';
        this.content = null;
      },
      complete: () => {
        this.loadingCache = false;
      }
    });
  }

  generate(): void {
    this.runGenerate(false);
  }

  regenerate(): void {
    this.runGenerate(true);
  }

  private runGenerate(regenerate: boolean): void {
    this.generating = true;
    this.error = null;
    this.learningApi.generateContent(this.chapterId, regenerate).subscribe({
      next: (data) => {
        this.content = data;
        this.flipped = {};
      },
      error: (err) => {
        const msg =
          err?.error?.message ??
          err?.error?.error ??
          (typeof err?.error === 'string' ? err.error : null) ??
          'La generation a echoue. Reessayez plus tard.';
        this.error = msg;
      },
      complete: () => {
        this.generating = false;
      }
    });
  }

  toggleCard(index: number): void {
    this.flipped[index] = !this.flipped[index];
  }

  isFlipped(index: number): boolean {
    return !!this.flipped[index];
  }

  trackByIndex(i: number, _item: FlashcardItem): number {
    return i;
  }

  cardPaletteClass(index: number): string {
    return `clc-palette-${index % 6}`;
  }
}

