import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserService, User, Status } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { AddUserDialogComponent } from './add-user-dialog.component';
import { EditUserDialogComponent } from './edit-user-dialog.component';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit {
  /** Liste complète chargée depuis l'API */
  users: User[] = [];
  /** Liste filtrée affichée (recherche + rôle + statut) */
  filteredUsers: User[] = [];
  searchTerm: string = '';
  selectedRole: string = 'all';
  selectedStatus: string = 'all';
  isLoading = false;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  get isAdmin(): boolean {
    const u = this.authService.getCurrentUser();
    return u?.role === 'ADMIN';
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading = true;
    this.userService.getAll().subscribe({
      next: (users) => {
        this.users = users.map((u) => this.normalizeUser(u));
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load users', err);
        this.isLoading = false;
      }
    });
  }

  private normalizeUser(u: User): User {
    return {
      ...u,
      status: u.status ?? 'ACTIVE',
      joinDate: u.joinDate ?? '',
      lastActive: u.lastActive ?? '',
      avatar: u.photoBase64?.startsWith('http')
        ? u.photoBase64
        : u.photoBase64
          ? `data:image/jpeg;base64,${u.photoBase64}`
          : `https://via.placeholder.com/40x40/667eea/ffffff?text=${(u.firstName || '').charAt(0)}${(u.lastName || '').charAt(0)}`
    };
  }

  /** Filtre la liste localement (recherche dynamique). */
  applyFilters(): void {
    const term = (this.searchTerm || '').trim().toLowerCase();
    const role = this.selectedRole;
    const status = this.selectedStatus;
    this.filteredUsers = this.users.filter((user) => {
      const matchesSearch =
        term === '' ||
        (user.firstName || '').toLowerCase().includes(term) ||
        (user.lastName || '').toLowerCase().includes(term) ||
        (user.email || '').toLowerCase().includes(term);
      const matchesRole = role === 'all' || user.role === role;
      const matchesStatus = status === 'all' || (user.status ?? 'ACTIVE') === status;
      return matchesSearch && matchesRole && matchesStatus;
    });
  }

  /** Saisie recherche : filtre immédiat (recherche dynamique). */
  onSearchInput(): void {
    this.applyFilters();
  }

  onRoleChange(): void {
    this.applyFilters();
  }

  onStatusChange(): void {
    this.applyFilters();
  }

  getStatusClass(status: string | undefined): string {
    const s = (status ?? 'ACTIVE').toLowerCase();
    return `status-badge ${s}`;
  }

  getStatusLabel(status: string | undefined): string {
    const s = status ?? 'ACTIVE';
    if (s === 'ACTIVE') return 'Actif';
    if (s === 'INACTIVE') return 'Bloqué';
    if (s === 'PENDING') return 'En attente';
    return s;
  }

  blockUser(user: User): void {
    if (!user.id || !this.isAdmin) return;
    const adminId = this.authService.getCurrentUser()?.id;
    if (!adminId) return;
    this.userService.setStatus(user.id, adminId, 'INACTIVE').subscribe({
      next: () => {
        this.loadUsers();
        this.snackBar.open('Utilisateur bloqué.', 'Fermer', { duration: 3000, horizontalPosition: 'end', verticalPosition: 'top' });
      },
      error: (err) => {
        const msg = err?.error?.message || 'Impossible de bloquer l\'utilisateur.';
        this.snackBar.open(msg, 'Fermer', { duration: 4000, horizontalPosition: 'end', verticalPosition: 'top' });
      }
    });
  }

  unblockUser(user: User): void {
    if (!user.id || !this.isAdmin) return;
    const adminId = this.authService.getCurrentUser()?.id;
    if (!adminId) return;
    this.userService.setStatus(user.id, adminId, 'ACTIVE').subscribe({
      next: () => {
        this.loadUsers();
        this.snackBar.open('Utilisateur débloqué.', 'Fermer', { duration: 3000, horizontalPosition: 'end', verticalPosition: 'top' });
      },
      error: (err) => {
        const msg = err?.error?.message || 'Impossible de débloquer l\'utilisateur.';
        this.snackBar.open(msg, 'Fermer', { duration: 4000, horizontalPosition: 'end', verticalPosition: 'top' });
      }
    });
  }

  getRoleClass(role: string | undefined): string {
    return `role-badge ${(role ?? '').toLowerCase()}`;
  }

  editUser(user: User): void {
    if (!user.id) {
      return;
    }

    const dialogRef = this.dialog.open(EditUserDialogComponent, {
      width: '600px',
      disableClose: true,
      data: user
    });

    dialogRef.afterClosed().subscribe((result: User | undefined) => {
      if (result) {
        this.loadUsers();
        this.snackBar.open('User updated successfully!', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top'
        });
      }
    });
  }

  deleteUser(user: User): void {
    if (!user.id) {
      return;
    }
    this.userService.delete(user.id).subscribe({
      next: () => {
        this.loadUsers();
      },
      error: (err) => {
        console.error('Failed to delete user', err);
      }
    });
  }

  viewUser(user: User): void {
    console.log('View user:', user);
  }

  addUser(): void {
    const dialogRef = this.dialog.open(AddUserDialogComponent, {
      width: '600px',
      disableClose: true
    });

    dialogRef.afterClosed().subscribe((result: User | undefined) => {
      if (result) {
        this.loadUsers();
        this.snackBar.open('User added successfully!', 'Close', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top'
        });
      }
    });
  }
}
