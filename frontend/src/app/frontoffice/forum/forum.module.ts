import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ForumGeneralComponent } from './forum-general/forum-general.component';
import { ForumLevelsComponent } from './forum-levels/forum-levels.component';
import { TopicDetailComponent } from './topic-detail/topic-detail.component';

const routes: Routes = [
    { path: 'general', component: ForumGeneralComponent },
    { path: 'levels', component: ForumLevelsComponent },
    { path: 'topic/:id', component: TopicDetailComponent }
];

@NgModule({
    declarations: [
        ForumGeneralComponent,
        ForumLevelsComponent,
        TopicDetailComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatIconModule,
        MatButtonModule,
        MatTooltipModule
    ]
})
export class ForumModule { }
