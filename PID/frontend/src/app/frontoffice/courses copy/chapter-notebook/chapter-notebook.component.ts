import {
  Component,
  ElementRef,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  PLATFORM_ID,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { Subject, Subscription, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { ChapterNoteService } from '../../../backoffice/courses-management/chapter-note.service';
import { ChapterNote, NoteTag } from '../../../models/chapter-note.model';
import {
  NoteAnalyzeDialogComponent,
  NoteAnalyzeDialogData,
  NoteAnalyzeDialogResult
} from '../note-analyze-dialog/note-analyze-dialog.component';

@Component({
  selector: 'app-chapter-notebook',
  templateUrl: './chapter-notebook.component.html',
  styleUrls: ['./chapter-notebook.component.css']
})
export class ChapterNotebookComponent implements OnChanges, OnDestroy {
  @Input() chapterId!: number;
  @Input() userId!: number;

  @ViewChild('editor') editorRef?: ElementRef<HTMLDivElement>;

  private readonly platformId = inject(PLATFORM_ID);
  private readonly chapterNoteService = inject(ChapterNoteService);
  private readonly dialog = inject(MatDialog);

  readonly isBrowser = isPlatformBrowser(this.platformId);

  notes: ChapterNote[] = [];
  selectedNoteId: number | null = null;
  draftTitle = '';
  draftTag: NoteTag = 'GRAMMAR';
  searchText = '';
  loading = false;
  saving = false;
  error: string | null = null;
  analyzeDialogOpen = false;

  readonly tagOptions: { value: NoteTag; label: string }[] = [
    { value: 'GRAMMAR', label: 'Grammaire' },
    { value: 'VOCABULARY', label: 'Vocabulaire' },
    { value: 'PHONETICS', label: 'Phonétique' }
  ];

  private readonly search$ = new Subject<string>();
  private searchSub?: Subscription;

  constructor() {
    this.searchSub = this.search$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => {
          if (!isPlatformBrowser(this.platformId) || this.chapterId == null || this.userId == null) {
            return of([]);
          }
          this.loading = true;
          return this.chapterNoteService.listByChapter(this.userId, this.chapterId, q);
        })
      )
      .subscribe({
        next: (list) => {
          this.notes = list;
          this.loading = false;
          this.error = null;
        },
        error: () => {
          this.loading = false;
          this.error = 'Impossible de charger les notes.';
        }
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (
      (changes['chapterId'] || changes['userId']) &&
      this.chapterId != null &&
      this.userId != null
    ) {
      if (!isPlatformBrowser(this.platformId)) {
        this.loading = false;
        this.notes = [];
        this.error = null;
        return;
      }
      if (changes['chapterId'] && !changes['chapterId'].firstChange) {
        this.newNote();
      }
      this.refreshList();
    }
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  onSearchInput(value: string): void {
    this.searchText = value;
    this.search$.next(value);
  }

  refreshList(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.loading = true;
    this.chapterNoteService.listByChapter(this.userId, this.chapterId, this.searchText).subscribe({
      next: (list) => {
        this.notes = list;
        this.loading = false;
        this.error = null;
      },
      error: () => {
        this.loading = false;
        this.error = 'Impossible de charger les notes.';
      }
    });
  }

  selectNote(note: ChapterNote): void {
    this.selectedNoteId = note.id;
    this.draftTitle = note.title ?? '';
    this.draftTag = note.tag;
    setTimeout(() => {
      const el = this.editorRef?.nativeElement;
      if (el) {
        el.innerHTML = note.content ?? '';
      }
    });
  }

  newNote(): void {
    this.selectedNoteId = null;
    this.draftTitle = '';
    this.draftTag = 'GRAMMAR';
    setTimeout(() => {
      const el = this.editorRef?.nativeElement;
      if (el) {
        el.innerHTML = '';
        el.focus();
      }
    });
  }

  private focusEditor(): void {
    this.editorRef?.nativeElement?.focus();
  }

  format(cmd: 'bold' | 'italic' | 'underline'): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.focusEditor();
    document.execCommand(cmd, false);
  }

  insertBulletList(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.focusEditor();
    document.execCommand('insertUnorderedList', false);
  }

  insertArrow(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.focusEditor();
    document.execCommand('insertText', false, ' → ');
  }

  insertExampleLabel(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.focusEditor();
    document.execCommand('insertText', false, 'Ex. ');
  }

  private getEditorHtml(): string {
    return this.editorRef?.nativeElement?.innerHTML ?? '';
  }

  saveNote(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const title = (this.draftTitle || '').trim() || 'Sans titre';
    const content = this.getEditorHtml();
    const payload = { title, content, tag: this.draftTag };
    this.saving = true;
    this.error = null;

    const done = () => {
      this.saving = false;
    };

    if (this.selectedNoteId != null) {
      this.chapterNoteService.update(this.userId, this.selectedNoteId, payload).subscribe({
        next: (updated) => {
          done();
          this.refreshList();
          this.selectNote(updated);
        },
        error: () => {
          done();
          this.error = 'Enregistrement impossible.';
        }
      });
    } else {
      this.chapterNoteService.create(this.userId, this.chapterId, payload).subscribe({
        next: (created) => {
          done();
          this.refreshList();
          this.selectNote(created);
        },
        error: () => {
          done();
          this.error = 'Enregistrement impossible.';
        }
      });
    }
  }

  deleteNote(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    if (this.selectedNoteId == null) return;
    if (!confirm('Supprimer cette note ?')) return;
    const id = this.selectedNoteId;
    this.chapterNoteService.delete(this.userId, id).subscribe({
      next: () => {
        this.newNote();
        this.refreshList();
      },
      error: () => {
        this.error = 'Suppression impossible.';
      }
    });
  }

  tagClass(tag: NoteTag): string {
    switch (tag) {
      case 'GRAMMAR':
        return 'tag-grammar';
      case 'VOCABULARY':
        return 'tag-vocabulary';
      case 'PHONETICS':
        return 'tag-phonetics';
      default:
        return '';
    }
  }

  tagLabel(tag: NoteTag): string {
    return this.tagOptions.find((o) => o.value === tag)?.label ?? tag;
  }

  formatNoteDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  openAnalyzeDialog(): void {
    if (!isPlatformBrowser(this.platformId) || this.selectedNoteId == null || this.analyzeDialogOpen) {
      return;
    }
    this.analyzeDialogOpen = true;
    const data: NoteAnalyzeDialogData = { userId: this.userId, noteId: this.selectedNoteId };
    const ref = this.dialog.open(NoteAnalyzeDialogComponent, {
      width: 'min(1120px, 96vw)',
      maxWidth: '96vw',
      maxHeight: '90vh',
      autoFocus: 'first-tabbable',
      data
    });
    ref.afterClosed().subscribe((result: NoteAnalyzeDialogResult | undefined) => {
      this.analyzeDialogOpen = false;
      if (!result) return;
      if (result.newTitle != null) {
        this.draftTitle = result.newTitle;
      }
      if (result.newBodyHtml != null) {
        setTimeout(() => {
          const el = this.editorRef?.nativeElement;
          if (el) {
            el.innerHTML = result.newBodyHtml!;
          }
        });
      }
    });
  }
}
