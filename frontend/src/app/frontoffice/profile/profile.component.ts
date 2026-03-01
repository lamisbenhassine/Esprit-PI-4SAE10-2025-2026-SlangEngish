import { Component } from '@angular/core';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  name: string;
  email: string;
  phone: string;
  address: string;
  role: string;
  avatar?: string;
}

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent {
  user: User = {
    id: 1,
    firstName: 'John',
    lastName: 'Doe',
    name: 'John Doe',
    email: 'john.doe@example.com',
    phone: '+1 (555) 123-4567',
    address: '123 Main St, Anytown, USA 12345',
    role: 'Student',
    avatar: ''
  };

  passwordData = {
    current: '',
    new: '',
    confirm: ''
  };

  saveProfile() {
    this.user.name = `${this.user.firstName} ${this.user.lastName}`;
    console.log('Profile saved:', this.user);
  }

  cancelEdit() {
    console.log('Edit cancelled');
  }

  changePassword() {
    if (this.passwordData.new !== this.passwordData.confirm) {
      console.error('Passwords do not match');
      return;
    }
    console.log('Password changed');
    this.passwordData = { current: '', new: '', confirm: '' };
  }
}
