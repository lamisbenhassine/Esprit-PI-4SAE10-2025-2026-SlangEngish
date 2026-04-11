import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { FrontofficeRoutingModule } from './frontoffice-routing.module';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProfileComponent } from './profile/profile.component';
import { CoursesComponent } from './courses/courses.component';
import { MessagesComponent } from './messages/messages.component';
import { LayoutComponent } from './layout/layout.component';
import { MessagingDockComponent } from './messaging-dock/messaging-dock.component';
import { FeedComponent } from './feed/feed.component';
import { SharedModule } from '../shared/shared.module';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';

@NgModule({
  declarations: [
    DashboardComponent,
    ProfileComponent,
    CoursesComponent,
    MessagesComponent,
    LayoutComponent,
    FeedComponent,
    MessagingDockComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    FrontofficeRoutingModule,
    SharedModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatTabsModule
  ]
})
export class FrontofficeModule { }
