import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { CoursesManagementComponent } from './courses-management/courses-management.component';
import { ForumManagementComponent } from './forum-management/forum-management.component';
import { SubscriptionManagementComponent } from './subscription-management/subscription-management.component';
import { LoyaltyAccountsComponent } from './loyalty-accounts/loyalty-accounts.component';
import { ClubsManagementComponent } from './clubs-management/clubs-management.component';
import { InscriptionStatsComponent } from './inscription-stats/inscription-stats.component';
import { TutorInboxComponent } from './tutor-inbox/tutor-inbox.component';
import { CommentModerationComponent } from './comment-moderation/comment-moderation.component';

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
      { path: 'forum', component: ForumManagementComponent },
      { path: 'forum-comments', component: CommentModerationComponent },
      { path: 'tutor-messages', component: TutorInboxComponent },
      { path: 'subscription', component: SubscriptionManagementComponent },
      { path: 'loyalty-accounts', component: LoyaltyAccountsComponent },
      { path: 'inscription-stats', component: InscriptionStatsComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BackofficeRoutingModule { }
