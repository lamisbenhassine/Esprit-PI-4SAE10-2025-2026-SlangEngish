import { UserProfile } from '../models/user-profile.types';

/**
 * Données de secours si le microservice user (port 8010) est arrêté ou la base est vide.
 * Aligné sur backend/sql/seed_demo_fil_et_discussions.sql
 */
export const DEMO_USER_PROFILES: UserProfile[] = [
  { id: 2, firstName: 'Sara', lastName: 'Mansour', email: 'sara.demo@test.local', accountRole: 'STUDENT', englishLevel: 'A2' },
  { id: 3, firstName: 'Youssef', lastName: 'Khelil', email: 'youssef.demo@test.local', accountRole: 'STUDENT', englishLevel: 'B1' },
  { id: 4, firstName: 'Inès', lastName: 'Trabelsi', email: 'ines.demo@test.local', accountRole: 'STUDENT', englishLevel: 'C1' },
  { id: 5, firstName: 'Mehdi', lastName: 'Gharbi', email: 'mehdi.demo@test.local', accountRole: 'STUDENT', englishLevel: 'A1' },
  { id: 11, firstName: 'Marie', lastName: 'Dupont', email: 'marie.tutor@test.local', accountRole: 'TUTOR', englishLevel: 'C2' },
  { id: 12, firstName: 'James', lastName: 'Wilson', email: 'james.tutor@test.local', accountRole: 'TUTOR', englishLevel: 'C1' },
  { id: 13, firstName: 'Issra', lastName: 'Belhabib', email: 'issra.demo@test.local', accountRole: 'STUDENT', englishLevel: 'B1' },
  { id: 19, firstName: 'Nadia', lastName: 'Ben Salem', email: 'nadia.tutor@test.local', accountRole: 'TUTOR', englishLevel: 'C2' },
  { id: 20, firstName: 'Omar', lastName: 'Haddad', email: 'omar.tutor@test.local', accountRole: 'TUTOR', englishLevel: 'C1' }
];

const byId = new Map(DEMO_USER_PROFILES.map(u => [u.id, u]));

export function demoUserById(id: number): UserProfile | undefined {
  return byId.get(id);
}

export function demoUsersByRole(role: 'TUTOR' | 'STUDENT'): UserProfile[] {
  const r = role.toUpperCase();
  return DEMO_USER_PROFILES.filter(u => (u.accountRole || '').toUpperCase() === r);
}
