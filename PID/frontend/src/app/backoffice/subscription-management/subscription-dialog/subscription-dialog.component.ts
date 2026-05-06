import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SubscriptionPlan } from '../../../services/subscription-plan.service';

@Component({
    selector: 'app-subscription-dialog',
    templateUrl: './subscription-dialog.component.html',
    styleUrls: ['./subscription-dialog.component.css']
})
export class SubscriptionDialogComponent implements OnInit {
    form: FormGroup;
    isEditMode: boolean;

    planTypes = ['Monthly', 'Quarterly', 'Yearly', 'Premium'];

    selectedFile: File | null = null;
    imagePreview: string | null = null;

    constructor(
        private fb: FormBuilder,
        private dialogRef: MatDialogRef<SubscriptionDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: { plan?: SubscriptionPlan }
    ) {
        this.isEditMode = !!data.plan;
        this.imagePreview = data.plan?.imageUrl || null;
        this.form = this.fb.group({
            name: [data.plan?.name || '', [Validators.required, Validators.minLength(1)]],
            planType: [data.plan?.planType || '', Validators.required],
            price: [
                data.plan?.price ?? 29.99,
                [
                    Validators.required,
                    Validators.min(0.01),
                    Validators.pattern(/^\d+(\.\d{1,2})?$/)
                ]
            ],
            currency: [data.plan?.currency || 'TND', Validators.required],
            durationDays: [data.plan?.durationDays ?? 30, [Validators.required, Validators.min(1)]],
            description: [data.plan?.description || ''],
            date: [data.plan?.date || new Date().toISOString().split('T')[0], Validators.required],
            userId: [data.plan?.userId || null],
            courseId: [data.plan?.courseId || null],
            imageUrl: [data.plan?.imageUrl || '']
        });
    }

    ngOnInit(): void { }

    onFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files[0]) {
            this.selectedFile = input.files[0];
            const reader = new FileReader();
            reader.onload = () => {
                const dataUrl = String(reader.result || '');
                // Resize/compress to keep payload reasonable
                this.resizeImageDataUrl(dataUrl, 900, 520, 0.75)
                    .then((resized) => {
                        this.imagePreview = resized;
                        this.form.patchValue({ imageUrl: resized });
                    })
                    .catch((err) => {
                        console.error('Image resize failed, using original:', err);
                        this.imagePreview = dataUrl;
                        this.form.patchValue({ imageUrl: dataUrl });
                    });
            };
            reader.readAsDataURL(this.selectedFile);
        }
    }

    private resizeImageDataUrl(dataUrl: string, maxW: number, maxH: number, quality: number): Promise<string> {
        return new Promise((resolve, reject) => {
            const img = new Image();
            img.onload = () => {
                const ratio = Math.min(maxW / img.width, maxH / img.height, 1);
                const w = Math.round(img.width * ratio);
                const h = Math.round(img.height * ratio);

                const canvas = document.createElement('canvas');
                canvas.width = w;
                canvas.height = h;
                const ctx = canvas.getContext('2d');
                if (!ctx) return reject(new Error('No canvas context'));
                ctx.drawImage(img, 0, 0, w, h);

                // Prefer jpeg compression
                const out = canvas.toDataURL('image/jpeg', quality);
                resolve(out);
            };
            img.onerror = (e) => reject(e);
            img.src = dataUrl;
        });
    }

    removeImage(): void {
        this.selectedFile = null;
        this.imagePreview = null;
        this.form.patchValue({ imageUrl: '' });
    }

    onCancel(): void {
        this.dialogRef.close();
    }

    onSubmit(): void {
        if (this.form.valid) {
            const formData = this.form.value;
            // Debug: log what we're sending
            console.log('Submitting plan data:', formData);
            console.log('Image URL being sent:', formData.imageUrl);
            this.dialogRef.close(formData);
        }
    }
}
