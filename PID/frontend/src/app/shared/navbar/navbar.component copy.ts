import { Component, Input, Output, EventEmitter, OnDestroy, OnInit, Inject } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { interval, Subscription } from 'rxjs';
import { NotificationDto, NotificationService } from '../../frontoffice/notifications/notification.service';
import { UserContextService } from '../../services/user-context.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  /** Avatar par défaut (SVG inline) : évite via.placeholder.com qui coupe souvent la connexion (40x40 dans la console). */
  readonly defaultAvatar =
    'data:image/svg+xml,' +
    encodeURIComponent(
      '<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40">' +
        '<rect width="40" height="40" fill="#e0e0e0"/>' +
        '<circle cx="20" cy="15" r="6" fill="#9e9e9e"/>' +
        '<ellipse cx="20" cy="31" rx="11" ry="7" fill="#9e9e9e"/>' +
        '</svg>'
    );

  @Input() theme: 'light' | 'dark' = 'light';
  @Input() username: string = 'User';
  @Input() userAvatar: string = '';
  @Input() showSidebarToggle: boolean = true;
  @Output() sidebarToggle = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();

  isProfileMenuOpen = false;
  notifications: NotificationDto[] = [];
  unreadCount = 0;
  isNotificationsOpen = false;
  private pollingSub?: Subscription;
  private userId = 0;

  constructor(
    private notificationService: NotificationService,
    private userContext: UserContextService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit(): void {
    // Ne rien faire côté serveur (SSR / prerender)
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    this.userId = this.userContext.getCurrentUserId();
    this.loadNotifications();
    this.pollingSub = interval(30000).subscribe(() => this.loadNotifications());
  }

  ngOnDestroy(): void {
    this.pollingSub?.unsubscribe();
  }

  loadNotifications(): void {
    if (!this.userId || !isPlatformBrowser(this.platformId)) {
      return;
    }
    this.notificationService.getUnreadNotifications(this.userId).subscribe({
      next: (data) => {
        this.notifications = data;
        this.unreadCount = data.length;
      },
      error: () => {
        // On ignore les erreurs pour ne pas casser le rendu
        this.notifications = [];
        this.unreadCount = 0;
      }
    });
  }

  toggleSidebar() {
    this.sidebarToggle.emit();
  }

  toggleProfileMenu() {
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
  }

  toggleNotifications() {
    this.isNotificationsOpen = !this.isNotificationsOpen;
  }

  markAsRead(notification: NotificationDto) {
    this.notificationService.markAsRead(notification.id).subscribe({
      next: () => this.loadNotifications()
    });
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
