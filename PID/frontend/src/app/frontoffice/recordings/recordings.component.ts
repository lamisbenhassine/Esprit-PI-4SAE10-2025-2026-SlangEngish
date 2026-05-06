import { isPlatformBrowser } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { Subject, takeUntil, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { Recording, RecordingAnalysisStatus, RecordingHighlight } from '../../models/recording.model';
import { RecordingService } from './recording.service';

@Component({
  selector: 'app-recordings',
  templateUrl: './recordings.component.html',
  styleUrls: ['./recordings.component.css']
})
export class RecordingsComponent implements OnInit, OnDestroy {

  recordings: Recording[] = [];
  loading = false;
  error: string | null = null;
  activeHighlightByRecording: Record<number, number> = {};
  highlightsByRecording: Record<number, RecordingHighlight[]> = {};
  analysisStatusByRecording: Record<number, RecordingAnalysisStatus> = {};
  regeneratingByRecording: Record<number, boolean> = {};
  private readonly pollingStopByRecording: Record<number, Subject<void>> = {};

  private readonly destroy$ = new Subject<void>();

  constructor(
    private recordingService: RecordingService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit(): void {
    this.loadRecordings();
  }

  ngOnDestroy(): void {
    Object.values(this.pollingStopByRecording).forEach((stop$) => {
      stop$.next();
      stop$.complete();
    });
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadRecordings(): void {
    this.loading = true;
    this.error = null;

    this.recordingService.getAvailable().subscribe({
      next: (data) => {
        this.recordings = data;
        this.initializeAnalysisForRecordings();
      },
      error: (err) => {
        console.error('Erreur chargement recordings', err);
        this.error = 'Impossible de charger les enregistrements pour le moment.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  seekTo(recordingId: number, seconds: number): void {
    const player = document.getElementById(`recording-video-${recordingId}`) as HTMLVideoElement | null;
    if (!player) {
      return;
    }
    player.currentTime = seconds;
    player.play().catch(() => undefined);
  }

  onVideoTimeUpdate(recordingId: number, currentTime: number): void {
    const highlights = this.highlightsByRecording[recordingId] ?? [];
    if (highlights.length === 0) {
      this.activeHighlightByRecording[recordingId] = -1;
      return;
    }

    let activeIndex = -1;
    for (let i = 0; i < highlights.length; i++) {
      const current = highlights[i];
      const next = highlights[i + 1];
      if (currentTime >= current.seconds && (!next || currentTime < next.seconds)) {
        activeIndex = i;
        break;
      }
    }
    const previousActive = this.activeHighlightByRecording[recordingId];
    this.activeHighlightByRecording[recordingId] = activeIndex;
    if (activeIndex !== previousActive) {
      this.scrollActiveHighlightIntoView(recordingId, activeIndex);
    }
  }

  formatTimestamp(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`;
  }

  regenerateHighlights(recordingId: number): void {
    this.stopPolling(recordingId);
    this.regeneratingByRecording[recordingId] = true;
    this.highlightsByRecording[recordingId] = [];
    this.activeHighlightByRecording[recordingId] = -1;
    this.analysisStatusByRecording[recordingId] = RecordingAnalysisStatus.PROCESSING;

    this.recordingService.analyzeRecording(recordingId).subscribe({
      next: () => {
        this.regeneratingByRecording[recordingId] = false;
        this.startPollingStatus(recordingId);
      },
      error: (err) => {
        console.error('Erreur relance highlights', err);
        this.regeneratingByRecording[recordingId] = false;
        this.analysisStatusByRecording[recordingId] = RecordingAnalysisStatus.FAILED;
      }
    });
  }

  isProfessor(): boolean {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }
    const role = (localStorage.getItem('role') || localStorage.getItem('userRole') || '').toUpperCase();
    return role.includes('PROF');
  }

  private initializeAnalysisForRecordings(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.recordings.forEach((recording) => {
      if (!recording.id) {
        return;
      }
      this.activeHighlightByRecording[recording.id] = -1;
      const status = this.normalizeStatus(recording.analysisStatus);
      this.analysisStatusByRecording[recording.id] = status;

      if (status === RecordingAnalysisStatus.COMPLETED) {
        this.loadHighlights(recording.id);
      } else {
        this.startPollingStatus(recording.id);
      }
    });
  }

  private loadHighlights(recordingId: number): void {
    this.recordingService.getHighlights(recordingId).subscribe({
      next: (items) => {
        this.highlightsByRecording[recordingId] = items;
      },
      error: (err) => {
        console.error(`Erreur highlights recording ${recordingId}`, err);
      }
    });
  }

  private startPollingStatus(recordingId: number): void {
    if (this.pollingStopByRecording[recordingId]) {
      return;
    }

    const stop$ = new Subject<void>();
    this.pollingStopByRecording[recordingId] = stop$;

    timer(0, 5000).pipe(
      takeUntil(stop$),
      takeUntil(this.destroy$),
      switchMap(() => this.recordingService.getAnalysisStatus(recordingId))
    ).subscribe({
      next: (response) => {
        const normalizedStatus = this.normalizeStatus(response.status);
        this.analysisStatusByRecording[recordingId] = normalizedStatus;
        if (normalizedStatus === RecordingAnalysisStatus.COMPLETED) {
          this.loadHighlights(recordingId);
          this.stopPolling(recordingId);
        }
        if (normalizedStatus === RecordingAnalysisStatus.FAILED) {
          this.stopPolling(recordingId);
        }
      },
      error: (err) => {
        console.error(`Erreur polling status recording ${recordingId}`, err);
      }
    });
  }

  private stopPolling(recordingId: number): void {
    const stop$ = this.pollingStopByRecording[recordingId];
    if (!stop$) {
      return;
    }
    stop$.next();
    stop$.complete();
    delete this.pollingStopByRecording[recordingId];
  }

  private scrollActiveHighlightIntoView(recordingId: number, activeIndex: number): void {
    if (activeIndex < 0) {
      return;
    }

    const panel = document.getElementById(`highlights-panel-${recordingId}`);
    const element = document.querySelector(
      `#highlights-panel-${recordingId} [data-highlight-index="${activeIndex}"]`
    );

    if (!(panel instanceof HTMLElement) || !(element instanceof HTMLElement)) {
      return;
    }

    const panelTop = panel.scrollTop;
    const panelBottom = panelTop + panel.clientHeight;
    const itemTop = element.offsetTop;
    const itemBottom = itemTop + element.offsetHeight;

    if (itemTop < panelTop) {
      panel.scrollTo({ top: itemTop - 8, behavior: 'smooth' });
      return;
    }

    if (itemBottom > panelBottom) {
      panel.scrollTo({ top: itemBottom - panel.clientHeight + 8, behavior: 'smooth' });
    }
  }

  private normalizeStatus(status: RecordingAnalysisStatus | string | null | undefined): RecordingAnalysisStatus {
    const normalized = String(status ?? '')
      .trim()
      .toUpperCase();
    if (normalized === RecordingAnalysisStatus.PROCESSING) {
      return RecordingAnalysisStatus.PROCESSING;
    }
    if (normalized === RecordingAnalysisStatus.COMPLETED) {
      return RecordingAnalysisStatus.COMPLETED;
    }
    if (normalized === RecordingAnalysisStatus.FAILED) {
      return RecordingAnalysisStatus.FAILED;
    }
    return RecordingAnalysisStatus.PENDING;
  }
}

