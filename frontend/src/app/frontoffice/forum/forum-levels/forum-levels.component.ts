import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { ForumTopicService, ForumTopic } from '../../../core/services/forum-topic.service';
import { UserProfile, UserProfileService } from '../../../core/services/user-profile.service';
import { ForumMediaService } from '../../../core/services/forum-media.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FrontofficeIdentityService } from '../../../core/services/frontoffice-identity.service';
import {
  isForumLevelUnlockedForUser,
  parseCefrFromProfile
} from '../../../core/utils/cefr-level.util';

export interface LevelMeta {
  code: string;
  title: string;
  summary: string;
  focus: string;
  gradient: string;
}

@Component({
  selector: 'app-forum-levels',
  templateUrl: './forum-levels.component.html',
  styleUrls: ['./forum-levels.component.css']
})
export class ForumLevelsComponent implements OnInit, OnDestroy {
  readonly levelsMeta: LevelMeta[] = [
    {
      code: 'A1',
      title: 'Introductory',
      summary: 'First words, introductions, and simple guided situations.',
      focus: 'Alphabet, numbers, greetings, and short basic instructions.',
      gradient: 'linear-gradient(135deg, #e0f7fa 0%, #b2ebf2 50%, #80deea 100%)'
    },
    {
      code: 'A2',
      title: 'Elementary',
      summary: 'Daily-life exchanges, hobbies, and practical basic needs.',
      focus: 'Short messages, descriptions, and familiar text understanding.',
      gradient: 'linear-gradient(135deg, #e8f5e9 0%, #a5d6a7 45%, #66bb6a 100%)'
    },
    {
      code: 'B1',
      title: 'Intermediate',
      summary: 'Express ideas on personal and simple professional topics.',
      focus: 'Narrate events, give opinions, and follow a discussion.',
      gradient: 'linear-gradient(135deg, #fff8e1 0%, #ffe082 40%, #ffca28 100%)'
    },
    {
      code: 'B2',
      title: 'Upper-Intermediate',
      summary: 'Argue with fluency and understand more demanding content.',
      focus: 'Articles, debates, structured writing, nuance and register.',
      gradient: 'linear-gradient(135deg, #fce4ec 0%, #f48fb1 45%, #ec407a 100%)'
    },
    {
      code: 'C1',
      title: 'Advanced',
      summary: 'Strong command for professional and academic English.',
      focus: 'Summaries, presentations, formal language and implicit meaning.',
      gradient: 'linear-gradient(135deg, #ede7f6 0%, #b39ddb 40%, #7e57c2 100%)'
    },
    {
      code: 'C2',
      title: 'Mastery',
      summary: 'Near-native command on complex and nuanced content.',
      focus: 'Subtlety, humor, instant reformulation, all registers.',
      gradient: 'linear-gradient(135deg, #263238 0%, #455a64 40%, #78909c 100%)'
    }
  ];

  selectedLevel = '';
  selectedMeta: LevelMeta | null = null;
  topics: ForumTopic[] = [];
  filteredTopics: ForumTopic[] = [];
  loading = false;
  error = '';
  isPaid = true;
  currentUserId = 2;

  levelSpaceId: number | null = null;

  searchQuery = '';
  sortBy: 'createdAt' | 'title' | 'views' = 'createdAt';
  sortOrder: 'asc' | 'desc' = 'desc';

  userById: { [id: number]: UserProfile } = {};
  /** Profil connecté (niveau certifié / profil → englishLevel). */
  me: UserProfile | null = null;
  private identitySub?: Subscription;

  newTitle = '';
  newDescription = '';
  newCoverUrl = '';
  creating = false;
  coverUploading = false;

  constructor(
    private forumTopicService: ForumTopicService,
    private users: UserProfileService,
    private media: ForumMediaService,
    private snackBar: MatSnackBar,
    private identity: FrontofficeIdentityService
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.identity.getCurrentUserId();
    this.identitySub = this.identity.userId$.subscribe(uid => {
      this.currentUserId = uid;
      this.loadProfileAndApplyLevelGate();
    });
    this.loadProfileAndApplyLevelGate();
    this.checkPayment();
  }

  ngOnDestroy(): void {
    this.identitySub?.unsubscribe();
  }

  private loadProfileAndApplyLevelGate(): void {
    this.users.getById(this.currentUserId).subscribe({
      next: u => {
        this.me = u;
        const role = (u.accountRole || 'STUDENT').toUpperCase();
        if (role === 'STUDENT') {
          const lvl = parseCefrFromProfile(u.englishLevel) ?? 'A1';
          this.selectLevel(lvl);
        } else {
          this.selectedLevel = '';
          this.selectedMeta = null;
          this.topics = [];
          this.filteredTopics = [];
          this.levelSpaceId = null;
          this.loading = false;
          this.error = '';
        }
      },
      error: () => {
        this.me = null;
        this.selectLevel('A1');
      }
    });
  }

  isStudentViewer(): boolean {
    return (this.me?.accountRole || 'STUDENT').toUpperCase() === 'STUDENT';
  }

  isLevelLocked(levelCode: string): boolean {
    return !isForumLevelUnlockedForUser(levelCode, this.me?.accountRole, this.me?.englishLevel);
  }

  studentUnlockedLevelLabel(): string {
    return parseCefrFromProfile(this.me?.englishLevel) ?? 'A1';
  }

  checkPayment(): void {
    // Level forum is intentionally enabled for all demo users.
    this.isPaid = true;
  }

  metaFor(code: string): LevelMeta | undefined {
    return this.levelsMeta.find(m => m.code === code);
  }

