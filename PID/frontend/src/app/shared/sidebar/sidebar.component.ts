import { Component, Input, Output, EventEmitter } from '@angular/core';
import { trigger, state, style, transition, animate } from '@angular/animations';

export interface MenuItem {
  id: string;
  label: string;
  icon: string;
  /** Vide pour les titres de section */
  routerLink: string;
  active?: boolean;
  /** `section` = titre de groupe (non cliquable) */
  kind?: 'link' | 'section';
  /** Badge optionnel (ex. messages non lus). */
  badge?: number;
}

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css'],
  animations: [
    trigger('slideInOut', [
      state('open', style({
        width: '250px',
        transform: 'translateX(0)'
      })),
      state('closed', style({
        width: '70px',
        transform: 'translateX(0)'
      })),
      transition('open <=> closed', [
        animate('0.3s ease-in-out')
      ])
    ]),
    trigger('fadeInOut', [
      state('visible', style({
        opacity: 1,
        display: 'block'
      })),
      state('hidden', style({
        opacity: 0,
        display: 'none'
      })),
      transition('visible <=> hidden', [
        animate('0.2s ease-in-out')
      ])
    ])
  ]
})
export class SidebarComponent {
  @Input() theme: 'light' | 'dark' = 'light';
  /** `frontoffice` / `backoffice` = styles dédiés (le menu admin n’est pas dans le backend Java). */
  @Input() variant: 'default' | 'frontoffice' | 'backoffice' = 'default';
  /** Titre affiché sous l’icône (ex. « Espace admin » pour le back-office). */
  @Input() sidebarTitle = 'Menu';
  @Input() menuItems: MenuItem[] = [];
  @Input() isCollapsed: boolean = false;
  /** Lien optionnel en bas du menu (ex. back-office « Messages tuteurs »). */
  @Input() footerQuickLink?: string;
  @Input() footerQuickLabel = '';
  @Input() footerQuickIcon = 'chat';
  @Input() footerQuickBadge?: number;
  @Output() toggleCollapse = new EventEmitter<void>();
  /** Conservé pour compatibilité avec les layouts existants (navigation programmatique). */
  @Output() menuItemClick = new EventEmitter<MenuItem>();

  onToggleCollapse() {
    this.toggleCollapse.emit();
  }

  onNavLinkClick(item: MenuItem) {
    if (item.kind !== 'section') {
      this.menuItemClick.emit(item);
    }
  }

  onFooterQuickClick() {
    if (this.footerQuickLink) {
      this.menuItemClick.emit({
        id: 'footer-quick',
        label: this.footerQuickLabel,
        icon: this.footerQuickIcon,
        routerLink: this.footerQuickLink
      });
    }
  }
}
