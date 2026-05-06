import { Component, OnInit } from '@angular/core';
import { Stream } from '../../models/stream.model';
import { StreamService } from './stream.service';

@Component({
  selector: 'app-live-stream',
  templateUrl: './live-stream.component.html',
  styleUrls: ['./live-stream.component.css']
})
export class LiveStreamComponent implements OnInit {

  liveStreams: Stream[] = [];
  selectedStream: Stream | null = null;
  loading = false;
  error: string | null = null;

  constructor(private streamService: StreamService) {}

  ngOnInit(): void {
    this.loadLiveStreams();
  }

  loadLiveStreams(): void {
    this.loading = true;
    this.error = null;

    this.streamService.getLiveStreams().subscribe({
      next: (streams) => {
        this.liveStreams = streams;
        this.selectedStream = streams.length > 0 ? streams[0] : null;
      },
      error: (err) => {
        console.error('Erreur chargement streams live', err);
        this.error = 'Impossible de charger les streams en direct pour le moment.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  selectStream(stream: Stream): void {
    this.selectedStream = stream;
  }
}

