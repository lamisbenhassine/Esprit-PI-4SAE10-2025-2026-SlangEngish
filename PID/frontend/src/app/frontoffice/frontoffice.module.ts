import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { FrontofficeRoutingModule } from './frontoffice-routing.module';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProfileComponent } from './profile/profile.component';
import { CoursesComponent } from './courses/courses.component';
import { ChatComponent } from './chat/chat.component';
import { LayoutComponent } from './layout/layout.component';
import { SharedModule } from '../shared/shared.module';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ReclamationsComponent } from './reclamations/reclamations.component';
import { EventsComponent } from './events/events.component';
import { ClubsComponent } from './clubs/clubs.component';
import { ClubChatComponent } from './club-chat/club-chat.component';
import { ClubFeedbackComponent } from './club-feedback/club-feedback.component';
import { ClubPostsComponent } from './club-posts/club-posts.component';
import { PassportCompetencesComponent } from './passport-competences/passport-competences.component';
import { JoinClubDialogComponent } from './clubs/join-club-dialog/join-club-dialog.component';
import { QrInvitationDialogComponent } from './events/qr-invitation-dialog/qr-invitation-dialog.component';
import { FeedbackDialogComponent } from './events/feedback-dialog/feedback-dialog.component';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatOptionModule } from '@angular/material/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { JobOffersComponent } from './job-offers/job-offers.component';
import { JobDetailsComponent } from './job-details/job-details.component';
import { SavedOffersComponent } from './saved-offers/saved-offers.component';
import { StudentPreferencesComponent } from './student-preferences/student-preferences.component';
import { ChatbotComponent } from './chatbot/chatbot.component';
import { EvaluationsListComponent } from './evaluations-list/evaluations-list.component';
import { TakeEvaluationComponent } from './take-evaluation/take-evaluation.component';
import { EvaluationResultsComponent } from './evaluation-results/evaluation-results.component';
import { DeadlineWarningDialogComponent } from './evaluations-list/deadline-warning-dialog.component';
import { SmartNotebookComponent } from './smart-notebook/smart-notebook.component';
import { ChapterLearningContentComponent } from './courses/chapter-learning-content/chapter-learning-content.component';
import { PresentationPlayerComponent } from './courses/presentation-player/presentation-player.component';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { KanbanComponent } from './kanban/kanban.component';
import { RecordingsComponent } from './recordings/recordings.component';
import { MessagesComponent } from './messages/messages.component';
import { MessagingDockComponent } from './messaging-dock/messaging-dock.component';
import { FeedComponent } from './feed/feed.component';


@NgModule({
  declarations: [
    DashboardComponent,
    FeedComponent,
    ProfileComponent,
    CoursesComponent,
    ChatComponent,
    LayoutComponent,
    ReclamationsComponent,
    EventsComponent,
    ClubsComponent,
    ClubChatComponent,
    ClubFeedbackComponent,
    ClubPostsComponent,
    PassportCompetencesComponent,
    JoinClubDialogComponent,
    QrInvitationDialogComponent,
    FeedbackDialogComponent,
    JobOffersComponent,
    JobDetailsComponent,
    SavedOffersComponent,
    StudentPreferencesComponent,
    ChatbotComponent,
    EvaluationsListComponent,
    TakeEvaluationComponent,
    EvaluationResultsComponent,
    DeadlineWarningDialogComponent,
    SmartNotebookComponent,
    ChapterLearningContentComponent,
    PresentationPlayerComponent,
    KanbanComponent,
    RecordingsComponent,
    MessagesComponent,
    MessagingDockComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    FrontofficeRoutingModule,
    SharedModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSnackBarModule,
    MatIconModule,
    MatOptionModule,
    MatDialogModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatTabsModule,
    MatCheckboxModule,
    DragDropModule
  ]
})
export class FrontofficeModule { }
