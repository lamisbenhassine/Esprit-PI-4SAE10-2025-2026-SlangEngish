import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Router } from '@angular/router';
import {
  SocialFeedService,
  FeedPost,
  ForumInvitation
} from '../../core/services/social-feed.service';
import { ForumTopicService } from '../../core/services/forum-topic.service';
import { ForumMediaService } from '../../core/services/forum-media.service';
import { ForumMessageService, ForumMessage } from '../../core/services/forum-message.service';
import { UserProfileService, UserProfile } from '../../core/services/user-profile.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-feed',
  templateUrl: './feed.component.html',
  styleUrls: ['./feed.component.css']
})
export class FeedComponent implements OnInit {
  currentUserId = 1;
  feed: FeedPost[] = [];
  invitations: ForumInvitation[] = [];
  loading = true;
  inviteToId = '';
  inviteMessage = '';
  generalSpaceId: number | null = null;

  composeTitle = '';
  composeCaption = '';
  composeImageUrl = '';
  composeVideoUrl = '';
  composing = false;
  coverBusy = false;
  expandedComments: { [topicId: number]: ForumMessage[] } = {};
  loadingComments: { [topicId: number]: boolean } = {};
  /** Profil de l’utilisateur connecté (zone « Créer une publication »). */
  me: UserProfile | null = null;

  constructor(
    private socialFeed: SocialFeedService,
    private forumTopic: ForumTopicService,
    private media: ForumMediaService,
    private messages: ForumMessageService,
    private users: UserProfileService,
    private snackBar: MatSnackBar,
    private router: Router,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.users.getById(this.currentUserId).subscribe({
      next: u => (this.me = u),
      error: () => (this.me = null)
    });
    this.forumTopic.getGeneralSpace().subscribe({
      next: s => (this.generalSpaceId = s.id ?? null),
      error: () => (this.generalSpaceId = null)
    });
    this.refreshAll();
  }

  refreshAll(): void {
    this.loading = true;
    this.socialFeed.getFeed(this.currentUserId).subscribe({
      next: f => {
        this.feed = Array.isArray(f) ? f : [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Impossible de charger le fil.', 'OK', { duration: 4000 });
      }
    });
    this.socialFeed.getInbox(this.currentUserId).subscribe({
      next: inv => (this.invitations = Array.isArray(inv) ? inv.filter(i => i.status === 'PENDING') : []),
      error: () => {}
    });
  }

  onMediaFile(ev: Event, kind: 'image' | 'video'): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.coverBusy = true;
    this.media.upload(file).subscribe({
      next: res => {
        if (kind === 'video' || file.type.startsWith('video/')) {
          this.composeVideoUrl = res.url;
        } else {
          this.composeImageUrl = res.url;
        }
        this.coverBusy = false;
        input.value = '';
      },
      error: err => {
        this.coverBusy = false;
        input.value = '';
        this.snackBar.open(this.media.describeUploadError(err), 'OK', { duration: 6000 });
      }
    });
  }

  publishPost(): void {
    if (this.generalSpaceId == null || !this.composeTitle.trim() || !this.composeCaption.trim()) {
      return;
    }
    this.composing = true;
    this.forumTopic
      .createTopicInSpace(this.generalSpaceId, {
        userId: this.currentUserId,
        authorId: this.currentUserId,
        title: this.composeTitle.trim(),
        description: this.composeCaption.trim(),
        coverImageUrl: this.composeImageUrl.trim() || undefined,
        coverVideoUrl: this.composeVideoUrl.trim() || undefined
      })
      .subscribe({
        next: () => {
          this.composeTitle = '';
          this.composeCaption = '';
          this.composeImageUrl = '';
          this.composeVideoUrl = '';
          this.composing = false;
          this.snackBar.open('Publication ajoutée', 'OK', { duration: 2500 });
          this.refreshAll();
        },
        error: () => {
          this.composing = false;
          this.snackBar.open('Erreur publication', 'OK', { duration: 4000 });
        }
      });
  }

  toggleLike(post: FeedPost): void {
    this.socialFeed.toggleLike(post.id, this.currentUserId).subscribe({
      next: r => {
        post.likedByViewer = r.liked;
        post.likeCount = r.likeCount;
      },
      error: () => this.snackBar.open('Erreur j’aime', 'OK', { duration: 3000 })
    });
  }

