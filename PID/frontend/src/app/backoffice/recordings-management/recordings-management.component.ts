import { Component, OnInit } from '@angular/core';
import { Recording, RecordingStatus } from '../../models/recording.model';
import { RecordingAdminService, RecordingUpdatePayload, RecordingCreatePayload } from './recording-admin.service';

@Component({
  selector: 'app-recordings-management',
  templateUrl: './recordings-management.component.html',
  styleUrls: ['./recordings-management.component.css']
})
export class RecordingsManagementComponent implements OnInit {

  recordings: Recording[] = [];
  loading = false;
  error: string | null = null;

  isEditing = false;
  isCreating = false;
  uploadingId: number | null = null;
  selectedRecordingFile: File | null = null;
  selectedRecordingFileName: string | null = null;

  formRecording: {
    id?: number;
    title: string;
    recordingLink: string | null;
    status: RecordingStatus;
    streamLink?: string | null;
    recordedAt?: string;
  } | null = null;

  readonly statuses = Object.values(RecordingStatus);

  constructor(private recordingService: RecordingAdminService) {}

  ngOnInit(): void {
    this.loadRecordings();
  }

  loadRecordings(): void {
    this.loading = true;
    this.error = null;

    this.recordingService.getAll().subscribe({
      next: (data) => (this.recordings = data),
      error: (err) => {
        console.error('Erreur chargement recordings', err);
        this.error = 'Impossible de charger les enregistrements.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  addRecording(): void {
    this.isCreating = true;
    this.isEditing = false;
    this.selectedRecordingFile = null;
    this.selectedRecordingFileName = null;
    this.formRecording = {
      title: '',
      recordingLink: null,
      status: RecordingStatus.AVAILABLE
    };
  }

  editRecording(rec: Recording): void {
    if (!rec.id) {
      return;
    }
    this.isEditing = true;
    this.isCreating = false;
    this.selectedRecordingFile = null;
    this.selectedRecordingFileName = null;
    this.formRecording = {
      id: rec.id,
      title: rec.title,
      recordingLink: rec.recordingLink,
      status: rec.status,
      streamLink: rec.streamLink,
      recordedAt: rec.recordedAt
    };
  }

  onFormFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      this.selectedRecordingFile = null;
      this.selectedRecordingFileName = null;
      return;
    }
    const file = input.files[0];
    this.selectedRecordingFile = file;
    this.selectedRecordingFileName = file.name;
  }

  saveRecording(): void {
    if (!this.formRecording) {
      return;
    }

    if (this.isCreating) {
      const payload: RecordingCreatePayload = {
        title: this.formRecording.title,
        recordingLink: null,
        status: this.formRecording.status
      };
      this.recordingService.create(payload).subscribe({
        next: (created) => {
          if (this.selectedRecordingFile && created.id) {
            this.recordingService.uploadFile(created.id, this.selectedRecordingFile).subscribe({
              next: () => {
                this.loadRecordings();
                this.cancelForm();
              },
              error: (err) => {
                console.error('Erreur upload fichier recording', err);
                this.loadRecordings();
                this.cancelForm();
              }
            });
          } else {
            this.loadRecordings();
            this.cancelForm();
          }
        },
        error: (err) => console.error('Erreur création recording', err)
      });
      return;
    }

    if (this.isEditing && this.formRecording.id) {
      const doUpdate = (recordingLink: string | null) => {
        const payload: RecordingUpdatePayload = {
          title: this.formRecording!.title,
          streamLink: this.formRecording!.streamLink ?? null,
          recordingLink,
          recordedAt: this.formRecording!.recordedAt ?? new Date().toISOString(),
          status: this.formRecording!.status
        };
        this.recordingService.update(this.formRecording!.id!, payload).subscribe({
          next: () => {
            this.loadRecordings();
            this.cancelForm();
          },
          error: (err) => console.error('Erreur mise à jour recording', err)
        });
      };

      if (this.selectedRecordingFile) {
        this.recordingService.uploadFile(this.formRecording.id, this.selectedRecordingFile).subscribe({
          next: (updated) => {
            doUpdate(updated.recordingLink ?? null);
          },
          error: (err) => {
            console.error('Erreur upload fichier recording', err);
            doUpdate(this.formRecording!.recordingLink ?? null);
          }
        });
      } else {
        doUpdate(this.formRecording.recordingLink ?? null);
      }
    }
  }

  cancelForm(): void {
    this.isCreating = false;
    this.isEditing = false;
    this.formRecording = null;
    this.selectedRecordingFile = null;
    this.selectedRecordingFileName = null;
  }

  deleteRecording(rec: Recording): void {
    if (!rec.id) {
      return;
    }
    if (!confirm('Supprimer cet enregistrement ?')) {
      return;
    }

    this.recordingService.delete(rec.id).subscribe({
      next: () => (this.recordings = this.recordings.filter(r => r.id !== rec.id)),
      error: (err) => console.error('Erreur suppression recording', err)
    });
  }

  onFileSelected(rec: Recording, event: Event): void {
    if (!rec.id) {
      return;
    }
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }
    const file = input.files[0];
    this.uploadingId = rec.id;

    this.recordingService.uploadFile(rec.id, file).subscribe({
      next: (updated) => {
        this.recordings = this.recordings.map(r => (r.id === updated.id ? updated : r));
        this.uploadingId = null;
      },
      error: (err) => {
        console.error('Erreur upload fichier recording', err);
        this.uploadingId = null;
      }
    });

    // reset input
    input.value = '';
  }
}

