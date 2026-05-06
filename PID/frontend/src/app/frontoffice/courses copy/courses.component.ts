import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { Course } from '../../models/course.model';
import { Chapter } from '../../models/chapter.model';
import { CourseService } from '../../backoffice/courses-management/course.service';
import { ChapterService } from '../../backoffice/courses-management/chapter.service';
import { ProgressionService } from '../../backoffice/courses-management/progression.service';
import { UserContextService } from '../../services/user-context.service';

@Component({
  selector: 'app-courses',
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.css']
})
export class CoursesComponent implements OnInit, OnDestroy {
  courses: Course[] = [];
  chapters: Chapter[] = [];
  selectedCourse: Course | null = null;
  loading = false;
  courseProgress = 0;
  private userId = 0;
  private progressPolling?: Subscription;

  constructor(
    private courseService: CourseService,
    private chapterService: ChapterService,
    private progressionService: ProgressionService,
    private userContext: UserContextService,
    private router: Router,
    private route: ActivatedRoute,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.userId = this.userContext.getCurrentUserId();
    this.loadCourses();
    this.route.queryParams.subscribe(params => {
      const courseId = params['courseId'];
      if (courseId != null && this.selectedCourse?.idCourse === Number(courseId)) {
        this.loadCourseProgressOnly(Number(courseId));
      }
    });
  }

  loadCourses(): void {
    this.loading = true;
    this.courseService.getAll().subscribe({
      next: (data) => {
        this.courses = data;
        this.openCourseFromQueryParam();
      },
      error: (err) => {
        console.error('Erreur chargement cours', err);
        this.loading = false;
      },
      complete: () => (this.loading = false)
    });
  }

  private openCourseFromQueryParam(): void {
    const courseId = this.route.snapshot.queryParamMap.get('courseId');
    if (courseId != null && this.courses.length > 0) {
      const id = Number(courseId);
      const course = this.courses.find(c => c.idCourse === id);
      if (course) {
        this.viewDetails(course);
      }
    }
  }

  loadCourseProgressOnly(courseId: number): void {
    this.progressionService.getCourseProgress(this.userId, courseId).subscribe({
      next: (p) => (this.courseProgress = p ?? 0),
      error: () => (this.courseProgress = 0)
    });
  }

  viewDetails(course: Course): void {
    this.selectedCourse = course;
    const id = course.idCourse;
    if (id == null) {
      this.chapters = [];
      this.courseProgress = 0;
      return;
    }
    this.chapterService.getByCourse(id).subscribe({
      next: (data) => (this.chapters = data),
      error: (err) => {
        console.error('Erreur chargement chapitres', err);
        this.chapters = [];
      }
    });
    this.progressionService.getCourseProgress(this.userId, id).subscribe({
      next: (p) => (this.courseProgress = p ?? 0),
      error: () => (this.courseProgress = 0)
    });

    this.progressPolling?.unsubscribe();
    this.progressPolling = interval(15000).subscribe(() => this.loadCourseProgressOnly(id));
  }

  ngOnDestroy(): void {
    this.progressPolling?.unsubscribe();
  }

  closeDetails(): void {
    this.progressPolling?.unsubscribe();
    this.progressPolling = undefined;
    this.selectedCourse = null;
    this.chapters = [];
    this.courseProgress = 0;
  }

  getCourseImage(course: Course): string {
    if (course.imageUrl) return course.imageUrl;
    const label = encodeURIComponent(course.name || 'Course');
    return `data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='300' height='200'><rect width='100%' height='100%' fill='%23667eea'/><text x='50%' y='50%' dominant-baseline='middle' text-anchor='middle' fill='white' font-size='22' font-family='Arial'>${label}</text></svg>`;
  }

  openPdf(chapter: Chapter): void {
    if (!chapter.pdfSupport) return;
    const courseId = this.selectedCourse?.idCourse;
    if (courseId == null || chapter.idChapter == null) return;
    this.router.navigate(['/frontoffice/course', courseId, 'chapter', chapter.idChapter, 'pdf']);
  }
}
