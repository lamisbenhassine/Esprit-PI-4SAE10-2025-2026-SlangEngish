import { Component, OnInit } from '@angular/core';
import { StatsService, DashboardStats } from '../../services/stats.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-inscription-stats',
  templateUrl: './inscription-stats.component.html',
  styleUrls: ['./inscription-stats.component.css']
})
export class InscriptionStatsComponent implements OnInit {
  loading = true;
  data: DashboardStats | null = null;
  recognizedRevenueChart: Array<{ label: string; amount: number; ratio: number }> = [];

  constructor(
    private statsService: StatsService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.statsService.getStats().subscribe({
      next: (d) => {
        this.data = d;
        this.buildRevenueChart(d);
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        this.snackBar.open(
          'Impossible de charger les statistiques inscription. Vérifiez la gateway (8100), le microservice inscription (8050) et le proxy /api/inscription.',
          'OK',
          { duration: 6000 }
        );
      }
    });
  }

  private buildRevenueChart(d: DashboardStats): void {
    const series = d.recognizedRevenueByMonth || [];
    if (!series.length) {
      this.recognizedRevenueChart = [];
      return;
    }
    const mapped = series.map((m) => {
      const label = new Date(m.year, m.month - 1, 1).toLocaleString('fr-FR', { month: 'short' });
      return { label, amount: m.recognizedAmount || 0 };
    });
    const max = Math.max(...mapped.map((x) => x.amount), 1);
    this.recognizedRevenueChart = mapped.map((m) => ({
      label: m.label,
      amount: m.amount,
      ratio: Math.round((m.amount / max) * 100)
    }));
  }
}
