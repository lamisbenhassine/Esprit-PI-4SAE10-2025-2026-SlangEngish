import { Component, OnInit } from '@angular/core';
import { Course, Level } from '../../models/course.model';
import { CourseService } from './course.service';

@Component({
  selector: 'app-courses-management',
  templateUrl: './courses-management.component.html',
  styleUrls: ['./courses-management.component.css']
})
export class CoursesManagementComponent implements OnInit {
  courses: Course[] = [];
  filteredCourses: Course[] = [];
  formCourse: Course | null = null;
  isCreating = false;
  isEditing = false;
  selectedCourseForChapters: Course | null = null;
  searchTerm = '';
  loading = false;
  private readonly levelOrder: Level[] = [
    Level.A1,
    Level.A2,
    Level.B1,
    Level.B2,
    Level.C1,
    Level.C2
  ];

  constructor(private courseService: CourseService) {}

  ngOnInit(): void {
    this.loadCourses();
  }

  loadCourses(): void {
    this.loading = true;
    this.courseService.getAll().subscribe({
      next: (data) => {
        this.courses = data;
        this.applyFilters();
      },
      error: (err) => {
        console.error('Erreur chargement cours', err);
        this.courses = [];
        this.filteredCourses = [];
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  getImageForCourse(course: Course): string {
    const base = 'https://via.placeholder.com/300x200/667eea/ffffff?text=';
    return `${base}${encodeURIComponent(course.name || 'Course')}`;
  }

  sortByLevel(direction: 'asc' | 'desc'): void {
    const factor = direction === 'asc' ? 1 : -1;
    const sorted = [...this.courses].sort((a, b) => {
      const ai = this.levelOrder.indexOf(a.level);
      const bi = this.levelOrder.indexOf(b.level);
      return (ai - bi) * factor;
    });
    this.courses = sorted;
    this.applyFilters(direction);
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  private applyFilters(direction: 'asc' | 'desc' = 'asc'): void {
    const term = this.searchTerm.toLowerCase().trim();
    let result = [...this.courses];

    if (term) {
      result = result.filter((course) => {
        const name = course.name?.toLowerCase() ?? '';
        const desc = course.description?.toLowerCase() ?? '';
        return name.includes(term) || desc.includes(term);
      });
    }

    const factor = direction === 'asc' ? 1 : -1;
    result.sort((a, b) => {
      const ai = this.levelOrder.indexOf(a.level);
      const bi = this.levelOrder.indexOf(b.level);
      return (ai - bi) * factor;
    });

    this.filteredCourses = result;
  }

  addCourse(): void {
    this.isCreating = true;
    this.isEditing = false;
    this.formCourse = {
      name: '',
      level: Level.A1,
      description: '',
      imageUrl: ''
    };
  }

  editCourse(course: Course): void {
    if (!course.idCourse) return;

    this.isEditing = true;
    this.isCreating = false;
    this.formCourse = { ...course };
  }

  handleSave(course: Course): void {
    if (this.isCreating) {
      this.courseService.create(course).subscribe({
        next: (created) => {
          // Ajout immédiat dans la liste sans rechargement de la page
          this.courses = [...this.courses, created];
          this.applyFilters();
          this.cancelForm();
        },
        error: (err) => console.error('Erreur création cours', err)
      });
      return;
    }

    if (this.isEditing && course.idCourse) {
      this.courseService.update(course.idCourse, course).subscribe({
        next: (updated) => {
          // Mise à jour locale du cours sans rechargement
          this.courses = this.courses.map(c =>
            c.idCourse === updated.idCourse ? updated : c
          );
          this.applyFilters();
          this.cancelForm();
        },
        error: (err) => console.error('Erreur mise à jour cours', err)
      });
    }
  }

  cancelForm(): void {
    this.isCreating = false;
    this.isEditing = false;
    this.formCourse = null;
  }

  deleteCourse(course: Course): void {
    if (!course.idCourse) return;
    if (!confirm('Supprimer ce cours ?')) return;

    this.courseService.delete(course.idCourse).subscribe({
      next: () => {
        // Suppression immédiate dans la liste locale
        this.courses = this.courses.filter(c => c.idCourse !== course.idCourse);
        this.applyFilters();
      },
      error: (err) => console.error('Erreur suppression cours', err)
    });
  }

  openChapters(course: Course): void {
    if (!course.idCourse) {
      return;
    }
    this.selectedCourseForChapters = course;
  }

  closeChapters(): void {
    this.selectedCourseForChapters = null;
  }
}
