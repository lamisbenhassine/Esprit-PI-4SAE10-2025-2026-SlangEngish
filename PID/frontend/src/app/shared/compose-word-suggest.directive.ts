import { Directive, ElementRef, HostListener, OnDestroy, inject } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, switchMap } from 'rxjs/operators';
import { EnglishCompletionService } from '../core/services/english-completion.service';

/**
 * Suggestions de mots (préfixe + dataset) sous le contrôle du template parent via exportAs.
 * Usage :
 * `<textarea composeWordSuggest #cw="composeWordSuggest" [(ngModel)]="draft"></textarea>`
 * puis une rangée de boutons qui appellent `cw.applySuggestion(w)` pour les entrées de `cw.suggestions`.
 */
@Directive({
  selector: 'input[composeWordSuggest],textarea[composeWordSuggest]',
  exportAs: 'composeWordSuggest'
})
export class ComposeWordSuggestDirective implements OnDestroy {
  private readonly el = inject(ElementRef<HTMLInputElement | HTMLTextAreaElement>);
  private readonly eng = inject(EnglishCompletionService);

  suggestions: string[] = [];

  private readonly tick$ = new Subject<void>();
  private readonly sub: Subscription;

  constructor() {
    this.sub = this.tick$
      .pipe(
        debounceTime(160),
        switchMap(() => this.eng.completionsFor(this.currentWordPrefix()))
      )
      .subscribe(list => {
        this.suggestions = list;
      });
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
    this.tick$.complete();
  }

  @HostListener('input')
  @HostListener('keyup')
  onChange(): void {
    this.tick$.next();
  }

  private currentWordPrefix(): string {
    const ta = this.el.nativeElement;
    const pos = ta.selectionStart ?? ta.value.length;
    const left = ta.value.slice(0, pos);
    const m = left.match(/([a-zA-Z']{1,28})$/);
    return m ? m[1] : '';
  }

  applySuggestion(word: string): void {
    const ta = this.el.nativeElement;
    const pos = ta.selectionStart ?? ta.value.length;
    const v = ta.value;
    const left = v.slice(0, pos);
    const right = v.slice(pos);
    const m = left.match(/([a-zA-Z']{1,28})$/);
    if (!m) {
      return;
    }
    const insert = word;
    const newLeft = left.slice(0, left.length - m[1].length) + insert;
    ta.value = newLeft + right;
    ta.dispatchEvent(new Event('input', { bubbles: true }));
    const caret = newLeft.length;
    ta.setSelectionRange(caret, caret);
    this.suggestions = [];
  }
}
