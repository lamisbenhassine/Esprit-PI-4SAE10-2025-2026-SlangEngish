import { Component, OnDestroy, OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Router } from '@angular/router';
import {
  SocialFeedService,
  FeedPost,
  ForumInvitation
} from '../../core/services/social-feed.service';
import { ForumTopicService } from '../../core/services/forum-topic.service';
import { ForumMediaService } from '../../core/services/forum-media.service';
import { ForumMessageService, ForumMessage, CreateMessageRequest } from '../../core/services/forum-message.service';
import { UserProfileService, UserProfile } from '../../core/services/user-profile.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FrontofficeIdentityService } from '../../core/services/frontoffice-identity.service';
import { apiErrorMessage, ComposeAssistService } from '../../core/services/compose-assist.service';

export interface FeedCommentAttachment {
  type: 'image' | 'video' | 'audio';
  url: string;
}

@Component({
  selector: 'app-feed',
  templateUrl: './feed.component.html',
  styleUrls: ['./feed.component.css']
})
export class FeedComponent implements OnInit, OnDestroy {
  currentUserId = 2;
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
  /** Panneau commentaires ouvert par sujet. */
  commentOpen: { [topicId: number]: boolean } = {};
  commentText: { [topicId: number]: string } = {};
  commentImageUrl: { [topicId: number]: string } = {};
  commentAudioUrl: { [topicId: number]: string } = {};
  commentBusy: { [topicId: number]: boolean } = {};
  commentAuthorMap: { [userId: number]: string } = {};
  recordingPostId: number | null = null;
  private mediaRecorder: MediaRecorder | null = null;
  private recordStream: MediaStream | null = null;
  private recordChunks: Blob[] = [];
  /** Profil de l’utilisateur connecté (zone « Créer une publication »). */
  me: UserProfile | null = null;

  constructor(
    private socialFeed: SocialFeedService,
    private forumTopic: ForumTopicService,
    private media: ForumMediaService,
    private messages: ForumMessageService,
    private users: UserProfileService,
    private identity: FrontofficeIdentityService,
    private snackBar: MatSnackBar,
    private router: Router,
    private sanitizer: DomSanitizer,
    private composeAssist: ComposeAssistService
  ) {}

  polishFeedCompose(): void {
    const title$ = this.composeTitle.trim()
      ? this.composeAssist.smartPolish$(this.composeTitle)
      : of({ text: this.composeTitle, source: 'local' as const });
    const cap$ = this.composeCaption.trim()
      ? this.composeAssist.smartPolish$(this.composeCaption)
      : of({ text: this.composeCaption, source: 'local' as const });
    forkJoin({ t: title$, c: cap$ }).subscribe({
      next: ({ t, c }) => {
        this.composeTitle = t.text;
        this.composeCaption = c.text;
        const ai = t.source === 'ai' || c.source === 'ai';
        this.snackBar.open(
          ai ? 'Texte amélioré (IA).' : 'Corrections locales (sans IA).',
          'OK',
          { duration: 2800 }
        );
      },
      error: err =>
        this.snackBar.open(apiErrorMessage(err, this.composeAssist.profanityHint), 'OK', { duration: 6000 })
    });
  }

  polishComment(post: FeedPost): void {
    const cur = this.commentText[post.id] || '';
    this.composeAssist.smartPolish$(cur).subscribe({
      next: ({ text, source }) => {
        this.commentText[post.id] = text;
        this.snackBar.open(
          source === 'ai' ? 'Commentaire amélioré (IA).' : 'Corrections locales (sans IA).',
          'OK',
          { duration: 2500 }
        );
      },
      error: err =>
        this.snackBar.open(apiErrorMessage(err, this.composeAssist.profanityHint), 'OK', { duration: 6000 })
    });
  }

