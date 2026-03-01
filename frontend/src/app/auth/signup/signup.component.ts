import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent {
  signUpData = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: ''
  };
  hidePassword = true;
  hideConfirmPassword = true;
  agreeToTerms = false;
  avatarPreview = '';

  constructor(private router: Router) {}

  isFormValid(): boolean {
    return this.signUpData.firstName.trim() !== '' &&
           this.signUpData.lastName.trim() !== '' &&
           this.signUpData.email.trim() !== '' &&
           this.signUpData.password.trim() !== '' &&
           this.signUpData.confirmPassword.trim() !== '' &&
           this.isValidEmail(this.signUpData.email) &&
           this.signUpData.password === this.signUpData.confirmPassword &&
           this.signUpData.password.length >= 8 &&
           this.agreeToTerms;
  }

  isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  onAvatarChange(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        this.avatarPreview = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  onSignUp() {
    if (this.isFormValid()) {
      console.log('Sign up data:', this.signUpData);
      console.log('Avatar:', this.avatarPreview);
      
      // Simulate registration success
      setTimeout(() => {
        this.router.navigate(['/frontoffice/dashboard']);
      }, 1000);
    }
  }
}
