import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { MenuItem } from '../../shared/sidebar/sidebar.component';
import { TutorDmNotifierService } from '../../core/services/tutor-dm-notifier.service';
import { MessagingUnreadService } from '../../core/services/messaging-unread.service';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.css']
})
export class LayoutComponent implements OnInit, OnDestroy {
  isSidebarCollapsed = false;
  username = 'Admin User';
  userAvatar = '';
  /** Badge bandeau + pied de menu (messages non lus côté tuteur). */
  tutorMsgBadge = 0;
  private tutorBadgeSub?: Subscription;


  backofficeMenuItems: MenuItem[] = [
    { id: 'dashboard', label: 'Dashboard', icon: 'dashboard', routerLink: '/backoffice/dashboard' },

    { id: 'sec-messagerie', label: 'Messagerie', icon: 'mail', routerLink: '', kind: 'section' },
    {
      id: 'tutor-inbox',
      label: 'Messages tuteurs',
      icon: 'chat',
      routerLink: '/backoffice/tutor-messages'
    },

    { id: 'sec-platform', label: 'Plateforme', icon: 'hub', routerLink: '', kind: 'section' },
    { id: 'users', label: 'Utilisateurs', icon: 'manage_accounts', routerLink: '/backoffice/users' },
    { id: 'courses', label: 'Cours', icon: 'school', routerLink: '/backoffice/courses' },
    { id: 'clubs', label: 'Clubs', icon: 'groups', routerLink: '/backoffice/clubs' },
    { id: 'forum-mgmt', label: 'Forum', icon: 'forum', routerLink: '/backoffice/forum' },
    {
      id: 'forum-comments-mod',
      label: 'Modération commentaires',
      icon: 'gavel',
      routerLink: '/backoffice/forum-comments'
    },

    { id: 'sec-inscription', label: 'Inscription & fidélité', icon: 'workspace_premium', routerLink: '', kind: 'section' },
    { id: 'sub-mgmt', label: 'Offres & abonnements', icon: 'subscriptions', routerLink: '/backoffice/subscription' },
    { id: 'loyalty-accounts', label: 'Cartes fidélité', icon: 'card_giftcard', routerLink: '/backoffice/loyalty-accounts' },
    { id: 'inscription-stats', label: 'Statistiques inscription', icon: 'insights', routerLink: '/backoffice/inscription-stats' }
  ];

  constructor(
    private router: Router,
    private tutorDmNotifier: TutorDmNotifierService,
    private messagingUnread: MessagingUnreadService
  ) {}

  ngOnInit(): void {
    this.tutorDmNotifier.startBackofficeWatch();
    this.tutorBadgeSub = this.messagingUnread.tutorUnread$.subscribe(n => {
      this.tutorMsgBadge = n;
      const item = this.backofficeMenuItems.find(i => i.id === 'tutor-inbox');
      if (item) {
        item.badge = n > 0 ? n : undefined;
      }
    });
  }

  ngOnDestroy(): void {
    this.tutorDmNotifier.stop();
    this.tutorBadgeSub?.unsubscribe();
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  onLogout() {
    this.router.navigate(['/auth/signin']);
  }

  onProfile() {
    this.router.navigate(['/backoffice/profile']);
  }

  onSettings() {
    this.router.navigate(['/backoffice/settings']);
  }
}
