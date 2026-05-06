export type KanbanColumn = 'TODO' | 'DOING' | 'DONE';
export type TaskImportance = 'VERY_IMPORTANT' | 'MEDIUM' | 'COULD_WAIT';

export interface KanbanTask {
  id: number;
  userId: number;
  title: string;
  description: string;
  columnStatus: KanbanColumn;
  importance: TaskImportance;
  positionInColumn: number;
  createdAt: string;
  updatedAt: string;
}

export interface KanbanBoard {
  toDo: KanbanTask[];
  doing: KanbanTask[];
  done: KanbanTask[];
}

export interface CreateOrUpdateTaskPayload {
  title: string;
  description: string;
  importance: TaskImportance;
  columnStatus?: KanbanColumn;
}

export interface MoveTaskPayload {
  targetColumn: KanbanColumn;
  targetIndex: number;
}
