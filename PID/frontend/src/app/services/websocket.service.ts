import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Subject, Observable, BehaviorSubject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';

@Injectable({
    providedIn: 'root'
})
export class WebSocketService {
    private client?: Client;
    private statsSubject = new BehaviorSubject<any>(null);
    /** Public notifications (for clients on /topic/notifications) */
    private notificationSubject = new Subject<any>();
    /** Admin-only notifications (for admins on /topic/admin-notifications) */
    private adminNotificationSubject = new Subject<any>();
    private connectionStatus = new BehaviorSubject<boolean>(false);

    constructor(@Inject(PLATFORM_ID) private platformId: any) {
        if (isPlatformBrowser(this.platformId)) {
            this.initialize();
        }
    }

    private async initialize() {
        try {
            const _SockJS = await import('sockjs-client');
            const SockJS = (_SockJS as any).default || _SockJS;
            this.connect(SockJS);
        } catch (error) {
            console.error('Failed to load SockJS:', error);
        }
    }

    private connect(SockJS: any) {
        this.client = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8030/ws'),
            debug: (msg) => {
                // console.log(msg);
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        this.client.onConnect = (frame) => {
            console.log('WebSocket connected');
            this.connectionStatus.next(true);

            // Dashboard stats updates
            this.client!.subscribe('/topic/stats', (message: IMessage) => {
                if (message.body) {
                    this.statsSubject.next(JSON.parse(message.body));
                }
            });

            // Public notifications (clients)
            this.client!.subscribe('/topic/notifications', (message: IMessage) => {
                if (message.body) {
                    this.notificationSubject.next(JSON.parse(message.body));
                }
            });

            // Admin-only notifications (payments, orders, etc.)
            this.client!.subscribe('/topic/admin-notifications', (message: IMessage) => {
                if (message.body) {
                    this.adminNotificationSubject.next(JSON.parse(message.body));
                }
            });
        };

        this.client.onStompError = (frame) => {
            console.error('STOMP Error:', frame.headers['message']);
        };

        this.client.onWebSocketClose = () => {
            this.connectionStatus.next(false);
        };

        this.client.activate();
    }

    /** Real-time dashboard stats */
    get stats$(): Observable<any> {
        return this.statsSubject.asObservable();
    }

    /** Public notifications (for client-facing pages) */
    get notifications$(): Observable<any> {
        return this.notificationSubject.asObservable();
    }

    /** Admin-only notifications (payments, orders — backoffice only) */
    get adminNotifications$(): Observable<any> {
        return this.adminNotificationSubject.asObservable();
    }

    get isConnected$(): Observable<boolean> {
        return this.connectionStatus.asObservable();
    }

    send(destination: string, payload: any) {
        if (this.client && this.client.connected) {
            this.client.publish({ destination, body: JSON.stringify(payload) });
        }
    }
}
