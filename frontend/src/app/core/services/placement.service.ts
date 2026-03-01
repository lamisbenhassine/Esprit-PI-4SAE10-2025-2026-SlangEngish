import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = 'http://localhost:8030/api/inscription/placement';

export interface CertificateAnalysis {
  fileName: string;
  detectedLevel: string | null;
  recommendedPlanId: number | null;
  recommendedPlanName: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class PlacementService {
  constructor(private http: HttpClient) {}

  uploadCertificate(file: File): Observable<CertificateAnalysis> {
    const formData = new FormData();
    formData.append('certificate', file);
    return this.http.post<CertificateAnalysis>(`${API_URL}/upload-certificate`, formData);
  }
}

