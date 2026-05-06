export enum StreamStatus {
  PLANNED = 'PLANNED',
  LIVE = 'LIVE',
  FINISHED = 'FINISHED'
}

export interface Stream {
  id?: number;
  title: string;
  meetingLink: string;
  startTime: string;
  status: StreamStatus;
}

export interface StreamRequest {
  title: string;
  startTime: string;
  status: StreamStatus;
}