  ngOnInit(): void {
    this.currentUserId = this.identity.getCurrentUserId();
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
        this.snackBar.open('Unable to load feed.', 'OK', { duration: 4000 });
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
          this.snackBar.open('Post created.', 'OK', { duration: 2500 });
          this.refreshAll();
        },
        error: err => {
          this.composing = false;
          this.snackBar.open(apiErrorMessage(err, 'Failed to publish post.'), 'OK', { duration: 6000 });
        }
      });
  }

  toggleLike(post: FeedPost): void {
    this.socialFeed.toggleLike(post.id, this.currentUserId).subscribe({
      next: r => {
        post.likedByViewer = r.liked;
        post.likeCount = r.likeCount;
      },
      error: () => this.snackBar.open('Failed to update like.', 'OK', { duration: 3000 })
    });
  }

  toggleRepost(post: FeedPost): void {
    this.socialFeed.toggleRepost(post.id, this.currentUserId).subscribe({
      next: r => {
        post.repostedByViewer = r.reposted;
        post.repostCount = r.repostCount;
      },
      error: () => this.snackBar.open('Failed to update repost.', 'OK', { duration: 3000 })
    });
  }

  openTopic(id: number): void {
    this.router.navigate(['/frontoffice/forum/topic', id]);
  }

  ngOnDestroy(): void {
    this.stopRecordingCleanup();
  }

  toggleComments(post: FeedPost): void {
    const id = post.id;
    if (this.commentOpen[id]) {
      this.commentOpen[id] = false;
      return;
    }
    this.commentOpen[id] = true;
    this.reloadComments(post);
  }

  reloadComments(post: FeedPost): void {
    this.loadingComments[post.id] = true;
    this.messages.getMessagesByTopic(post.id, this.currentUserId).subscribe({
      next: msgs => {
        const roots = (msgs || []).filter(m => !m.parentMessageId).slice(0, 12);
        this.expandedComments[post.id] = roots;
        this.loadingComments[post.id] = false;
        this.hydrateCommentAuthors(roots);
      },
      error: () => {
        this.loadingComments[post.id] = false;
      }
    });
  }

  private hydrateCommentAuthors(roots: ForumMessage[]): void {
    const ids = [...new Set(roots.map(m => m.authorId).filter((x): x is number => x != null))];
    if (!ids.length) {
      return;
    }
    this.users.lookup(ids).subscribe({
      next: list => {
        list.forEach(u => {
          if (u.id != null) {
            const name = [u.firstName, u.lastName].filter(Boolean).join(' ').trim();
            this.commentAuthorMap[u.id] = name || `User #${u.id}`;
          }
        });
      },
      error: () => {}
    });
  }

  /**
   * Aligner le feed avec la page détail :
   * tout utilisateur front-office connecté peut publier un commentaire.
   */
  canStudentComment(): boolean {
    return this.currentUserId > 0;
  }

  parseCommentAttachments(msg: ForumMessage): FeedCommentAttachment[] {
    if (!msg.attachments?.trim()) {
      return [];
    }
    try {
      const raw = JSON.parse(msg.attachments) as unknown;
      if (!Array.isArray(raw)) {
        return [];
      }
      return raw.filter(
        (a): a is FeedCommentAttachment =>
          !!a &&
          typeof a === 'object' &&
          (a as FeedCommentAttachment).url != null &&
          ['image', 'video', 'audio'].includes(String((a as FeedCommentAttachment).type))
      ) as FeedCommentAttachment[];
    } catch {
      return [];
    }
  }

  commentAuthorDisplay(authorId: number): string {
    if (this.commentAuthorMap[authorId]) {
      return this.commentAuthorMap[authorId];
    }
    return this.commentAuthor(authorId);
  }

  onCommentImageFile(ev: Event, post: FeedPost): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.commentBusy[post.id] = true;
    this.media.upload(file).subscribe({
      next: res => {
        this.commentImageUrl[post.id] = res.url;
        this.commentBusy[post.id] = false;
        input.value = '';
      },
      error: err => {
        this.commentBusy[post.id] = false;
        input.value = '';
        this.snackBar.open(this.media.describeUploadError(err), 'OK', { duration: 6000 });
      }
    });
  }

  clearCommentImage(post: FeedPost): void {
    delete this.commentImageUrl[post.id];
  }

  clearCommentAudio(post: FeedPost): void {
    delete this.commentAudioUrl[post.id];
  }

  startVoiceRecording(post: FeedPost): void {
    if (!isBrowserMediaSupported()) {
      this.snackBar.open('Enregistrement vocal non pris en charge par ce navigateur.', 'OK', { duration: 5000 });
      return;
    }
    this.stopRecordingCleanup();
    navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
      this.recordStream = stream;
      this.recordChunks = [];
      const mr = new MediaRecorder(stream);
      this.mediaRecorder = mr;
      this.recordingPostId = post.id;
      mr.ondataavailable = e => {
        if (e.data.size) {
          this.recordChunks.push(e.data);
        }
      };
      mr.onstop = () => {
        const blob = new Blob(this.recordChunks, { type: mr.mimeType || 'audio/webm' });
        const ext = blob.type.includes('webm') ? 'webm' : 'mp3';
        const file = new File([blob], `voice.${ext}`, { type: blob.type || 'audio/webm' });
        this.commentBusy[post.id] = true;
        this.media.upload(file).subscribe({
          next: res => {
            this.commentAudioUrl[post.id] = res.url;
            this.commentBusy[post.id] = false;
            this.recordingPostId = null;
          },
          error: err => {
            this.commentBusy[post.id] = false;
            this.recordingPostId = null;
            this.snackBar.open(this.media.describeUploadError(err), 'OK', { duration: 6000 });
          }
        });
        stream.getTracks().forEach(t => t.stop());
        this.recordStream = null;
      };
      mr.start();
    }).catch(() => {
      this.snackBar.open('Accès au micro refusé ou indisponible.', 'OK', { duration: 5000 });
    });
  }

  stopVoiceRecording(): void {
    if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
      this.mediaRecorder.stop();
    }
    this.mediaRecorder = null;
  }

  private stopRecordingCleanup(): void {
    if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
      this.mediaRecorder.stop();
    }
    this.mediaRecorder = null;
    if (this.recordStream) {
      this.recordStream.getTracks().forEach(t => t.stop());
      this.recordStream = null;
    }
    this.recordingPostId = null;
  }

  isRecording(post: FeedPost): boolean {
    return this.recordingPostId === post.id;
  }

  sendComment(post: FeedPost): void {
    if (!this.canStudentComment()) {
      return;
    }
    if (post.locked) {
      this.snackBar.open('Ce sujet est verrouillé.', 'OK', { duration: 3000 });
      return;
    }
    const text = (this.commentText[post.id] || '').trim();
    const img = this.commentImageUrl[post.id]?.trim();
    const aud = this.commentAudioUrl[post.id]?.trim();
    if (!text && !img && !aud) {
      this.snackBar.open('Ajoutez du texte, une image ou un message vocal.', 'OK', { duration: 3000 });
      return;
    }
    const parts: { type: string; url: string }[] = [];
    if (img) {
      parts.push({ type: 'image', url: img });
    }
    if (aud) {
      parts.push({ type: 'audio', url: aud });
    }
    const req: CreateMessageRequest = {
      topicId: post.id,
      authorId: this.currentUserId,
      content: text || ' ',
      parentMessageId: null,
      attachments: parts.length ? JSON.stringify(parts) : undefined
    };
    this.commentBusy[post.id] = true;
    this.messages.createMessage(req).subscribe({
      next: () => {
        this.commentBusy[post.id] = false;
        this.commentText[post.id] = '';
        delete this.commentImageUrl[post.id];
        delete this.commentAudioUrl[post.id];
        post.commentCount = (post.commentCount || 0) + 1;
        this.snackBar.open('Commentaire publié.', 'OK', { duration: 2500 });
        this.reloadComments(post);
      },
      error: err => {
        this.commentBusy[post.id] = false;
        this.snackBar.open(apiErrorMessage(err, 'Envoi impossible.'), 'OK', { duration: 6000 });
      }
    });
  }

  sendInvitation(): void {
    const to = Number(this.inviteToId);
    if (!to || to === this.currentUserId) {
      return;
    }
    this.socialFeed.sendInvitation(this.currentUserId, to, this.inviteMessage || 'Join me on Slang English!').subscribe({
      next: () => {
        this.inviteToId = '';
        this.inviteMessage = '';
        this.snackBar.open('Invitation sent.', 'OK', { duration: 2500 });
      },
      error: () => this.snackBar.open('Failed to send invitation.', 'OK', { duration: 4000 })
    });
  }

  respond(inv: ForumInvitation, accept: boolean): void {
    if (!inv.id) {
      return;
    }
    this.socialFeed.respondInvitation(inv.id, this.currentUserId, accept).subscribe({
      next: () => {
        this.invitations = this.invitations.filter(i => i.id !== inv.id);
        this.snackBar.open(accept ? 'Invitation accepted.' : 'Invitation declined.', 'OK', { duration: 2500 });
      },
      error: () => this.snackBar.open('Operation failed.', 'OK', { duration: 3000 })
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
    return 'Member';
  }

  authorRoleLabel(post: FeedPost): string | null {
    const r = (post.authorRole || '').toUpperCase();
    if (r === 'TUTOR') {
      return 'Tutor';
    }
    if (r === 'ADMIN') {
      return 'Team';
    }
    if (r === 'STUDENT') {
      return 'Student';
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
    return 'You';
  }

  commentAuthor(authorId: number): string {
    const known = this.feed.find(p => p.authorId === authorId);
    if (known) {
      return this.authorFullName(known);
    }
    return 'Member';
  }
}

function isBrowserMediaSupported(): boolean {
  return (
    typeof window !== 'undefined' &&
    !!navigator.mediaDevices?.getUserMedia &&
    typeof MediaRecorder !== 'undefined'
  );
}
