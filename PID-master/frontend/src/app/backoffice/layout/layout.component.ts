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
    { id: 'users', label: 'User Management', icon: 'people', routerLink: '/backoffice/users' },
    { id: 'courses', label: 'Course Management', icon: 'school', routerLink: '/backoffice/courses' },
    { id: 'evaluations', label: 'Evaluations', icon: 'assignment', routerLink: '/backoffice/evaluations' },
    { id: 'clubs', label: 'Club Management', icon: 'groups', routerLink: '/backoffice/clubs' },
    { id: 'reports', label: 'Reports', icon: 'assessment', routerLink: '/backoffice/reports' },
    { id: 'settings', label: 'Settings', icon: 'settings', routerLink: '/backoffice/settings' }
  ];

  constructor(private router: Router) {}

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  onMenuItemClick(item: MenuItem) {
    this.router.navigate([item.routerLink]);
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
