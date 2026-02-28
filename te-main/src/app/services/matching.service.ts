import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MatchingResult {
  offerId: number;
  offerTitle: string;
  offerCompany: string;
  offerLocation: string;
  offerContractType: string;
  offerSalary: string;
  matchScore: number;
  matchPercent: number;
  matchLevel: string;
  locationScore: number;
  contractScore: number;
  salaryScore: number;
  keywordScore: number;
}

export interface StudentProfile {
  id?: number;
  studentId?: number;
  preferredLocation?: string;
  preferredContractType?: string;
  expectedSalary?: number;
  skills?: string;
}

@Injectable({ providedIn: 'root' })
export class MatchingService {
  private base = '/api/matching';

  constructor(private http: HttpClient) {}

  getMatchingOffers(studentId: number): Observable<MatchingResult[]> {
    return this.http.get<MatchingResult[]>(`${this.base}/${studentId}`);
  }

  getMatchScore(studentId: number, offerId: number): Observable<MatchingResult> {
    return this.http.get<MatchingResult>(`${this.base}/${studentId}/offer/${offerId}`);
  }

  getProfile(studentId: number): Observable<StudentProfile> {
    return this.http.get<StudentProfile>(`${this.base}/profile/${studentId}`);
  }

  saveProfile(studentId: number, profile: StudentProfile): Observable<StudentProfile> {
    return this.http.post<StudentProfile>(`${this.base}/profile/${studentId}`, profile);
  }
}