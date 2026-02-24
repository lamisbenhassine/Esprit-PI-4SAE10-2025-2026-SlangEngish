import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Client, IMessage } from '@stomp/stompjs';
import { Subject } from 'rxjs';

export interface NewOfferNotification {
  id: number;
  title: string;
  company: string;
  location: string;
  contractType: string;
  date: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NewOfferNotificationService {

  private stompClient!: Client;
  public onNewOffer = new Subject<NewOfferNotification>();

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  connect(): void {
    // ✅ Ne se connecte que dans le navigateur, pas côté serveur SSR
    if (!isPlatformBrowser(this.platformId)) return;

    this.stompClient = new Client({
      webSocketFactory: () => new WebSocket('ws://localhost:8081/ws/websocket'),
      onConnect: () => {
        console.log('✅ WebSocket connecté');
        this.stompClient.subscribe('/topic/new-offers', (message: IMessage) => {
          const notification: NewOfferNotification = JSON.parse(message.body);
          this.onNewOffer.next(notification);
        });
      },
      onDisconnect: () => console.log('❌ WebSocket déconnecté'),
      reconnectDelay: 5000
    });
    this.stompClient.activate();
  }

  disconnect(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.stompClient?.deactivate();
  }
}