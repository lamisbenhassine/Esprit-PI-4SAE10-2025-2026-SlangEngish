import { Component } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { MenuItem } from '../../shared/sidebar/sidebar.component';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.css']
})
export class LayoutComponent {
  isSidebarCollapsed = false;
  feedLayout = false;
  username = 'John Doe';
  userAvatar = '';

  frontofficeMenuItems: MenuItem[] = [
    { id: 'sec-social', label: 'Espace social', icon: 'hub', routerLink: '', kind: 'section' },
    { id: 'feed', label: 'Fil', icon: 'dynamic_feed', routerLink: '/frontoffice/feed' },
    { id: 'sec-forum', label: 'Forum', icon: 'forum', routerLink: '', kind: 'section' },
    {
      id: 'forum-gen',
      label: 'Général · visible par tous',
      icon: 'public',
      routerLink: '/frontoffice/forum/general'
    },
    {
      id: 'forum-lev',
      label: 'Par niveau A1–C2 · abonnés',
      icon: 'school',
      routerLink: '/frontoffice/forum/levels'
    },
    { id: 'sec-dm', label: 'Messages privés', icon: 'mail_outline', routerLink: '', kind: 'section' },
    {
      id: 'messages',
      label: 'Collègues & tuteurs',
      icon: 'chat',
      routerLink: '/frontoffice/messages'
    },
    { id: 'dashboard', label: 'Tableau de bord', icon: 'dashboard', routerLink: '/frontoffice/dashboard' },
    { id: 'profile', label: 'Profil', icon: 'person', routerLink: '/frontoffice/profile' },
    { id: 'courses', label: 'Cours', icon: 'menu_book', routerLink: '/frontoffice/courses' },
    { id: 'inscription', label: 'Inscription', icon: 'how_to_reg', routerLink: '/frontoffice/inscription' }
  ];

  constructor(private router: Router) {
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe(e => {
      this.feedLayout = e.urlAfterRedirects.includes('/frontoffice/feed');
    });
    this.feedLayout = this.router.url.includes('/frontoffice/feed');
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
