import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent {
  @Input() theme: 'light' | 'dark' = 'light';
  @Input() username: string = 'User';
  @Input() userAvatar: string = '';
  @Input() showSidebarToggle: boolean = true;

  /** Inline placeholder to avoid external image requests (e.g. via.placeholder.com) */
  avatarPlaceholder = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40"><circle cx="20" cy="20" r="20" fill="%23667eea"/><text x="20" y="26" font-size="16" fill="white" text-anchor="middle" font-family="sans-serif">?</text></svg>');
  @Output() sidebarToggle = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();

  isProfileMenuOpen = false;

  toggleSidebar() {
    this.sidebarToggle.emit();
  }

  toggleProfileMenu() {
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
  }

  onLogout() {
    this.logout.emit();
    this.isProfileMenuOpen = false;
  }

  onProfile() {
    this.isProfileMenuOpen = false;
  }

  onSettings() {
    this.isProfileMenuOpen = false;
  }
}
