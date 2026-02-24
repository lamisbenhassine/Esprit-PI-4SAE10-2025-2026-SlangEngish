export interface JobOffer {
  id?: number;
  title: string;
  description: string;
  company: string;
  location: string;
  contractType: 'CDI' | 'CDD' | 'STAGE' | 'ALTERNANCE' | 'FREELANCE';
  salary?: number;
  publishedAt?: string;
  deadline?: string;
  active: boolean;
}

export interface Application {
  id?: number;
  jobOfferId: number;
  applicantName: string;
  applicantEmail: string;
  coverLetter?: string;
  cvUrl?: string;
  coverLetterUrl?: string;
  status?: 'PENDING' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
  appliedAt?: string;
}
