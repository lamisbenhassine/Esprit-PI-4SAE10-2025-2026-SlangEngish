import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { FrontofficeRoutingModule } from './frontoffice-routing.module';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProfileComponent } from './profile/profile.component';
import { CoursesComponent } from './courses/courses.component';
import { ChatComponent } from './chat/chat.component';
import { LayoutComponent } from './layout/layout.component';
import { EvaluationsListComponent } from './evaluations-list/evaluations-list.component';
import { TakeEvaluationComponent } from './take-evaluation/take-evaluation.component';
import { EvaluationResultsComponent } from './evaluation-results/evaluation-results.component';
import { SharedModule } from '../shared/shared.module';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@NgModule({
  declarations: [
    DashboardComponent,
    ProfileComponent,
    CoursesComponent,
    ChatComponent,
    LayoutComponent,
    EvaluationsListComponent,
    TakeEvaluationComponent,
    EvaluationResultsComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    FrontofficeRoutingModule,
    SharedModule,
    MatProgressBarModule
  ]
})
export class FrontofficeModule { }
