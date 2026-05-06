import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GESTION_CLASSES_API_BASE } from '../../api-config';

export interface ProfessorAvailabilitySlotPayload {
  slotCode: string;
  /** Texte libre (plan de séance). */
  description?: string;
}

export interface ProfessorAvailabilityDto {
  slots: ProfessorAvailabilitySlotPayload[];
}

@Injectable({
  providedIn: 'root'
})
export class ProfessorAvailabilityService {
  private readonly apiUrl = `${GESTION_CLASSES_API_BASE}/professor-availability`;

  constructor(private readonly http: HttpClient) {}

  get(professorUserId: number): Observable<ProfessorAvailabilityDto> {
    const params = new HttpParams().set('professorUserId', String(professorUserId));
    return this.http.get<ProfessorAvailabilityDto>(this.apiUrl, { params });
  }

  save(professorUserId: number, slots: ProfessorAvailabilitySlotPayload[]): Observable<ProfessorAvailabilityDto> {
    const params = new HttpParams().set('professorUserId', String(professorUserId));
    return this.http.put<ProfessorAvailabilityDto>(this.apiUrl, { slots }, { params });
  }
}
