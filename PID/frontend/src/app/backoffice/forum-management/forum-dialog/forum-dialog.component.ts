import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ForumTopic } from '../../../core/services/forum-topic.service';
import { ForumMediaService } from '../../../core/services/forum-media.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-forum-dialog',
  templateUrl: './forum-dialog.component.html',
  styleUrls: ['./forum-dialog.component.css']
})
export class ForumDialogComponent implements OnInit {
  form: FormGroup;
  isEditMode: boolean;
  uploadBusy = false;

  categories = ['GENERAL', 'LEVEL_1', 'LEVEL_2', 'LEVEL_3'];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ForumDialogComponent>,
    private mediaService: ForumMediaService,
    private snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) public data: { topic?: ForumTopic }
  ) {
    this.isEditMode = !!data.topic;
    const t = data.topic;
    this.form = this.fb.group({
      title: [t?.title || '', [Validators.required, Validators.minLength(5)]],
      description: [t?.description || '', Validators.required],
      category: [t?.category || 'GENERAL', Validators.required],
      isPublic: [t?.isPublic ?? true],
      authorId: [t?.authorId || 1],
      coverImageUrl: [t?.coverImageUrl || ''],
      pinned: [t?.pinned ?? false],
      locked: [t?.locked ?? false]
    });
  }

  ngOnInit(): void {}

  onCancel(): void {
    this.dialogRef.close();
  }

  onCoverSelected(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !file.type.startsWith('image/')) {
      return;
    }
    this.uploadBusy = true;
    this.mediaService.upload(file).subscribe({
      next: res => {
        this.form.patchValue({ coverImageUrl: res.url });
        this.uploadBusy = false;
        input.value = '';
        this.snackBar.open('Image téléversée', 'OK', { duration: 2000 });
      },
      error: err => {
        this.uploadBusy = false;
        input.value = '';
        this.snackBar.open(this.mediaService.describeUploadError(err), 'OK', { duration: 7000 });
      }
    });
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value);
    }
  }

}
