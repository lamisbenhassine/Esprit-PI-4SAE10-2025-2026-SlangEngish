import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { CoursesManagementComponent } from './courses-management/courses-management.component';
import { ClubsManagementComponent } from './clubs-management/clubs-management.component';
import { BackofficeProfileComponent } from './profile/backoffice-profile.component';
import { ReclamationsManagementComponent } from './reclamations-management/reclamations-management.component';
import { DemandesClubComponent } from './demandes-club/demandes-club.component';
import { ReunionsClubComponent } from './reunions-club/reunions-club.component';
import { ClubMembersComponent } from './club-members/club-members.component';
import { ClubPostsComponent } from './club-posts/club-posts.component';
import { EventsManagementComponent } from './events-management/events-management.component';
import { FeedbacksManagementComponent } from './feedbacks-management/feedbacks-management.component';
import { PilotageVieAssociativeComponent } from './pilotage-vie-associative/pilotage-vie-associative.component';
import { JobsManagementComponent } from './jobs-management/jobs-management.component';
import { ApplicationsManagementComponent } from './applications-management/applications-management.component';
import { JobOfferStatsComponent } from './job-offer-stats/job-offer-stats.component';
import { InterviewCalendarComponent } from './interview-calendar/interview-calendar.component';
import { FraudDashboardComponent } from './fraud-dashboard/fraud-dashboard.component';
import { EvaluationsManagementComponent } from './evaluations-management/evaluations-management.component';
import { EvaluationFormComponent } from './evaluation-form/evaluation-form.component';
import { EvaluationQuestionsComponent } from './evaluation-questions/evaluation-questions.component';
import { EvaluationAttemptsComponent } from './evaluation-attempts/evaluation-attempts.component';
import { NotebookGamesComponent } from './notebook-games/notebook-games.component';
import { StreamsManagementComponent } from './streams-management/streams-management.component';
import { RecordingsManagementComponent } from './recordings-management/recordings-management.component';
import { ProfessorSessionCalendarComponent } from './professor-session-calendar/professor-session-calendar.component';
import { ForumManagementComponent } from './forum-management/forum-management.component';
import { CommentModerationComponent } from './comment-moderation/comment-moderation.component';
import { SubscriptionManagementComponent } from './subscription-management/subscription-management.component';
import { LoyaltyAccountsComponent } from './loyalty-accounts/loyalty-accounts.component';
import { InscriptionStatsComponent } from './inscription-stats/inscription-stats.component';
import { TutorInboxComponent } from './tutor-inbox/tutor-inbox.component';

const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'tutor-inbox', component: TutorInboxComponent },
      { path: 'forum', component: ForumManagementComponent },
      { path: 'forum-comments', component: CommentModerationComponent },
      { path: 'subscription', component: SubscriptionManagementComponent },
      { path: 'loyalty-accounts', component: LoyaltyAccountsComponent },
      { path: 'inscription-stats', component: InscriptionStatsComponent },
      { path: 'users', component: UserManagementComponent },
      { path: 'courses', component: CoursesManagementComponent },
      { path: 'clubs', component: ClubsManagementComponent },
      { path: 'demandes-club', component: DemandesClubComponent },
      { path: 'club-members', component: ClubMembersComponent },
      { path: 'club-posts', component: ClubPostsComponent },
      { path: 'reunions-club', component: ReunionsClubComponent },
      { path: 'events', component: EventsManagementComponent },
      { path: 'feedbacks', component: FeedbacksManagementComponent },
      { path: 'vie-associative', component: PilotageVieAssociativeComponent },
      { path: 'reclamations', component: ReclamationsManagementComponent },
      { path: 'profile', component: BackofficeProfileComponent },
      { path: 'jobs', component: JobsManagementComponent },
      { path: 'applications', component: ApplicationsManagementComponent },
      { path: 'job-stats', component: JobOfferStatsComponent },
      { path: 'interviews', component: InterviewCalendarComponent },
      { path: 'fraud', component: FraudDashboardComponent },
      { path: 'evaluations', component: EvaluationsManagementComponent },
      { path: 'evaluations/new', component: EvaluationFormComponent },
      { path: 'evaluations/:id', component: EvaluationFormComponent },
      { path: 'evaluations/:id/questions', component: EvaluationQuestionsComponent },
      { path: 'evaluations/:id/attempts', component: EvaluationAttemptsComponent },
      { path: 'notebook-games', component: NotebookGamesComponent },
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
