import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CartService } from '../../core/services/cart.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { MessagingUnreadService } from '../../core/services/messaging-unread.service';
import { Observable, Subscription, combineLatest } from 'rxjs';
import { Router } from '@angular/router';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  @Input() theme: 'light' | 'dark' = 'light';
  @Input() username: string = 'User';
  @Input() userAvatar: string = '';
  @Input() showSidebarToggle: boolean = true;
  /** When true, listens to admin-only notifications instead of public ones */
  @Input() isAdmin: boolean = false;
  /** Affiche le lien vers /backoffice/tuteur-messages (front-office et back-office admin). */
  @Input() showTutorMessagesShortcut = false;
  @Output() sidebarToggle = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();

  cartCount$: Observable<number>;
  notifications: any[] = [];
  unreadCount = 0;
  /** Messages privés (DM) non lus — compteur messagerie. */
  dmUnreadCount = 0;
  private wsSub?: Subscription;
  private dmUnreadSub?: Subscription;

  constructor(
    private cartService: CartService,
    private wsService: WebSocketService,
    private messagingUnread: MessagingUnreadService,
    private router: Router
  ) {
    this.cartCount$ = this.cartService.getCartCount();
  }

  get totalBellBadge(): number {
    return this.unreadCount + this.dmUnreadCount;
  }

  ngOnInit(): void {
    // Initial fetch of cart state (only for frontoffice)
    if (!this.isAdmin) {
      this.cartService.getCartByUserId(1).subscribe();
    }

    // Subscribe to the appropriate notification channel
    const notif$ = this.isAdmin
      ? this.wsService.adminNotifications$
      : this.wsService.notifications$;

    this.wsSub = notif$.subscribe(notif => {
      if (notif) {
        this.notifications.unshift(notif);
        this.unreadCount++;
        // Keep at most 10 notifications in the list
        if (this.notifications.length > 10) this.notifications.pop();
      }
    });

    this.dmUnreadSub = combineLatest([
      this.messagingUnread.tutorUnread$,
      this.messagingUnread.studentUnread$
    ]).subscribe(([t, s]) => {
      this.dmUnreadCount = this.isAdmin ? t : s;
    });
  }

  ngOnDestroy() {
    this.wsSub?.unsubscribe();
    this.dmUnreadSub?.unsubscribe();
  }

  markAsRead() {
    this.unreadCount = 0;
  }

  clearAll() {
    this.notifications = [];
    this.unreadCount = 0;
  }

  toggleSidebar() {
    this.sidebarToggle.emit();
  }

  onCart() {
    this.router.navigate(['/frontoffice/inscription/cart']);
  }

  onLogout() {
    this.logout.emit();
  }

  onProfile() {
    // Navigate to profile
  }

  onSettings() {
    // Navigate to settings
  }

  getNotifIcon(type: string): string {
    switch (type?.toUpperCase()) {
      case 'SUCCESS': return 'check_circle';
      case 'WARNING': return 'warning';
      case 'ERROR': return 'error';
      default: return 'notifications';
    }
  }
}
