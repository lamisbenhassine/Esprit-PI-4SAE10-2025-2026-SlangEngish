import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProfileComponent } from './profile/profile.component';
import { CoursesComponent } from './courses/courses.component';
import { ChatComponent } from './chat/chat.component';
import { ReclamationsComponent } from './reclamations/reclamations.component';
import { EventsComponent } from './events/events.component';
import { ClubsComponent } from './clubs/clubs.component';
import { ClubChatComponent } from './club-chat/club-chat.component';
import { ClubFeedbackComponent } from './club-feedback/club-feedback.component';
import { ClubPostsComponent } from './club-posts/club-posts.component';
import { PassportCompetencesComponent } from './passport-competences/passport-competences.component';
import { JobOffersComponent } from './job-offers/job-offers.component';
import { JobDetailsComponent } from './job-details/job-details.component';
import { SavedOffersComponent } from './saved-offers/saved-offers.component';
import { StudentPreferencesComponent } from './student-preferences/student-preferences.component';
import { ChatbotComponent } from './chatbot/chatbot.component';
import { EvaluationsListComponent } from './evaluations-list/evaluations-list.component';
import { TakeEvaluationComponent } from './take-evaluation/take-evaluation.component';
import { EvaluationResultsComponent } from './evaluation-results/evaluation-results.component';
import { SmartNotebookComponent } from './smart-notebook/smart-notebook.component';
import { KanbanComponent } from './kanban/kanban.component';
import { RecordingsComponent } from './recordings/recordings.component';
import { MessagesComponent } from './messages/messages.component';
import { FeedComponent } from './feed/feed.component';

const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'feed', component: FeedComponent, data: { feedLayout: true } },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'profile', component: ProfileComponent },
      { path: 'courses', component: CoursesComponent },
      { path: 'chat', component: ChatComponent },
      { path: 'reclamations', component: ReclamationsComponent },
      { path: 'events', component: EventsComponent },
      { path: 'clubs', component: ClubsComponent },
      { path: 'club-chat', component: ClubChatComponent },
      { path: 'club-feedback', component: ClubFeedbackComponent },
      { path: 'club-posts', component: ClubPostsComponent },
      { path: 'passport', component: PassportCompetencesComponent },
      { path: 'job-offers', component: JobOffersComponent },
      { path: 'job-details/:id', component: JobDetailsComponent },
      { path: 'saved-offers', component: SavedOffersComponent },
      { path: 'preferences', component: StudentPreferencesComponent },
      { path: 'chatbot', component: ChatbotComponent },
      { path: 'evaluations', component: EvaluationsListComponent },
      { path: 'evaluations/:id/take', component: TakeEvaluationComponent },
      { path: 'evaluations/:id/results', component: EvaluationResultsComponent },
      { path: 'notebook', component: SmartNotebookComponent },
      { path: 'kanban', component: KanbanComponent },
      { path: 'recordings', component: RecordingsComponent },
      { path: 'messages', component: MessagesComponent },
      {
        path: 'forum',
        loadChildren: () => import('./forum/forum.module').then(m => m.ForumModule)
      },
      {
        path: 'inscription',
        loadChildren: () => import('./inscription/inscription.module').then(m => m.InscriptionModule)
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FrontofficeRoutingModule { }
