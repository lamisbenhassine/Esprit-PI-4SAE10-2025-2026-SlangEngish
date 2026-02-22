import { Component, OnInit } from '@angular/core';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  status: 'active' | 'inactive' | 'pending';
  avatar: string;
  joinDate: string;
  lastActive: string;
}

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit {
  users: User[] = [
    {
      id: 1,
      firstName: 'John',
      lastName: 'Smith',
      email: 'john.smith@example.com',
      role: 'Student',
      status: 'active',
      avatar: 'https://via.placeholder.com/40x40/667eea/ffffff?text=JS',
      joinDate: '2024-01-15',
      lastActive: '2 hours ago'
    },
    {
      id: 2,
      firstName: 'Sarah',
      lastName: 'Johnson',
      email: 'sarah.j@example.com',
      role: 'Instructor',
      status: 'active',
      avatar: 'https://via.placeholder.com/40x40/4caf50/ffffff?text=SJ',
      joinDate: '2023-11-20',
      lastActive: '5 minutes ago'
    },
    {
      id: 3,
      firstName: 'Mike',
      lastName: 'Davis',
      email: 'mike.d@example.com',
      role: 'Student',
      status: 'inactive',
      avatar: 'https://via.placeholder.com/40x40/ff9800/ffffff?text=MD',
      joinDate: '2023-09-10',
      lastActive: '3 days ago'
    },
    {
      id: 4,
      firstName: 'Emily',
      lastName: 'Wilson',
      email: 'emily.w@example.com',
      role: 'Admin',
      status: 'active',
      avatar: 'https://via.placeholder.com/40x40/9c27b0/ffffff?text=EW',
      joinDate: '2023-06-01',
      lastActive: '1 hour ago'
    },
    {
      id: 5,
      firstName: 'David',
      lastName: 'Brown',
      email: 'david.b@example.com',
      role: 'Instructor',
      status: 'pending',
      avatar: 'https://via.placeholder.com/40x40/2196f3/ffffff?text=DB',
      joinDate: '2024-02-01',
      lastActive: 'Never'
    },
    {
      id: 6,
      firstName: 'Lisa',
      lastName: 'Anderson',
      email: 'lisa.a@example.com',
      role: 'Student',
      status: 'active',
      avatar: 'https://via.placeholder.com/40x40/f44336/ffffff?text=LA',
      joinDate: '2024-01-28',
      lastActive: '30 minutes ago'
    },
    {
      id: 7,
      firstName: 'Chris',
      lastName: 'Martin',
      email: 'chris.m@example.com',
      role: 'Student',
      status: 'active',
      avatar: 'https://via.placeholder.com/40x40/00bcd4/ffffff?text=CM',
      joinDate: '2023-12-15',
      lastActive: '4 hours ago'
    },
    {
      id: 8,
      firstName: 'Anna',
      lastName: 'Taylor',
      email: 'anna.t@example.com',
      role: 'Instructor',
      status: 'inactive',
      avatar: 'https://via.placeholder.com/40x40/e91e63/ffffff?text=AT',
      joinDate: '2023-08-22',
      lastActive: '1 week ago'
    }
  ];

  filteredUsers: User[] = [];
  searchTerm: string = '';
  selectedRole: string = 'all';
  selectedStatus: string = 'all';

  ngOnInit(): void {
    this.filteredUsers = [...this.users];
  }

  filterUsers(): void {
    this.filteredUsers = this.users.filter(user => {
      const matchesSearch = this.searchTerm === '' || 
        user.firstName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        user.lastName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        user.email.toLowerCase().includes(this.searchTerm.toLowerCase());
      
      const matchesRole = this.selectedRole === 'all' || user.role === this.selectedRole;
      const matchesStatus = this.selectedStatus === 'all' || user.status === this.selectedStatus;
      
      return matchesSearch && matchesRole && matchesStatus;
    });
  }

  onSearch(): void {
    this.filterUsers();
  }

  onRoleChange(): void {
    this.filterUsers();
  }

  onStatusChange(): void {
    this.filterUsers();
  }

  getStatusClass(status: string): string {
    return `status-badge ${status}`;
  }

  getRoleClass(role: string): string {
    return `role-badge ${role.toLowerCase()}`;
  }

  editUser(user: User): void {
    console.log('Edit user:', user);
  }

  deleteUser(user: User): void {
    console.log('Delete user:', user);
  }

  viewUser(user: User): void {
    console.log('View user:', user);
  }

  addUser(): void {
    console.log('Add new user');
  }
}
