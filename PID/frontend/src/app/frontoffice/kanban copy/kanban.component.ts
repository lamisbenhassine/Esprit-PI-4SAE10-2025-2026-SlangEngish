import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { UserContextService } from '../../services/user-context.service';
import { KanbanService } from './kanban.service';
import { KanbanTask, TaskImportance } from './kanban.models';

@Component({
  selector: 'app-kanban',
  templateUrl: './kanban.component.html',
  styleUrls: ['./kanban.component.css']
})
export class KanbanComponent implements OnInit {
  userId = 1;
  loading = false;
  error: string | null = null;

  toDo: KanbanTask[] = [];
  doing: KanbanTask[] = [];
  done: KanbanTask[] = [];

  creating = false;
  newTitle = '';
  newDescription = '';
  newImportance: TaskImportance = 'MEDIUM';

  readonly importanceOptions: { value: TaskImportance; label: string }[] = [
    { value: 'VERY_IMPORTANT', label: 'Très important' },
    { value: 'MEDIUM', label: 'Moyen' },
    { value: 'COULD_WAIT', label: 'Peut attendre' }
  ];

  constructor(
    private readonly kanbanService: KanbanService,
    private readonly userContext: UserContextService,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.userId = this.userContext.getCurrentUserId();
    this.loadBoard();
  }

  loadBoard(): void {
    this.loading = true;
    this.error = null;
    this.kanbanService.getBoard(this.userId).subscribe({
      next: (board) => {
        this.toDo = board.toDo ?? [];
        this.doing = board.doing ?? [];
        this.done = board.done ?? [];
      },
      error: () => {
        this.error = 'Impossible de charger le tableau Kanban.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  createTask(): void {
    const title = this.newTitle.trim();
    if (!title) return;
    this.creating = true;
    this.kanbanService.createTask(this.userId, {
      title,
      description: this.newDescription.trim(),
      importance: this.newImportance,
      columnStatus: 'TODO'
    }).subscribe({
      next: () => {
        this.newTitle = '';
        this.newDescription = '';
        this.newImportance = 'MEDIUM';
        this.loadBoard();
      },
      error: () => {
        this.error = 'Création de la tâche impossible.';
      },
      complete: () => {
        this.creating = false;
      }
    });
  }

  deleteTask(taskId: number): void {
    this.kanbanService.deleteTask(this.userId, taskId).subscribe({
      next: () => this.loadBoard(),
      error: () => {
        this.error = 'Suppression impossible.';
      }
    });
  }

  updateImportance(task: KanbanTask, importance: TaskImportance): void {
    this.kanbanService.updateTask(this.userId, task.id, {
      title: task.title,
      description: task.description ?? '',
      importance
    }).subscribe({
      next: () => this.loadBoard(),
      error: () => {
        this.error = 'Mise à jour de la priorité impossible.';
      }
    });
  }

  drop(event: CdkDragDrop<KanbanTask[]>, targetColumn: 'TODO' | 'DOING' | 'DONE'): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );
    }

    const movedTask = event.container.data[event.currentIndex];
    if (!movedTask) return;

    this.kanbanService.moveTask(this.userId, movedTask.id, {
      targetColumn,
      targetIndex: event.currentIndex
    }).subscribe({
      next: () => this.loadBoard(),
      error: () => {
        this.error = 'Déplacement impossible.';
        this.loadBoard();
      }
    });
  }

  importanceLabel(importance: TaskImportance): string {
    return this.importanceOptions.find((i) => i.value === importance)?.label ?? importance;
  }

  /** Classes CSS pour le badge (lueur rouge / orange-jaune / vert). */
  importanceBadgeClass(importance: TaskImportance): string {
    switch (importance) {
      case 'VERY_IMPORTANT':
        return 'badge--very-important';
      case 'MEDIUM':
        return 'badge--medium';
      case 'COULD_WAIT':
        return 'badge--could-wait';
      default:
        return 'badge--medium';
    }
  }
}
