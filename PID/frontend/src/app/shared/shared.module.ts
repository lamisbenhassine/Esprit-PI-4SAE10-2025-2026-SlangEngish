import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from './navbar/navbar.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
import { ClubAvisDialogComponent } from './club-avis-dialog/club-avis-dialog.component';
import { PaginationComponent } from './pagination/pagination.component';
import { ToastComponent } from './toast/toast.component';
import { SafeUrlPipe } from './safe-url.pipe';
import { TopicPostTranslateBlockComponent } from './topic-post-translate-block/topic-post-translate-block.component';
import { ForumSelectionAssistComponent } from './forum-selection-assist/forum-selection-assist.component';
import { ComposeWordSuggestDirective } from './compose-word-suggest.directive';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';



@NgModule({
  declarations: [
    NavbarComponent,
    SidebarComponent,
    TopicPostTranslateBlockComponent,
    ForumSelectionAssistComponent,
    ComposeWordSuggestDirective,
    ClubAvisDialogComponent,
    PaginationComponent,
    ToastComponent,
    SafeUrlPipe
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatBadgeModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule,
    MatProgressBarModule,
    MatCardModule,
    MatCheckboxModule,
    MatDialogModule,
    MatSelectModule
  ],
  exports: [
    RouterModule,
    NavbarComponent,
    SidebarComponent,
    TopicPostTranslateBlockComponent,
    ForumSelectionAssistComponent,
    ComposeWordSuggestDirective,
    ClubAvisDialogComponent,
    PaginationComponent,
    ToastComponent,
    SafeUrlPipe,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatToolbarModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule,
    MatProgressBarModule,
    MatCardModule,
    MatCheckboxModule,
    MatDialogModule,
    MatSelectModule,
    MatBadgeModule,
    MatProgressSpinnerModule
  ]
})
export class SharedModule { }
