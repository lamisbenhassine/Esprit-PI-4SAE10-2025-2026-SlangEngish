import { Component, OnInit, OnDestroy } from '@angular/core';
import { StatsService, DashboardStats } from '../../core/services/stats.service';
import { WebSocketService } from '../../core/services/websocket.service';
import { Subscription } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

interface StatCard {
  title: string;
  value: string | number;
  icon: string;
  change: number;
  changeType: 'positive' | 'negative' | 'neutral';
  color: string;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {
  stats: StatCard[] = [];
  recentActivities: any[] = [];
  isLoading = true;
  isConnected = false;
  lastUpdated: string | null = null;
  recognizedRevenueChart: Array<{ label: string; amount: number; ratio: number }> = [];
  topCourses = [
    { name: 'Introduction to Angular', students: 1250, rating: 4.8, progress: 92 },
    { name: 'Advanced CSS Techniques', students: 890, rating: 4.7, progress: 85 },
    { name: 'JavaScript Fundamentals', students: 2100, rating: 4.9, progress: 98 },
    { name: 'React Development', students: 1560, rating: 4.6, progress: 78 },
    { name: 'Node.js Backend', students: 980, rating: 4.5, progress: 72 }
  ];

  private statsSub?: Subscription;
  private notificationSub?: Subscription;

  constructor(
    private statsService: StatsService,
    private wsService: WebSocketService,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit() {
    this.loadInitialStats();
    this.setupWebSocket();
  }

  ngOnDestroy() {
    this.statsSub?.unsubscribe();
    this.notificationSub?.unsubscribe();
  }

  private loadInitialStats() {
    this.statsService.getStats().subscribe({
      next: (data) => {
        this.updateStatsUI(data);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load stats:', err);
        this.isLoading = false;
        this.snackBar.open('⚠️ Could not connect to real-time stats backend.', 'RETRY', {
          duration: 5000
        });
      }
    });
  }

  private setupWebSocket() {
    // Listen for connection status (live indicator)
    this.wsService.isConnected$.subscribe(connected => {
      this.isConnected = connected;
    });

    // Listen for real-time stats updates
    this.statsSub = this.wsService.stats$.subscribe(data => {
      if (data) {
        this.updateStatsUI(data);
        // Subtle toast for admin
        this.snackBar.open('📈 Dashboard updated in real-time', '', {
          duration: 2000,
          horizontalPosition: 'right',
          verticalPosition: 'bottom',
          panelClass: ['mini-toast']
        });
      }
    });

    // Listen for real-time notifications
    this.notificationSub = this.wsService.notifications$.subscribe(notif => {
      if (notif) {
        this.recentActivities.unshift({
          id: Date.now(),
          action: notif.title,
          user: notif.message,
          time: 'Just now',
          type: notif.type.toLowerCase()
        });

        // Limit to 10 activities
        if (this.recentActivities.length > 10) this.recentActivities.pop();

        // Also show a snackbar for important notifications
        this.snackBar.open(`${notif.title}: ${notif.message}`, 'OK', {
          duration: 5000,
          panelClass: [notif.type.toLowerCase() + '-snackbar']
        });
      }
    });
  }

  private updateStatsUI(data: DashboardStats) {
    this.lastUpdated = data.lastUpdated;
    const currencySuffix = ' TND';
    this.stats = [
      {
        title: 'Total Revenue',
        value: (data.totalRevenue || 0).toLocaleString() + currencySuffix,
        icon: 'payments',
        change: 15.4, // We could calculate this if we had history
        changeType: 'positive',
        color: 'green'
      },
      {
        title: 'New Orders (Today)',
        value: data.newSubscriptionsToday,
        icon: 'shopping_cart',
        change: 5.2,
        changeType: 'positive',
        color: 'blue'
      },
      {
        title: 'Conversion Rate',
        value: (data.conversionRate || 0).toFixed(1) + '%',
        icon: 'trending_up',
        change: 2.1,
        changeType: 'positive',
        color: 'purple'
      },
      {
        title: 'Monthly Revenue',
        value: (data.revenueThisMonth || 0).toLocaleString() + currencySuffix,
        icon: 'bar_chart',
        change: 12.8,
        changeType: 'positive',
        color: 'orange'
      }
    ];

    // Build advanced metric chart from recognized revenue by month (Métier 3)
    const series = data.recognizedRevenueByMonth || [];
    if (series.length) {
      const mapped = series.map(m => {
        const label = new Date(m.year, m.month - 1, 1).toLocaleString('default', { month: 'short' });
        const amount = m.recognizedAmount || 0;
        return { label, amount };
      });
      const max = Math.max(...mapped.map(m => m.amount), 1);
      this.recognizedRevenueChart = mapped.map(m => ({
        label: m.label,
        amount: m.amount,
        ratio: Math.round((m.amount / max) * 100)
      }));
    } else {
      this.recognizedRevenueChart = [];
    }
  }

  getActivityIcon(type: string): string {
    switch (type) {
      case 'success': return 'check_circle';
      case 'info': return 'info';
      case 'warning': return 'warning';
      case 'error': return 'error';
      default: return 'notifications';
    }
  }

  getActivityIconClass(type: string): string {
    return `activity-icon ${type}`;
  }
}
