import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-signin',
  templateUrl: './signin.component.html',
  styleUrls: ['./signin.component.css']
})
export class SigninComponent {
  signInData = {
    email: '',
    password: ''
  };
  hidePassword = true;
  rememberMe = false;

  constructor(private router: Router) {}

  isFormValid(): boolean {
    return this.signInData.email.trim() !== '' && 
           this.signInData.password.trim() !== '' &&
           this.isValidEmail(this.signInData.email);
  }

  isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  onSignIn() {
    if (this.isFormValid()) {
      console.log('Sign in data:', this.signInData);
      console.log('Remember me:', this.rememberMe);
      
      // Simulate authentication success
      setTimeout(() => {
        this.router.navigate(['/frontoffice/dashboard']);
      }, 1000);
    }
  }
}
