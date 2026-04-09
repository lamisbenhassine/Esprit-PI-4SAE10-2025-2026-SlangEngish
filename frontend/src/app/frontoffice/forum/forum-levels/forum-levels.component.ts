import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ForumTopicService, ForumTopic } from '../../../core/services/forum-topic.service';
import { PaymentService } from '../../../core/services/payment.service';
import { UserProfile, UserProfileService } from '../../../core/services/user-profile.service';
import { ForumMediaService } from '../../../core/services/forum-media.service';
import { MatSnackBar } from '@angular/material/snack-bar';

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
export class ForumLevelsComponent implements OnInit {
  readonly levelsMeta: LevelMeta[] = [
    {
      code: 'A1',
      title: 'Introductif',
      summary: 'Premiers mots, présentations et situations très guidées.',
      focus: 'Alphabet, nombres, salutations, comprendre des consignes courtes.',
      gradient: 'linear-gradient(135deg, #e0f7fa 0%, #b2ebf2 50%, #80deea 100%)'
    },
    {
      code: 'A2',
      title: 'Élémentaire',
      summary: 'Échanges sur le quotidien, les loisirs et les besoins simples.',
      focus: 'Messages courts, descriptions, compréhension de textes familiers.',
      gradient: 'linear-gradient(135deg, #e8f5e9 0%, #a5d6a7 45%, #66bb6a 100%)'
    },
    {
      code: 'B1',
      title: 'Intermédiaire',
      summary: 'S’exprimer sur des sujets personnels et professionnels simples.',
      focus: 'Raconter un événement, donner un avis, suivre une conversation.',
      gradient: 'linear-gradient(135deg, #fff8e1 0%, #ffe082 40%, #ffca28 100%)'
    },
    {
      code: 'B2',
      title: 'Intermédiaire supérieur',
      summary: 'Argumenter avec fluidité et comprendre des contenus exigeants.',
      focus: 'Articles, débats, rédaction structurée, nuances et registres.',
      gradient: 'linear-gradient(135deg, #fce4ec 0%, #f48fb1 45%, #ec407a 100%)'
    },
    {
      code: 'C1',
      title: 'Avancé',
      summary: 'Maîtrise fine pour le travail et les études en anglais.',
      focus: 'Synthèses, présentations, langage soutenu et implicite.',
      gradient: 'linear-gradient(135deg, #ede7f6 0%, #b39ddb 40%, #7e57c2 100%)'
    },
    {
      code: 'C2',
      title: 'Maîtrise',
      summary: 'Niveau proche d’un locuteur natif sur des contenus complexes.',
      focus: 'Subtilités, humour, reformulation instantanée, tous registres.',
      gradient: 'linear-gradient(135deg, #263238 0%, #455a64 40%, #78909c 100%)'
    }
  ];

  selectedLevel = '';
  selectedMeta: LevelMeta | null = null;
  topics: ForumTopic[] = [];
  filteredTopics: ForumTopic[] = [];
  loading = false;
  error = '';
  isPaid = false;
  currentUserId = 1;

  levelSpaceId: number | null = null;

  searchQuery = '';
  sortBy: 'createdAt' | 'title' | 'views' = 'createdAt';
  sortOrder: 'asc' | 'desc' = 'desc';

  userById: { [id: number]: UserProfile } = {};

  newTitle = '';
  newDescription = '';
  newCoverUrl = '';
  creating = false;
  coverUploading = false;

  constructor(
    private forumTopicService: ForumTopicService,
    private paymentService: PaymentService,
    private users: UserProfileService,
    private media: ForumMediaService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.checkPayment();
  }

  checkPayment(): void {
    this.paymentService.verifyUserPayment(this.currentUserId).subscribe({
      next: paid => (this.isPaid = paid),
      error: () => (this.isPaid = false)
    });
  }

  metaFor(code: string): LevelMeta | undefined {
    return this.levelsMeta.find(m => m.code === code);
  }

  onPickLevel(level: string): void {
    if (!this.isPaid) {
      this.snackBar.open('Abonnement actif requis pour ouvrir un forum par niveau.', 'Offres', {
        duration: 5000
      }).onAction().subscribe(() => this.router.navigate(['/frontoffice/inscription/offers']));
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

    this.forumTopicService.getLevelSpace(level, this.currentUserId).subscribe({
      next: space => {
        this.levelSpaceId = space.id ?? null;
        this.fetchTopicsFor(level);
      },
      error: () => {
        this.loading = false;
        this.error = 'Impossible d’accéder à cet espace (abonnement ou serveur).';
        this.snackBar.open(this.error, 'OK', { duration: 5000 });
      }
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
        this.error = 'Accès refusé ou erreur serveur.';
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
    return `Utilisateur #${authorId}`;
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
      return 'Tuteur';
    }
    if (r === 'ADMIN') {
      return 'Équipe';
    }
    return null;
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
          this.snackBar.open('Sujet publié', 'OK', { duration: 2500 });
          this.fetchTopicsFor(this.selectedLevel);
        },
        error: () => {
          this.creating = false;
          this.snackBar.open('Publication impossible', 'OK', { duration: 4000 });
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
