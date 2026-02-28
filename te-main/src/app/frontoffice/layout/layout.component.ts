import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { MenuItem } from '../../shared/sidebar/sidebar.component';
import { NewOfferNotificationService, NewOfferNotification } from '../../services/new-offer-notification.service';
@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.css']
})
export class LayoutComponent implements OnInit, OnDestroy {
  isSidebarCollapsed = false;
  username = 'John Doe';
  userAvatar = '';
  notifications: NewOfferNotification[] = [];  // ✅ ajouté

  private newOfferSub: Subscription | null = null;

  frontofficeMenuItems: MenuItem[] = [
    { id: 'dashboard', label: 'Dashboard', icon: 'dashboard', routerLink: '/frontoffice/dashboard' },
    { id: 'profile', label: 'Profile', icon: 'person', routerLink: '/frontoffice/profile' },
    { id: 'courses', label: 'Courses', icon: 'school', routerLink: '/frontoffice/courses' },
    { id: 'chat', label: 'Chat', icon: 'chat', routerLink: '/frontoffice/chat' },
    { id: 'settings', label: 'Settings', icon: 'settings', routerLink: '/frontoffice/settings' },
    { id: 'help', label: 'Help', icon: 'help', routerLink: '/frontoffice/help' },
    { id: 'jobs', label: 'Job Offers', icon: 'work_outline', routerLink: '/frontoffice/job-offers' },
    { id: 'saved-offers', label: 'Offres Sauvegardées', icon: 'bookmark', routerLink: '/frontoffice/saved-offers' },
    { id: 'preferences', label: 'Mes Préférences', icon: 'tune', routerLink: '/frontoffice/preferences' }
  ];

  constructor(
    private router: Router,
    private snackBar: MatSnackBar,
    private newOfferNotification: NewOfferNotificationService
  ) {}

  ngOnInit(): void {
    try {
      this.newOfferSub = this.newOfferNotification.onNewOffer.subscribe((offer) => {
              this.notifications.unshift(offer);  // ✅ ajoute à la liste

        try {
          const message = offer?.message ?? `Nouvelle offre : ${offer?.title ?? ''}`;
          const ref = this.snackBar.open(message, 'Voir', { duration: 6000 });
          ref.onAction().subscribe(() => {
            try {
              if (offer?.id != null) this.router.navigate(['/frontoffice/job-details', offer.id]);
            } catch {
              // ignore
            }
          });
        } catch {
          // ignore
        }
      });
      setTimeout(() => {
        try {
          this.newOfferNotification.connect();
        } catch {
          // ignore
        }
      }, 500);
    } catch {
      // notifications indisponibles : la page continue
    }
  }

  ngOnDestroy(): void {
    try {
      this.newOfferSub?.unsubscribe();
      this.newOfferSub = null;
    } catch {
      // ignore
    }
  }

  // ✅ ajouté
  goToOffer(notif: NewOfferNotification): void {
    this.router.navigate(['/frontoffice/job-details', notif.id]);
  }

  // ✅ ajouté
  clearNotifications(): void {
    this.notifications = [];
  }

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
    this.router.navigate(['/frontoffice/profile']);
  }

  onSettings() {
    this.router.navigate(['/frontoffice/settings']);
  }
}
