import { Component, OnDestroy, OnInit } from '@angular/core';
import { Reclamation, ReclamationService } from '../../services/reclamation.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reclamations',
  templateUrl: './reclamations.component.html',
  styleUrls: ['./reclamations.component.css']
})
export class ReclamationsComponent implements OnInit, OnDestroy {
  reclamations: Reclamation[] = [];
  formModel: Reclamation = {
    sujet: '',
    description: ''
  };
  studentId: number | null = null;
  loading = false;
  errorMessage = '';
  successMessage = '';
  isRecording = false;
  recordingField: 'sujet' | 'description' = 'description';
  speechSupported = false;
  private recognition: any | null = null;
  showChatbot = false;
  isChatbotLoading = false;
  chatbotInput = '';
  chatbotMessages: Array<{ role: 'bot' | 'user'; text: string }> = [
    {
      role: 'bot',
      text: 'Hi! I am your Reclamation Assistant. Tell me your issue and I can help you write a clear complaint.'
    }
  ];

  constructor(
    private reclamationService: ReclamationService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser || currentUser.role !== 'STUDENT') {
      this.errorMessage = 'This page is only available for students.';
      return;
    }
    this.studentId = currentUser.id;
    this.loadMyReclamations();
    this.initVoiceRecognition();
  }

  ngOnDestroy(): void {
    if (this.recognition) {
      this.recognition.stop();
    }
  }

  loadMyReclamations(): void {
    if (!this.studentId) return;
    this.loading = true;
    this.reclamationService.getByStudent(this.studentId).subscribe({
      next: (data) => {
        this.reclamations = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  submit(): void {
    if (!this.studentId) return;
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.formModel.sujet?.trim() || !this.formModel.description?.trim()) {
      this.errorMessage = 'Subject and description are required.';
      return;
    }

    const payload: Reclamation = {
      sujet: this.formModel.sujet.trim(),
      description: this.formModel.description.trim(),
      studentId: this.studentId
    };

    this.reclamationService.create(payload).subscribe({
      next: () => {
        this.successMessage = 'Reclamation created successfully.';
        this.resetForm();
        this.loadMyReclamations();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'An error occurred while creating the reclamation.';
      }
    });
  }

  resetForm(): void {
    this.formModel = {
      sujet: '',
      description: ''
    };
  }

  getStatusLabel(status?: string): string {
    if (status === 'RESOLUE') return 'Processed';
    if (status === 'EN_COURS' || status === 'IN_PROGRESS') return 'In Progress';
    return 'Pending';
  }

  getStatusClass(status?: string): string {
    if (status === 'RESOLUE') return 'status-processed';
    if (status === 'EN_COURS' || status === 'IN_PROGRESS') return 'status-progress';
    return 'status-pending';
  }

  private initVoiceRecognition(): void {
    const browserWindow = window as any;
    const SpeechRecognition = browserWindow.SpeechRecognition || browserWindow.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      this.speechSupported = false;
      return;
    }

    this.speechSupported = true;
    this.recognition = new SpeechRecognition();
    this.recognition.lang = 'en-US';
    this.recognition.continuous = true;
    this.recognition.interimResults = false;

    this.recognition.onresult = (event: any) => {
      let transcript = '';
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        transcript += event.results[i][0].transcript;
      }
      const clean = transcript.trim();
      if (!clean) return;

      if (this.recordingField === 'sujet') {
        this.formModel.sujet = `${this.formModel.sujet} ${clean}`.trim();
      } else {
        this.formModel.description = `${this.formModel.description} ${clean}`.trim();
      }
    };

    this.recognition.onerror = () => {
      this.isRecording = false;
      this.errorMessage = 'Voice recognition failed. Please try again.';
    };

    this.recognition.onend = () => {
      this.isRecording = false;
    };
  }

  toggleVoiceInput(field: 'sujet' | 'description'): void {
    if (!this.speechSupported || !this.recognition) {
      this.errorMessage = 'Voice input is not supported in this browser.';
      return;
    }

    this.errorMessage = '';
    if (this.isRecording) {
      this.recognition.stop();
      return;
    }

    this.recordingField = field;
    this.isRecording = true;
    this.recognition.start();
  }

  toggleChatbot(): void {
    this.showChatbot = !this.showChatbot;
  }

  useQuickPrompt(type: 'access' | 'payment' | 'bug' | 'other'): void {
    const templates: Record<string, { sujet: string; description: string; prompt: string }> = {
      access: {
        sujet: 'Access problem',
        description: 'I cannot access my account/course. Please verify my access rights and restore access.',
        prompt: 'I cannot access my account.'
      },
      payment: {
        sujet: 'Payment issue',
        description: 'I completed the payment but my access is still blocked. Please verify my payment status.',
        prompt: 'I paid but still no access.'
      },
      bug: {
        sujet: 'Platform bug report',
        description: 'I encountered a technical issue on the platform. Please investigate and fix it.',
        prompt: 'I found a bug on the platform.'
      },
      other: {
        sujet: 'General complaint',
        description: 'I want to report an issue and request support from the admin team.',
        prompt: 'I need help with another issue.'
      }
    };
    const tpl = templates[type];
    this.formModel.sujet = tpl.sujet;
    this.formModel.description = tpl.description;
    this.chatbotMessages.push({ role: 'user', text: tpl.prompt });
    this.chatbotMessages.push({ role: 'bot', text: 'Done. I pre-filled your form. You can edit it and submit.' });
    this.showChatbot = true;
  }

  sendChatbotMessage(): void {
    const text = this.chatbotInput.trim();
    if (!text) return;
    this.chatbotMessages.push({ role: 'user', text });
    this.chatbotInput = '';
    this.isChatbotLoading = true;

    this.reclamationService.assistWithChatbot({
      message: text,
      sujet: this.formModel.sujet,
      description: this.formModel.description
    }).subscribe({
      next: (res) => {
        if (res.suggestedSubject && !this.formModel.sujet.trim()) {
          this.formModel.sujet = res.suggestedSubject;
        }
        if (res.suggestedDescription && !this.formModel.description.trim()) {
          this.formModel.description = res.suggestedDescription;
        }
        this.chatbotMessages.push({
          role: 'bot',
          text: res.reply || 'I updated your complaint draft.'
        });
        this.isChatbotLoading = false;
      },
      error: () => {
        this.chatbotMessages.push({
          role: 'bot',
          text: 'AI service is currently unavailable. I kept your message; please continue editing manually.'
        });
        this.isChatbotLoading = false;
      }
    });
  }
}
