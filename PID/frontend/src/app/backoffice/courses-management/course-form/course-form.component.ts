import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Course, Level } from '../../../models/course.model';

@Component({
  selector: 'app-course-form',
  templateUrl: './course-form.component.html',
  styleUrls: ['./course-form.component.css']
})
export class CourseFormComponent {
  @Input() course: Course | null = null;
  @Input() isCreating = false;
  @Output() save = new EventEmitter<Course>();
  @Output() cancel = new EventEmitter<void>();

  levels = Object.values(Level);

  onSubmit(): void {
    if (this.course) {
      this.save.emit(this.course);
    }
  }

  onCancel(): void {
    this.cancel.emit();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.course) {
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (this.course) {
        this.course.imageUrl = reader.result as string; // data URL (base64)
      }
    };
    reader.readAsDataURL(file);
  }
}

