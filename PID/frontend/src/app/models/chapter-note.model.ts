export type NoteTag = 'GRAMMAR' | 'VOCABULARY' | 'PHONETICS';

export interface ChapterNote {
  id: number;
  chapterId: number;
  title: string;
  content: string;
  tag: NoteTag;
  createdAt: string;
  updatedAt: string;
}
