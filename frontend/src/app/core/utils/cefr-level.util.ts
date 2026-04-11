/** Niveaux CECRL gérés par les espaces forum. */
export const CEFR_FORUM_LEVELS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as const;
export type CefrForumCode = (typeof CEFR_FORUM_LEVELS)[number];

/**
 * Extrait A1…C2 depuis une chaîne (profil utilisateur, certificat, etc.).
 */
export function parseCefrFromProfile(raw: string | null | undefined): CefrForumCode | null {
  if (!raw?.trim()) {
    return null;
  }
  const m = raw.trim().toUpperCase().match(/\b(A1|A2|B1|B2|C1|C2)\b/);
  return m ? (m[1] as CefrForumCode) : null;
}

/**
 * Étudiant : un seul niveau ouvert (celui du profil). Tuteur / admin : tous.
 * Sans niveau connu pour un étudiant → seul A1 est proposé (point d’entrée minimal).
 */
export function isForumLevelUnlockedForUser(
  targetLevel: string,
  accountRole: string | undefined,
  englishLevel: string | undefined
): boolean {
  const role = (accountRole || 'STUDENT').toUpperCase();
  if (role === 'TUTOR' || role === 'ADMIN') {
    return true;
  }
  const want = parseCefrFromProfile(targetLevel);
  if (!want) {
    return false;
  }
  const mine = parseCefrFromProfile(englishLevel);
  if (!mine) {
    return want === 'A1';
  }
  return want === mine;
}
