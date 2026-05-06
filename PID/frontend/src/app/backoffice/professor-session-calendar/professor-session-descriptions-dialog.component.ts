import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface SessionDescriptionItem {
  slotCode: string;
  dayLabel: string;
  timeLabel: string;
  description: string;
}

export interface ProfessorSessionDescriptionsDialogData {
  items: SessionDescriptionItem[];
  readOnly: boolean;
}

/** Résultat : soit enregistrement des textes, soit retrait d'un créneau. */
export interface ProfessorSessionDescriptionsDialogResult {
  descriptions?: Record<string, string>;
  removed?: string;
}

@Component({
  selector: 'app-professor-session-descriptions-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './professor-session-descriptions-dialog.component.html',
  styleUrls: ['./professor-session-descriptions-dialog.component.css']
})
export class ProfessorSessionDescriptionsDialogComponent {
  items: SessionDescriptionItem[];

  constructor(
    private readonly dialogRef: MatDialogRef<
      ProfessorSessionDescriptionsDialogComponent,
      ProfessorSessionDescriptionsDialogResult
    >,
    @Inject(MAT_DIALOG_DATA) public readonly data: ProfessorSessionDescriptionsDialogData
  ) {
    this.items = data.items.map((i) => ({
      ...i,
      description: i.description ?? ''
    }));
  }

  hintLength(item: SessionDescriptionItem): number {
    return (item.description ?? '').length;
  }

  save(): void {
    const out: Record<string, string> = {};
    for (const item of this.items) {
      out[item.slotCode] = (item.description ?? '').trim();
    }
    this.dialogRef.close({ descriptions: out });
  }

  removeSlot(item: SessionDescriptionItem): void {
    this.dialogRef.close({ removed: item.slotCode });
  }
}
