import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ForumTopic } from '../../../core/services/forum-topic.service';

@Component({
    selector: 'app-forum-dialog',
    templateUrl: './forum-dialog.component.html',
    styleUrls: ['./forum-dialog.component.css']
})
export class ForumDialogComponent implements OnInit {
    form: FormGroup;
    isEditMode: boolean;

    categories = ['GENERAL', 'LEVEL_1', 'LEVEL_2', 'LEVEL_3'];

    constructor(
        private fb: FormBuilder,
        private dialogRef: MatDialogRef<ForumDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: { topic?: ForumTopic }
    ) {
        this.isEditMode = !!data.topic;
        this.form = this.fb.group({
            title: [data.topic?.title || '', [Validators.required, Validators.minLength(5)]],
            description: [data.topic?.description || '', Validators.required],
            category: [data.topic?.category || 'GENERAL', Validators.required],
            isPublic: [data.topic?.isPublic ?? true],
            authorId: [data.topic?.authorId || 1] // Mock for now
        });
    }

    ngOnInit(): void { }

    onCancel(): void {
        this.dialogRef.close();
    }

    onSubmit(): void {
        if (this.form.valid) {
            this.dialogRef.close(this.form.value);
        }
    }
}
