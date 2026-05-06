export interface Chapter {
  idChapter?: number;
  name: string;
  description: string;
  pdfSupport?: string | null;
  videoSupport?: string | null;
  course?: {
    idCourse: number;
  };
}