  onPickLevel(level: string): void {
    if (this.isLevelLocked(level)) {
      const mine = parseCefrFromProfile(this.me?.englishLevel);
      const msg = mine
        ? `Votre niveau enregistré est ${mine}. Seul cet espace forum vous est ouvert. Mettez à jour votre certificat ou votre profil pour changer de niveau.`
        : `Déposez un certificat (inscription) pour détecter votre niveau, ou complétez votre profil avec un niveau CECRL. En attendant, seul l’espace A1 est accessible.`;
      this.snackBar.open(msg, 'OK', { duration: 7000 });
      return;
    }
    this.selectLevel(level);
  }

  selectLevel(level: string): void {
    this.selectedLevel = level;
    this.selectedMeta = this.metaFor(level) ?? null;
    this.error = '';
    this.topics = [];
    this.filteredTopics = [];
    this.levelSpaceId = null;
    this.loading = true;

    // Load topics first so level access does not depend on resolving write-space metadata.
    this.fetchTopicsFor(level);
    // Best effort: resolve level space id for topic creation.
    this.forumTopicService.getLevelSpace(level, this.currentUserId).subscribe({
      next: space => (this.levelSpaceId = space.id ?? null),
      error: () => (this.levelSpaceId = null)
    });
  }

  private fetchTopicsFor(level: string): void {
    this.loading = true;
    this.forumTopicService.getTopicsByLevel(level, this.currentUserId).subscribe({
      next: data => {
        this.topics = Array.isArray(data) ? data : [];
        this.hydrateAuthors();
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load this level right now.';
        this.loading = false;
      }
    });
  }

  private hydrateAuthors(): void {
    const ids = [...new Set(this.topics.map(t => t.authorId).filter((id): id is number => id != null))];
    if (!ids.length) {
      return;
    }
    this.users.lookup(ids).subscribe({
      next: list => {
        this.userById = {};
        list.forEach(u => {
          if (u.id != null) {
            this.userById[u.id] = u;
          }
        });
      },
      error: () => {}
    });
  }

  authorName(authorId: number | undefined): string {
    if (authorId == null) {
      return '?';
    }
    const u = this.userById[authorId];
    if (u?.firstName?.trim() || u?.lastName?.trim()) {
      return [u.firstName, u.lastName].filter(Boolean).join(' ');
    }
      return 'Member';
  }

  initials(authorId: number | undefined): string {
    if (authorId == null) {
      return '?';
    }
    const u = this.userById[authorId];
    const f = u?.firstName?.trim();
    const l = u?.lastName?.trim();
    if (f && l) {
      return (f[0] + l[0]).toUpperCase();
    }
    if (f) {
      return f.slice(0, 2).toUpperCase();
    }
    return String(authorId).slice(0, 2);
  }

  authorRole(authorId: number | undefined): string | null {
    if (authorId == null) {
      return null;
    }
    const r = (this.userById[authorId]?.accountRole || '').toUpperCase();
    if (r === 'TUTOR') {
      return 'Tutor';
    }
    if (r === 'ADMIN') {
      return 'Team';
    }
    return 'Student';
  }

  applyFilters(): void {
    let list = [...this.topics];
    const q = this.searchQuery.trim().toLowerCase();
    if (q) {
      list = list.filter(
        t =>
          t.title.toLowerCase().includes(q) ||
          (t.description || '').toLowerCase().includes(q)
      );
    }
    list.sort((a, b) => {
      const pa = a.pinned ? 1 : 0;
      const pb = b.pinned ? 1 : 0;
      if (pb !== pa) {
        return pb - pa;
      }
      let av: number | string = 0;
      let bv: number | string = 0;
      switch (this.sortBy) {
        case 'title':
          av = a.title.toLowerCase();
          bv = b.title.toLowerCase();
          break;
        case 'views':
          av = a.views;
          bv = b.views;
          break;
        default:
          av = new Date(a.createdAt || '').getTime();
          bv = new Date(b.createdAt || '').getTime();
      }
      const cmp = av < bv ? -1 : av > bv ? 1 : 0;
      return this.sortOrder === 'asc' ? cmp : -cmp;
    });
    this.filteredTopics = list;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onSortChange(): void {
    this.applyFilters();
  }

  toggleSortOrder(): void {
    this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
    this.applyFilters();
  }

  createTopic(): void {
    if (!this.isPaid || this.levelSpaceId == null || this.creating) {
      return;
    }
    const title = this.newTitle.trim();
    const description = this.newDescription.trim();
    if (!title || !description) {
      return;
    }
    this.creating = true;
    this.forumTopicService
      .createTopicInSpace(this.levelSpaceId, {
        userId: this.currentUserId,
        authorId: this.currentUserId,
        title,
        description,
        coverImageUrl: this.newCoverUrl.trim() || undefined
      })
      .subscribe({
        next: () => {
          this.newTitle = '';
          this.newDescription = '';
          this.newCoverUrl = '';
          this.creating = false;
          this.snackBar.open('Topic published', 'OK', { duration: 2500 });
          this.fetchTopicsFor(this.selectedLevel);
        },
        error: err => {
          this.creating = false;
          const body = err?.error;
          const msg =
            typeof body === 'string' && body.trim()
              ? body.trim()
              : body?.message || err?.message || 'Publishing failed. Check forum (8040) and try again.';
          this.snackBar.open(msg, 'OK', { duration: 7000 });
        }
      });
  }

  onCoverFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.coverUploading = true;
    this.media.upload(file).subscribe({
      next: res => {
        this.newCoverUrl = res.url;
        this.coverUploading = false;
        input.value = '';
      },
      error: err => {
        this.coverUploading = false;
        input.value = '';
        this.snackBar.open(this.media.describeUploadError(err), 'OK', { duration: 6000 });
      }
    });
  }
}
