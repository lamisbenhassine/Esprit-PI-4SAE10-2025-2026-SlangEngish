/** Langues cibles demandées pour le forum (scope réduit pour éviter les appels inutiles). */
export interface TranslationTarget {
  code: string;
  label: string;
}

export const TRANSLATION_TARGET_LANGUAGES: TranslationTarget[] = [
  { code: 'fr', label: 'Français' },
  { code: 'ar', label: 'العربية' },
  { code: 'de', label: 'Deutsch' },
  { code: 'it', label: 'Italiano' }
];
