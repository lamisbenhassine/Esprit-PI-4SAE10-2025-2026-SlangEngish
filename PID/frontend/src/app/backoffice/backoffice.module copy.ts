import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { BackofficeRoutingModule } from './backoffice-routing.module';
import { DashboardComponent } from './dashboard/dashboard.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { CoursesManagementComponent } from './courses-management/courses-management.component';
import { ClubsManagementComponent } from './clubs-management/clubs-management.component';
import { LayoutComponent } from './layout/layout.component';
import { CourseFormComponent } from './courses-management/course-form/course-form.component';
import { CourseChaptersComponent } from './courses-management/course-chapters/course-chapters.component';
import { SharedModule } from '../shared/shared.module';
import { StreamsManagementComponent } from './streams-management/streams-management.component';
import { RecordingsManagementComponent } from './recordings-management/recordings-management.component';
import { ProfessorSessionCalendarComponent } from './professor-session-calendar/professor-session-calendar.component';
import { ProfessorSessionDescriptionsDialogComponent } from './professor-session-calendar/professor-session-descriptions-dialog.component';

// Angular Material Modules
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { FullCalendarModule } from '@fullcalendar/angular';

@NgModule({
  declarations: [
    DashboardComponent,
    UserManagementComponent,
    CoursesManagementComponent,
    ClubsManagementComponent,
    LayoutComponent,
    CourseFormComponent,
    CourseChaptersComponent,
    StreamsManagementComponent,
    RecordingsManagementComponent,
    ProfessorSessionCalendarComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    BackofficeRoutingModule,
    SharedModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatSnackBarModule,
    MatChipsModule,
    MatBadgeModule,
    MatTabsModule,
    MatSelectModule,
    MatProgressBarModule,
    MatButtonModule,
    FullCalendarModule,
    ProfessorSessionDescriptionsDialogComponent
  ]
})
export class BackofficeModule { }
