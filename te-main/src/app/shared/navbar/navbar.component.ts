import { Component, Input, Output, EventEmitter } from '@angular/core';
import { NewOfferNotification } from '../../services/new-offer-notification.service';

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
  @Input() notifications: NewOfferNotification[] = [];
  @Output() sidebarToggle = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();
  @Output() notifClick = new EventEmitter<NewOfferNotification>();
  @Output() notifClear = new EventEmitter<void>();

  isProfileMenuOpen = false;
  showNotifPanel = false;

  toggleSidebar() { this.sidebarToggle.emit(); }
  toggleProfileMenu() { this.isProfileMenuOpen = !this.isProfileMenuOpen; }
  toggleNotifPanel() { this.showNotifPanel = !this.showNotifPanel; }
  onLogout() { this.logout.emit(); this.isProfileMenuOpen = false; }
  onProfile() { this.isProfileMenuOpen = false; }
  onSettings() { this.isProfileMenuOpen = false; }
  onNotifClick(notif: NewOfferNotification) { this.notifClick.emit(notif); this.showNotifPanel = false; }
  onClearNotifs() { this.notifClear.emit(); this.showNotifPanel = false; }
}