import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { CoursesManagementComponent } from './courses-management/courses-management.component';
import { ClubsManagementComponent } from './clubs-management/clubs-management.component';
import { JobsManagementComponent } from './jobs-management/jobs-management.component';
import { ApplicationsManagementComponent } from './applications-management/applications-management.component';
import { JobOfferStatsComponent } from './job-offer-stats/job-offer-stats.component';



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
      { path: 'jobs', component: JobsManagementComponent },  // ← NOUVELLE ROUTE
      { path: 'applications', component: ApplicationsManagementComponent },
        { path: 'job-stats', component: JobOfferStatsComponent }, // ✅ ajoute

    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BackofficeRoutingModule { }
