import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { FrontofficeRoutingModule } from './frontoffice-routing.module';
import { SharedModule } from '../shared/shared.module';

// Composants existants
import { LayoutComponent } from './layout/layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProfileComponent } from './profile/profile.component';
import { CoursesComponent } from './courses/courses.component';
import { ChatComponent } from './chat/chat.component';

// Nouveaux composants
import { JobOffersComponent } from './job-offers/job-offers.component';
import { JobDetailsComponent } from './job-details/job-details.component';
import { StudentPreferencesComponent } from './student-preferences/student-preferences.component';



// Angular Material
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { SavedOffersComponent } from './saved-offers/saved-offers.component';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';


@NgModule({
  declarations: [
    LayoutComponent,
    DashboardComponent,
    ProfileComponent,
    CoursesComponent,
    ChatComponent,
    JobOffersComponent,   // ← NOUVEAU
    JobDetailsComponent, SavedOffersComponent, StudentPreferencesComponent   // ← NOUVEAU
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    FrontofficeRoutingModule,
    SharedModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatChipsModule
  ]
})
export class FrontofficeModule { }
