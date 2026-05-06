import { Component, OnInit } from '@angular/core';
import { Stream, StreamRequest, StreamStatus } from '../../models/stream.model';
import { StreamAdminService } from './stream-admin.service';

@Component({
  selector: 'app-streams-management',
  templateUrl: './streams-management.component.html',
  styleUrls: ['./streams-management.component.css']
})
export class StreamsManagementComponent implements OnInit {

  streams: Stream[] = [];
  formStream: { id?: number; title: string; startTime: string; status: StreamStatus } | null = null;
  isCreating = false;
  isEditing = false;
  loading = false;
  error: string | null = null;

  readonly statuses = Object.values(StreamStatus);

  constructor(private streamService: StreamAdminService) {}

  ngOnInit(): void {
    this.loadStreams();
  }

  loadStreams(): void {
    this.loading = true;
    this.error = null;

    this.streamService.getAll().subscribe({
      next: (data) => (this.streams = data),
      error: (err) => {
        console.error('Erreur chargement streams', err);
        this.error = 'Impossible de charger les streams.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  addStream(): void {
    this.isCreating = true;
    this.isEditing = false;

    const now = new Date();
    const localIso = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16); // format pour input datetime-local

    this.formStream = {
      title: '',
      startTime: localIso,
      status: StreamStatus.PLANNED
    };
  }

  editStream(stream: Stream): void {
    if (!stream.id) {
      return;
    }

    this.isEditing = true;
    this.isCreating = false;

    let startTime = stream.startTime;
    if (startTime) {
      // Adapter pour le champ datetime-local
      startTime = startTime.slice(0, 16);
    }

    this.formStream = {
      id: stream.id,
      title: stream.title,
      startTime: startTime,
      status: stream.status
    };
  }

  saveStream(): void {
    if (!this.formStream) {
      return;
    }

    const request: StreamRequest = this.toRequest(this.formStream);

    if (this.isCreating) {
      this.streamService.create(request).subscribe({
        next: () => {
          this.loadStreams();
          this.cancelForm();
        },
        error: (err) => console.error('Erreur création stream', err)
      });
      return;
    }

    if (this.isEditing && this.formStream.id) {
      this.streamService.update(this.formStream.id, request).subscribe({
        next: () => {
          this.loadStreams();
          this.cancelForm();
        },
        error: (err) => console.error('Erreur mise à jour stream', err)
      });
    }
  }

  cancelForm(): void {
    this.isCreating = false;
    this.isEditing = false;
    this.formStream = null;
  }

  deleteStream(stream: Stream): void {
    if (!stream.id) {
      return;
    }
    if (!confirm('Supprimer ce live stream ?')) {
      return;
    }

    this.streamService.delete(stream.id).subscribe({
      next: () => (this.streams = this.streams.filter(s => s.id !== stream.id)),
      error: (err) => console.error('Erreur suppression stream', err)
    });
  }

  setLive(stream: Stream): void {
    this.updateStatus(stream, StreamStatus.LIVE);
  }

  setFinished(stream: Stream): void {
    this.updateStatus(stream, StreamStatus.FINISHED);
  }

  private updateStatus(stream: Stream, status: StreamStatus): void {
    if (!stream.id) {
      return;
    }

    this.streamService.updateStatus(stream, status).subscribe({
      next: () => this.loadStreams(),
      error: (err) => console.error('Erreur mise à jour statut stream', err)
    });
  }

  private toRequest(form: { title: string; startTime: string; status: StreamStatus }): StreamRequest {
    let startTime = form.startTime;

    // Si c'est un datetime-local (YYYY-MM-DDTHH:mm), on ajoute les secondes
    if (startTime && startTime.length === 16) {
      startTime = `${startTime}:00`;
    }

    return {
      title: form.title,
      startTime,
      status: form.status
    };
  }
}

