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
  /** Neutral styling for learner area (less “Material blue” than backoffice). */
  @Input() variant: 'default' | 'frontoffice' = 'default';
  @Input() menuItems: MenuItem[] = [];
  @Input() isCollapsed: boolean = false;
  @Output() toggleCollapse = new EventEmitter<void>();

  onToggleCollapse() {
    this.toggleCollapse.emit();
  }
}