  toggleRepost(post: FeedPost): void {
    this.socialFeed.toggleRepost(post.id, this.currentUserId).subscribe({
      next: r => {
        post.repostedByViewer = r.reposted;
        post.repostCount = r.repostCount;
      },
      error: () => this.snackBar.open('Erreur republication', 'OK', { duration: 3000 })
    });
  }

  openTopic(id: number): void {
    this.router.navigate(['/frontoffice/forum/topic', id]);
  }

  toggleComments(post: FeedPost): void {
    if (this.expandedComments[post.id]) {
      delete this.expandedComments[post.id];
      return;
    }
    this.loadingComments[post.id] = true;
    this.messages.getMessagesByTopic(post.id, this.currentUserId).subscribe({
      next: msgs => {
        const roots = (msgs || []).filter(m => !m.parentMessageId).slice(0, 5);
        this.expandedComments[post.id] = roots;
        this.loadingComments[post.id] = false;
      },
      error: () => {
        this.loadingComments[post.id] = false;
      }
    });
  }

  sendInvitation(): void {
    const to = Number(this.inviteToId);
    if (!to || to === this.currentUserId) {
      return;
    }
    this.socialFeed.sendInvitation(this.currentUserId, to, this.inviteMessage || 'Rejoins-moi sur Slang English !').subscribe({
      next: () => {
        this.inviteToId = '';
        this.inviteMessage = '';
        this.snackBar.open('Invitation envoyée', 'OK', { duration: 2500 });
      },
      error: () => this.snackBar.open('Erreur envoi invitation', 'OK', { duration: 4000 })
    });
  }

  respond(inv: ForumInvitation, accept: boolean): void {
    if (!inv.id) {
      return;
    }
    this.socialFeed.respondInvitation(inv.id, this.currentUserId, accept).subscribe({
      next: () => {
        this.invitations = this.invitations.filter(i => i.id !== inv.id);
        this.snackBar.open(accept ? 'Invitation acceptée' : 'Invitation refusée', 'OK', { duration: 2500 });
      },
      error: () => this.snackBar.open('Erreur', 'OK', { duration: 3000 })
    });
  }

  youtubeEmbed(url: string | null | undefined): string | null {
    if (!url) {
      return null;
    }
    const m =
      url.match(/youtube\.com\/watch\?v=([a-zA-Z0-9_-]+)/) ||
      url.match(/youtu\.be\/([a-zA-Z0-9_-]+)/);
    return m ? `https://www.youtube.com/embed/${m[1]}` : null;
  }

  safeYoutube(url: string): SafeResourceUrl | null {
    const e = this.youtubeEmbed(url);
    return e ? this.sanitizer.bypassSecurityTrustResourceUrl(e) : null;
  }

  isYoutubeUrl(url: string | null | undefined): boolean {
    return !!this.youtubeEmbed(url || '');
  }

  canSendInvite(): boolean {
    const to = Number(this.inviteToId);
    return !!this.inviteToId && !Number.isNaN(to) && to !== this.currentUserId;
  }

  authorFullName(post: FeedPost): string {
    const f = post.authorFirstName?.trim();
    const l = post.authorLastName?.trim();
    if (f || l) {
      return [f, l].filter(Boolean).join(' ');
    }
    return `Utilisateur #${post.authorId}`;
  }

  authorRoleLabel(post: FeedPost): string | null {
    const r = (post.authorRole || '').toUpperCase();
    if (r === 'TUTOR') {
      return 'Tuteur';
    }
    if (r === 'ADMIN') {
      return 'Équipe';
    }
    if (r === 'STUDENT') {
      return 'Étudiant';
    }
    return null;
  }

  authorInitials(post: FeedPost): string {
    const f = post.authorFirstName?.trim();
    const l = post.authorLastName?.trim();
    if (f && l) {
      return (f[0] + l[0]).toUpperCase();
    }
    if (f) {
      return f.slice(0, 2).toUpperCase();
    }
    return String(post.authorId ?? '?').slice(0, 2);
  }

  composerInitials(): string {
    if (this.me?.firstName && this.me?.lastName) {
      return (this.me.firstName[0] + this.me.lastName[0]).toUpperCase();
    }
    return String(this.currentUserId);
  }

  composerLabel(): string {
    if (this.me?.firstName || this.me?.lastName) {
      return [this.me?.firstName, this.me?.lastName].filter(Boolean).join(' ');
    }
    return `Vous (#${this.currentUserId})`;
  }
}
