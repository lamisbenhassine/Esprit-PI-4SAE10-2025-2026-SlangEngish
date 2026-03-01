import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CartService } from '../../core/services/cart.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { Observable, Subscription } from 'rxjs';
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
  @Output() sidebarToggle = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();

  cartCount$: Observable<number>;
  notifications: any[] = [];
  unreadCount = 0;
  private wsSub?: Subscription;

  constructor(
    private cartService: CartService,
    private wsService: WebSocketService,
    private router: Router
  ) {
    this.cartCount$ = this.cartService.getCartCount();
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
  }

  ngOnDestroy() {
    this.wsSub?.unsubscribe();
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
