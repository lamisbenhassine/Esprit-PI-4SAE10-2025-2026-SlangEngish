import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { HttpErrorResponse } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { NoteAnalyzeService, NoteAnalysisResponse } from '../note-analyze.service';

export interface NoteAnalyzeDialogData {
  userId: number;
  noteId: number;
}

export interface NoteAnalyzeDialogResult {
  newTitle?: string;
  newBodyHtml?: string;
}

@Component({
  selector: 'app-note-analyze-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatRadioModule
  ],
  templateUrl: './note-analyze-dialog.component.html',
  styleUrls: ['./note-analyze-dialog.component.css']
})
export class NoteAnalyzeDialogComponent implements OnInit {
  loading = true;
  error: string | null = null;
  response: NoteAnalysisResponse | null = null;

  applySuggestedTitle = false;
  bodyMode: 'none' | 'grammar' | 'full' = 'full';

  constructor(
    private dialogRef: MatDialogRef<NoteAnalyzeDialogComponent, NoteAnalyzeDialogResult | undefined>,
    @Inject(MAT_DIALOG_DATA) public data: NoteAnalyzeDialogData,
    private noteAnalyzeService: NoteAnalyzeService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.noteAnalyzeService.analyze(this.data.userId, this.data.noteId).subscribe({
      next: (res) => {
        this.response = res;
        this.applySuggestedTitle = this.hasUsefulTitleSuggestion(res);
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        const body = err.error as { message?: string; error?: string } | null;
        const detail = body?.message || body?.error;
        this.error =
          `L'analyse n'a pas abouti (erreur ${err.status}). ` +
          (detail ? detail : 'Vérifiez la gateway, que GestionCours tourne, et la configuration OpenAI.');
        this.loading = false;
      }
    });
  }

  /** Contenu HTML fourni par notre API (aperçu) : évite que le sanitizer Angular supprime des balises utiles. */
  trustedHtml(html: string | null | undefined): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(html ?? '');
  }

  hasUsefulTitleSuggestion(res: NoteAnalysisResponse): boolean {
    const s = (res.suggestedTitle ?? '').trim();
    if (!s) return false;
    const cur = (res.originalTitle ?? '').trim();
    return s.toLowerCase() !== cur.toLowerCase();
  }

  canApplySelection(): boolean {
    if (!this.response) return false;
    const titleOk = this.applySuggestedTitle && this.hasUsefulTitleSuggestion(this.response);
    const bodyOk = this.bodyMode !== 'none';
    return titleOk || bodyOk;
  }

  reject(): void {
    this.dialogRef.close(undefined);
  }

  applyAll(): void {
    if (!this.response) return;
    const r = this.response;
    const newTitle =
      this.hasUsefulTitleSuggestion(r) && r.suggestedTitle ? r.suggestedTitle.trim() : undefined;
    this.dialogRef.close({
      newTitle,
      newBodyHtml: r.restructuredHtml || r.grammarCorrectedHtml || undefined
    });
  }

  applySelection(): void {
    if (!this.response) return;
    const r = this.response;
    let newTitle: string | undefined;
    if (this.applySuggestedTitle && this.hasUsefulTitleSuggestion(r) && r.suggestedTitle) {
      newTitle = r.suggestedTitle.trim();
    }
    let newBodyHtml: string | undefined;
    if (this.bodyMode === 'grammar') {
      newBodyHtml = r.grammarCorrectedHtml;
    } else if (this.bodyMode === 'full') {
      newBodyHtml = r.restructuredHtml;
    }
    if (!newTitle && !newBodyHtml) {
      return;
    }
    this.dialogRef.close({ newTitle, newBodyHtml });
  }
}
