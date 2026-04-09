import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ForumTopicService, ForumTopic } from '../../../core/services/forum-topic.service';
import { ForumMessageService, ForumMessage, CreateMessageRequest } from '../../../core/services/forum-message.service';
import { ForumBlockService } from '../../../core/services/forum-block.service';
import { ForumReportService } from '../../../core/services/forum-report.service';
import { ForumMediaService } from '../../../core/services/forum-media.service';
import { UserProfile, UserProfileService } from '../../../core/services/user-profile.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';

export interface ForumAttachment {
  type: 'image' | 'video';
  url: string;
}

@Component({
  selector: 'app-topic-detail',
  templateUrl: './topic-detail.component.html',
  styleUrls: ['./topic-detail.component.css']
})
export class TopicDetailComponent implements OnInit {
  topic: ForumTopic | null = null;
  messages: ForumMessage[] = [];
  messageReplies: { [key: number]: ForumMessage[] } = {};
  newMessageContent = '';
  replyContent = '';
  replyingTo: number | null = null;
  editingMessageId: number | null = null;
  editContent = '';
  editAttachmentsJson = '';
  currentUserId = 1;

  /** Médias optionnels pour le prochain message / réponse */
  composeImageUrl = '';
  composeVideoUrl = '';
  composeUploading = false;

  /** Cache prénom / nom / rôle par id utilisateur (microservice user). */
  userById: { [userId: number]: UserProfile } = {};

  constructor(
    private route: ActivatedRoute,
    private topicService: ForumTopicService,
    private messageService: ForumMessageService,
    private blockService: ForumBlockService,
    private reportService: ForumReportService,
    private mediaService: ForumMediaService,
    private users: UserProfileService,
    private snackBar: MatSnackBar,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.topicService.getTopicById(id).subscribe(t => {
      this.topic = t;
      this.loadMessages(id);
    });
  }

  loadTopic(id: number) {
    this.topicService.getTopicById(id).subscribe(t => {
      this.topic = t;
      this.hydrateUsers();
    });
  }

  loadMessages(topicId: number) {
    this.messageService.getMessagesByTopic(topicId, this.currentUserId).subscribe(msgs => {
      const roots = msgs.filter(m => !m.parentMessageId);
      if (!roots.length) {
        this.messages = [];
        this.messageReplies = {};
        this.hydrateUsers();
        return;
      }
      forkJoin(roots.map(m => this.messageService.getReplies(m.id!, this.currentUserId))).subscribe(groups => {
        this.messages = roots;
        roots.forEach((m, idx) => {
          this.messageReplies[m.id!] = groups[idx] || [];
        });
        this.hydrateUsers();
      });
    });
  }

  loadReplies(messageId: number) {
    this.messageService.getReplies(messageId, this.currentUserId).subscribe(replies => {
      this.messageReplies[messageId] = replies;
      this.hydrateUsers();
    });
  }

  private hydrateUsers() {
    const ids = new Set<number>();
    if (this.topic?.authorId != null) {
      ids.add(this.topic.authorId);
    }
    this.messages.forEach(m => ids.add(m.authorId));
    Object.values(this.messageReplies)
      .flat()
      .forEach(r => ids.add(r.authorId));
    if (!ids.size) {
      return;
    }
    this.users.lookup([...ids]).subscribe(list => {
      this.userById = {};
      list.forEach(u => {
        if (u.id != null) {
          this.userById[u.id] = u;
        }
      });
    });
  }

  displayName(authorId: number | null | undefined): string {
    if (authorId == null) {
      return '?';
    }
    const u = this.userById[authorId];
    if (u?.firstName?.trim() || u?.lastName?.trim()) {
      return [u.firstName, u.lastName].filter(Boolean).join(' ');
    }
    return `Utilisateur #${authorId}`;
  }

  userRoleLabel(authorId: number | null | undefined): string | null {
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
    if (r === 'STUDENT') {
      return 'Étudiant';
    }
    return null;
  }

