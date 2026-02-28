import { Component, OnInit } from '@angular/core';
import { MatchingService, StudentProfile } from '../../services/matching.service';

@Component({
  selector: 'app-student-preferences',
  templateUrl: './student-preferences.component.html',
  styleUrls: ['./student-preferences.component.css']
})
export class StudentPreferencesComponent implements OnInit {
  studentId = 1;
  loading = false;
  saving = false;
  saved = false;

  profile: StudentProfile = {
    preferredLocation: '',
    preferredContractType: '',
    expectedSalary: undefined,
    skills: ''
  };

  skillInput = '';
  skillsList: string[] = [];

  contractTypes = ['CDI', 'CDD', 'STAGE', 'ALTERNANCE', 'FREELANCE'];

  constructor(private matchingService: MatchingService) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.matchingService.getProfile(this.studentId).subscribe({
      next: (data) => {
        this.profile = data;
        // ✅ Convertit skills string en tableau
        if (data.skills) {
          this.skillsList = data.skills.split(',')
            .map(s => s.trim())
            .filter(s => s.length > 0);
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  addSkill(): void {
    const skill = this.skillInput.trim();
    if (skill && !this.skillsList.includes(skill)) {
      this.skillsList.push(skill);
      this.skillInput = '';
    }
  }

  removeSkill(skill: string): void {
    this.skillsList = this.skillsList.filter(s => s !== skill);
  }

  onSkillKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addSkill();
    }
  }

  saveProfile(): void {
    this.saving = true;
    this.saved = false;

    // ✅ Convertit tableau skills en string
    this.profile.skills = this.skillsList.join(',');

    this.matchingService.saveProfile(this.studentId, this.profile).subscribe({
      next: () => {
        this.saving = false;
        this.saved = true;
        setTimeout(() => this.saved = false, 3000);
      },
      error: () => { this.saving = false; }
    });
  }

  getContractColor(type: string): string {
    switch (type) {
      case 'CDI': return '#166534';
      case 'CDD': return '#1e40af';
      case 'STAGE': return '#9a3412';
      case 'ALTERNANCE': return '#6b21a8';
      case 'FREELANCE': return '#991b1b';
      default: return '#475569';
    }
  }

  getContractBg(type: string): string {
    switch (type) {
      case 'CDI': return '#dcfce7';
      case 'CDD': return '#dbeafe';
      case 'STAGE': return '#ffedd5';
      case 'ALTERNANCE': return '#f3e8ff';
      case 'FREELANCE': return '#fee2e2';
      default: return '#f1f5f9';
    }
  }
}