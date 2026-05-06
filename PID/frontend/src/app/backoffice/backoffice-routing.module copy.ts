import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { CoursesManagementComponent } from './courses-management/courses-management.component';
import { ClubsManagementComponent } from './clubs-management/clubs-management.component';
import { StreamsManagementComponent } from './streams-management/streams-management.component';
import { RecordingsManagementComponent } from './recordings-management/recordings-management.component';
import { ProfessorSessionCalendarComponent } from './professor-session-calendar/professor-session-calendar.component';

const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'users', component: UserManagementComponent },
      { path: 'courses', component: CoursesManagementComponent },
      { path: 'clubs', component: ClubsManagementComponent },
      { path: 'streams', component: StreamsManagementComponent },
      { path: 'recordings', component: RecordingsManagementComponent },
      { path: 'session-calendar', component: ProfessorSessionCalendarComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BackofficeRoutingModule { }