  initials(authorId: number | null | undefined): string {
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

  parseAttachments(msg: ForumMessage): ForumAttachment[] {
    if (!msg.attachments || !msg.attachments.trim()) {
      return [];
    }
    try {
      const raw = JSON.parse(msg.attachments) as unknown;
      if (!Array.isArray(raw)) {
        return [];
      }
      return raw.filter(
        (a): a is ForumAttachment =>
          !!a && typeof a === 'object' && (a as ForumAttachment).url != null
      ) as ForumAttachment[];
    } catch {
      return [];
    }
  }

  youtubeEmbed(url: string): string | null {
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

  isYoutube(url: string): boolean {
    return !!this.youtubeEmbed(url);
  }

  private buildAttachmentsJson(): string | null {
    const parts: ForumAttachment[] = [];
    const img = this.composeImageUrl.trim();
    const vid = this.composeVideoUrl.trim();
    if (img) {
      parts.push({ type: 'image', url: img });
    }
    if (vid) {
      parts.push({ type: 'video', url: vid });
    }
    if (!parts.length) {
      return null;
    }
    return JSON.stringify(parts);
  }

  private clearComposeMedia() {
    this.composeImageUrl = '';
    this.composeVideoUrl = '';
  }

  onComposeFile(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.composeUploading = true;
    this.mediaService.upload(file).subscribe({
      next: res => {
        if (file.type.startsWith('video/')) {
          this.composeVideoUrl = res.url;
        } else {
          this.composeImageUrl = res.url;
        }
        this.composeUploading = false;
        this.snackBar.open('Média ajouté', 'OK', { duration: 2000 });
        input.value = '';
      },
      error: err => {
        this.composeUploading = false;
        this.snackBar.open(this.mediaService.describeUploadError(err), 'OK', { duration: 7000 });
        input.value = '';
      }
    });
  }

  postMessage() {
    if (!this.topic || !this.newMessageContent.trim() || this.topic.locked) {
      return;
    }
    const req: CreateMessageRequest = {
      topicId: this.topic.id!,
      authorId: this.currentUserId,
      content: this.newMessageContent,
      parentMessageId: null,
      attachments: this.buildAttachmentsJson() ?? undefined
    };
    this.messageService.createMessage(req).subscribe({
      next: () => {
        this.snackBar.open('Message publié', 'OK', { duration: 3000 });
        this.newMessageContent = '';
        this.clearComposeMedia();
        this.loadMessages(this.topic!.id!);
      },
      error: err => {
        const msg =
          err?.error?.error ||
          (typeof err?.error === 'string' ? err.error : null) ||
          'Erreur envoi';
        this.snackBar.open(msg, 'OK', { duration: 5000 });
      }
    });
  }

  replyTo(msgId: number) {
    this.replyingTo = msgId;
    this.replyContent = '';
  }

  submitReply(parentId: number) {
    if (!this.topic || !this.replyContent.trim() || this.topic.locked) {
      return;
    }
    const req: CreateMessageRequest = {
      topicId: this.topic.id!,
      authorId: this.currentUserId,
      content: this.replyContent,
      parentMessageId: parentId,
      attachments: this.buildAttachmentsJson() ?? undefined
    };
    this.messageService.createMessage(req).subscribe({
      next: () => {
        this.snackBar.open('Réponse envoyée', 'OK', { duration: 3000 });
        this.replyingTo = null;
        this.replyContent = '';
        this.clearComposeMedia();
        this.loadReplies(parentId);
      },
      error: err => {
        const msg = err?.error?.error || 'Erreur envoi';
        this.snackBar.open(msg, 'OK', { duration: 5000 });
      }
    });
  }

  startEdit(message: ForumMessage) {
    this.editingMessageId = message.id!;
    this.editContent = message.content;
    this.editAttachmentsJson = message.attachments || '';
  }

  saveEdit(messageId: number) {
    if (!this.editContent.trim()) {
      return;
    }
    const att = this.editAttachmentsJson.trim() || null;
    this.messageService.updateMessage(messageId, this.editContent, att).subscribe({
      next: () => {
        this.snackBar.open('Message mis à jour', 'OK', { duration: 3000 });
        this.editingMessageId = null;
        this.loadMessages(this.topic!.id!);
      },
      error: () => this.snackBar.open('Erreur mise à jour', 'OK', { duration: 3000 })
    });
  }

  deleteMessage(messageId: number) {
    if (confirm('Supprimer ce message ?')) {
      this.messageService.deleteMessage(messageId).subscribe({
        next: () => {
          this.snackBar.open('Supprimé', 'OK', { duration: 3000 });
          this.loadMessages(this.topic!.id!);
        },
        error: () => this.snackBar.open('Erreur suppression', 'OK', { duration: 3000 })
      });
    }
  }

  blockUser(targetAuthorId: number) {
    if (targetAuthorId === this.currentUserId) {
      return;
    }
    if (!confirm('Masquer tous les messages de cet utilisateur ?')) {
      return;
    }
    this.blockService.block(this.currentUserId, targetAuthorId).subscribe({
      next: () => {
        this.snackBar.open('Utilisateur bloqué', 'OK', { duration: 3000 });
        this.loadMessages(this.topic!.id!);
        this.loadTopic(this.topic!.id!);
      },
      error: () => this.snackBar.open('Déjà bloqué ou erreur', 'OK', { duration: 3000 })
    });
  }

  reportTopic() {
    if (!this.topic?.id) {
      return;
    }
    const reason = prompt('Raison du signalement (optionnel) :') ?? '';
    this.reportService.submit(this.currentUserId, 'TOPIC', this.topic.id, reason).subscribe({
      next: () => this.snackBar.open('Signalement envoyé', 'OK', { duration: 3000 }),
      error: () => this.snackBar.open('Erreur signalement', 'OK', { duration: 3000 })
    });
  }

  reportMessage(m: ForumMessage) {
    if (!m.id) {
      return;
    }
    const reason = prompt('Raison du signalement :') ?? '';
    this.reportService.submit(this.currentUserId, 'MESSAGE', m.id, reason).subscribe({
      next: () => this.snackBar.open('Signalement envoyé', 'OK', { duration: 3000 }),
      error: () => this.snackBar.open('Erreur signalement', 'OK', { duration: 3000 })
    });
  }
}
