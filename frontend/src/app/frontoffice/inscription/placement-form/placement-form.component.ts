import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
    selector: 'app-placement-form',
    templateUrl: './placement-form.component.html',
    styleUrls: ['./placement-form.component.css']
})
export class PlacementFormComponent implements OnInit {
    placementForm: FormGroup;
    levels = [
        {
            value: 'A1',
            label: 'Beginner (A1)',
            icon: 'child_care',
            description: 'Start from scratch with basic vocabulary and essential grammar.',
            examplePrice: '29.99 TND',
            benefits: ['Basic Greetings', 'Daily Nouns', 'Simple Sentences']
        },
        {
            value: 'A2',
            label: 'Elementary (A2)',
            icon: 'person',
            description: 'Communicate in simple, routine tasks and everyday situations.',
            examplePrice: '39.99 TND',
            benefits: ['Past Tense', 'Shopping & Travel', 'Family Descriptions']
        },
        {
            value: 'B1',
            label: 'Intermediate (B1)',
            icon: 'school',
            description: 'Handle most situations while traveling. Describe experiences & dreams.',
            examplePrice: '49.99 TND',
            benefits: ['Complex Dialogues', 'Work Vocabulary', 'Travel Fluency']
        },
        {
            value: 'B2',
            label: 'Upper Int. (B2)',
            icon: 'psychology',
            description: 'Understand main ideas of complex text. Interact with native speakers.',
            examplePrice: '59.99 TND',
            benefits: ['Abstract Topics', 'Business English', 'Fluent Debate']
        },
        {
            value: 'C1',
            label: 'Advanced (C1)',
            icon: 'auto_stories',
            description: 'Express ideas fluently without much obvious searching for expressions.',
            examplePrice: '79.99 TND',
            benefits: ['Professional Writing', 'Subtle Meanings', 'Academic Success']
        }
    ];

    constructor(
        private fb: FormBuilder,
        private router: Router
    ) {
        this.placementForm = this.fb.group({
            firstName: ['', Validators.required],
            lastName: ['', Validators.required],
            email: ['', [Validators.required, Validators.email]],
            estimatedLevel: ['', Validators.required],
            goals: ['']
        });
    }

    ngOnInit(): void { }

    onSubmit(): void {
        if (this.placementForm.valid) {
            console.log('Placement data:', this.placementForm.value);
            // We can store this in a service or pass as query params
            const selectedLevel = this.placementForm.value.estimatedLevel;
            this.router.navigate(['/frontoffice/inscription/offers'], {
                queryParams: { level: selectedLevel }
            });
        }
    }
}
