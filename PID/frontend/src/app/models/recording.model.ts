export enum RecordingStatus {
  AVAILABLE = 'AVAILABLE',
  ARCHIVED = 'ARCHIVED'
}

export enum RecordingAnalysisStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

export interface RecordingHighlight {
  id?: number;
  recordingId: number;
  seconds: number;
  title: string;
  description: string;
  createdAt?: string;
}

export interface Recording {
  id?: number;
  title: string;
  streamLink: string | null;
  recordingLink: string | null;
  recordedAt: string;
  status: RecordingStatus;
  analysisStatus?: RecordingAnalysisStatus;
}

