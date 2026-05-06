import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../api.config';

const PLACEMENT_API = `${API_URL}/inscription/placement`;

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
    return this.http.post<CertificateAnalysis>(`${PLACEMENT_API}/upload-certificate`, formData);
  }
}

