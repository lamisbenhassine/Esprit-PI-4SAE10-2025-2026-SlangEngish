import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProfileComponent } from './profile/profile.component';
import { CoursesComponent } from './courses/courses.component';
import { ChatComponent } from './chat/chat.component';
import { LiveStreamComponent } from './live-stream/live-stream.component';
import { RecordingsComponent } from './recordings/recordings.component';
import { KanbanComponent } from './kanban/kanban.component';

const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'profile', component: ProfileComponent },
      { path: 'courses', component: CoursesComponent },
      {
        path: 'course/:courseId/chapter/:chapterId/pdf',
        loadChildren: () => import('./courses/pdf-viewer.module').then(m => m.PdfChapterViewerModule)
      },
      { path: 'chat', component: ChatComponent },
      { path: 'live-stream', component: LiveStreamComponent },
      { path: 'recordings', component: RecordingsComponent },
      { path: 'kanban', component: KanbanComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FrontofficeRoutingModule { }
