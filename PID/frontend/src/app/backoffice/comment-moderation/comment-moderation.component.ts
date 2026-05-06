import { Component, OnDestroy, OnInit } from '@angular/core';
import { ForumModerationService, ModerationMessageRow } from '../../core/services/forum-moderation.service';
import { BackofficeTutorIdentityService } from '../../core/services/backoffice-tutor-identity.service';
import { UserProfile, UserProfileService } from '../../core/services/user-profile.service';
import { AuthService } from '../../services/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-comment-moderation',
  templateUrl: './comment-moderation.component.html',
  styleUrls: ['./comment-moderation.component.css']
})
export class CommentModerationComponent implements OnInit, OnDestroy {
  rows: ModerationMessageRow[] = [];
  loading = false;
  userById: { [id: number]: UserProfile } = {};
  displayedColumns = ['createdAt', 'topic', 'author', 'preview', 'actions'];
  private tutorSub?: Subscription;
  private authSub?: Subscription;

  constructor(
    private moderation: ForumModerationService,
    private tutorIdentity: BackofficeTutorIdentityService,
    private auth: AuthService,
    private users: UserProfileService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.tutorSub = this.tutorIdentity.tutorId$.subscribe(() => this.refresh());
    this.authSub = this.auth.currentUser$.subscribe(() => this.refresh());
    this.refresh();
  }

  ngOnDestroy(): void {
    this.tutorSub?.unsubscribe();
    this.authSub?.unsubscribe();
  }

  /**
   * Le backend vérifie que cet id existe et a le rôle TUTOR ou ADMIN.
   * On utilise l’utilisateur connecté (back-office) — pas l’id fictif 12 par défaut.
   */
  moderatorId(): number {
    const u = this.auth.getCurrentUser();
    if (u?.role === 'ADMIN' || u?.role === 'TUTOR') {
      const id = Number(u.id);
      if (Number.isFinite(id) && id > 0) {
        return id;
      }
    }
    return this.tutorIdentity.getTutorId();
  }

  refresh(): void {
    this.loading = true;
    this.moderation.listRecent(this.moderatorId(), 120).subscribe({
      next: list => {
        this.rows = Array.isArray(list) ? list : [];
        this.loading = false;
        this.hydrateUsers();
      },
      error: err => {
        this.loading = false;
        const status = err?.status;
        const e = err?.error as { error?: string; message?: string; detail?: string } | undefined;
        const body = e?.error || e?.detail || e?.message;
        let msg =
          typeof body === 'string'
            ? body
            : 'Impossible de charger les commentaires. Vérifiez le microservice forum (port 8040), le user-service (8011), puis recompilez et redémarrez le forum.';
        if (status === 404) {
          msg =
            'API introuvable (404). Redémarrez le microservice forum après compilation pour charger les routes /api/forum/messages/moderation/…';
        }
        if (status === 502 || status === 503) {
          msg =
            typeof body === 'string'
              ? body
              : 'Le forum ne peut pas joindre le service utilisateurs (8011). Démarrez le microservice users ou vérifiez user.api.base-url dans le forum.';
        }
        this.snackBar.open(msg, 'OK', { duration: 10000 });
        this.rows = [];
      }
    });
  }

  private hydrateUsers(): void {
    const ids = new Set<number>();
    this.rows.forEach(r => {
      if (r.authorId != null) {
        ids.add(r.authorId);
      }
    });
    if (!ids.size) {
      return;
    }
    this.users.lookup([...ids]).subscribe({
      next: list => {
        this.userById = {};
        list.forEach(u => {
          if (u.id != null) {
            this.userById[u.id] = u;
          }
        });
      },
      error: () => (this.userById = {})
    });
  }

  authorLabel(authorId: number): string {
    const u = this.userById[authorId];
    if (u?.firstName || u?.lastName) {
      return [u.firstName, u.lastName].filter(Boolean).join(' ');
    }
    return `Utilisateur #${authorId}`;
  }

  previewContent(row: ModerationMessageRow): string {
    const t = (row.content || '').trim();
    return t.length > 160 ? t.slice(0, 157) + '…' : t || '(pièce jointe)';
  }

  deleteRow(row: ModerationMessageRow): void {
    if (!row.id) {
      return;
    }
    if (!confirm('Supprimer ce commentaire (et ses réponses éventuelles) ?')) {
      return;
    }
    this.moderation.deleteMessage(row.id, this.moderatorId()).subscribe({
      next: () => {
        this.snackBar.open('Commentaire supprimé.', 'OK', { duration: 3000 });
        this.refresh();
      },
      error: err => {
        const e = err?.error as { error?: string; detail?: string } | undefined;
        const msg = e?.error || e?.detail || 'Suppression impossible.';
        this.snackBar.open(msg, 'OK', { duration: 5000 });
      }
    });
  }

  blockAuthor(row: ModerationMessageRow): void {
    const authorId = Number(row.authorId);
    if (!Number.isFinite(authorId) || authorId <= 0) {
      return;
    }
    const modId = this.moderatorId();
    if (modId === authorId) {
      this.snackBar.open(
        'Vous ne pouvez pas bloquer votre propre compte. Choisissez un autre auteur.',
        'OK',
        { duration: 6000 }
      );
      return;
    }
    if (!confirm(`Bloquer ${this.authorLabel(authorId)} ? Il ne pourra plus interagir avec vous dans le forum (liste de blocage).`)) {
      return;
    }
    this.moderation.blockUser(modId, authorId).subscribe({
      next: () => {
        this.snackBar.open('Utilisateur bloqué pour votre profil modérateur.', 'OK', { duration: 4000 });
      },
      error: err => {
        const e = err?.error as { error?: string; detail?: string } | undefined;
        const msg = e?.error || e?.detail || 'Blocage impossible.';
        this.snackBar.open(msg, 'OK', { duration: 5000 });
      }
    });
  }
}
