import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { MenuItem } from '../../shared/sidebar/sidebar.component';
import { MessagingUnreadService } from '../../core/services/messaging-unread.service';
import { FrontofficeIdentityService } from '../../core/services/frontoffice-identity.service';
import { UserProfileService } from '../../core/services/user-profile.service';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.css']
})
export class LayoutComponent implements OnInit, OnDestroy {
  isSidebarCollapsed = false;
  feedLayout = false;
  /** Aligné sur l’utilisateur actif (même source que feed / messagerie). */
  username = '…';
  userAvatar = '';

  private studentBadgeSub?: Subscription;
  private identitySub?: Subscription;

  frontofficeMenuItems: MenuItem[] = [
    { id: 'sec-social', label: 'Social', icon: 'hub', routerLink: '', kind: 'section' },
    { id: 'feed', label: 'Feed', icon: 'dynamic_feed', routerLink: '/frontoffice/feed' },
    { id: 'sec-forum', label: 'Forum', icon: 'forum', routerLink: '', kind: 'section' },
    {
      id: 'forum-lev',
      label: 'Advanced Tracks A1–C2',
      icon: 'school',
      routerLink: '/frontoffice/forum/levels'
    },
    { id: 'sec-dm', label: 'Private Messages', icon: 'mail_outline', routerLink: '', kind: 'section' },
    {
      id: 'messages',
      label: 'Students & Tutors',
      icon: 'chat',
      routerLink: '/frontoffice/messages'
    },
    { id: 'dashboard', label: 'Dashboard', icon: 'dashboard', routerLink: '/frontoffice/dashboard' },
    { id: 'profile', label: 'Profile', icon: 'person', routerLink: '/frontoffice/profile' },
    { id: 'courses', label: 'Courses', icon: 'menu_book', routerLink: '/frontoffice/courses' },
    { id: 'inscription', label: 'Enrollment', icon: 'how_to_reg', routerLink: '/frontoffice/inscription' }
  ];

  constructor(
    private router: Router,
    private messagingUnread: MessagingUnreadService,
    private identity: FrontofficeIdentityService,
    private users: UserProfileService
  ) {
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe(e => {
      this.feedLayout = e.urlAfterRedirects.includes('/frontoffice/feed');
    });
    this.feedLayout = this.router.url.includes('/frontoffice/feed');
  }

  ngOnInit(): void {
    this.messagingUnread.startStudentInboxWatch(() => this.identity.getCurrentUserId());
    this.studentBadgeSub = this.messagingUnread.studentUnread$.subscribe(n => {
      const item = this.frontofficeMenuItems.find(i => i.id === 'messages');
      if (item) {
        item.badge = n > 0 ? n : undefined;
      }
    });
    this.identitySub = this.identity.userId$.subscribe(uid => this.refreshHeaderProfile(uid));
    this.refreshHeaderProfile(this.identity.getCurrentUserId());
  }

  ngOnDestroy(): void {
    this.messagingUnread.stopStudentInboxWatch();
    this.studentBadgeSub?.unsubscribe();
    this.identitySub?.unsubscribe();
  }

  private refreshHeaderProfile(userId: number): void {
    if (!userId) {
      return;
    }
    this.username = '…';
    this.users.getById(userId).subscribe({
      next: u => {
        const name = [u.firstName, u.lastName].filter(Boolean).join(' ').trim();
        this.username = name || `Utilisateur #${userId}`;
        this.userAvatar = '';
      },
      error: () => {
        this.username = `Utilisateur #${userId}`;
        this.userAvatar = '';
      }
    });
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  onLogout() {
    this.router.navigate(['/auth/signin']);
  }

  onProfile() {
    this.router.navigate(['/frontoffice/profile']);
  }

  onSettings() {
    this.router.navigate(['/frontoffice/settings']);
  }
}
