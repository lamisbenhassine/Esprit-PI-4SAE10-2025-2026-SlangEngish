import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { PdfChapterViewerComponent } from './pdf-chapter-viewer/pdf-chapter-viewer.component';
import { ChapterNotebookComponent } from './chapter-notebook/chapter-notebook.component';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ChapterLearningContentComponent } from './chapter-learning-content/chapter-learning-content.component';
import { PresentationPlayerComponent } from './presentation-player/presentation-player.component';

const routes: Routes = [
  { path: '', component: PdfChapterViewerComponent }
];

@NgModule({
  declarations: [
    PdfChapterViewerComponent,
    ChapterNotebookComponent,
    ChapterLearningContentComponent,
    PresentationPlayerComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild(routes),
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    MatDialogModule,
    MatProgressBarModule
  ]
})
export class PdfViewerRoutingModule { }

@NgModule({
  imports: [
    PdfViewerRoutingModule
  ]
})
export class PdfChapterViewerModule { }
