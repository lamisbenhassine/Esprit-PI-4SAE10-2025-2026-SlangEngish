import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { MenuItem } from '../../shared/sidebar/sidebar.component';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.css']
})
export class LayoutComponent {
  isSidebarCollapsed = false;
  username = 'Admin User';
  userAvatar = '';

  backofficeMenuItems: MenuItem[] = [
    { id: 'dashboard', label: 'Dashboard', icon: 'dashboard', routerLink: '/backoffice/dashboard' },

    { id: 'sec-platform', label: 'Plateforme', icon: 'hub', routerLink: '', kind: 'section' },
    { id: 'users', label: 'Utilisateurs', icon: 'manage_accounts', routerLink: '/backoffice/users' },
    { id: 'courses', label: 'Cours', icon: 'school', routerLink: '/backoffice/courses' },
    { id: 'clubs', label: 'Clubs', icon: 'groups', routerLink: '/backoffice/clubs' },
    { id: 'forum-mgmt', label: 'Forum', icon: 'forum', routerLink: '/backoffice/forum' },

    { id: 'sec-inscription', label: 'Inscription & fidélité', icon: 'workspace_premium', routerLink: '', kind: 'section' },
    { id: 'sub-mgmt', label: 'Offres & abonnements', icon: 'subscriptions', routerLink: '/backoffice/subscription' },
    { id: 'loyalty-accounts', label: 'Cartes fidélité', icon: 'card_giftcard', routerLink: '/backoffice/loyalty-accounts' },
    { id: 'inscription-stats', label: 'Statistiques inscription', icon: 'insights', routerLink: '/backoffice/inscription-stats' }
  ];

  constructor(private router: Router) { }

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
