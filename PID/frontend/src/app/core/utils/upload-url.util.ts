import { UPLOADS_URL } from '../../api.config';

/**
 * Backend stores absolute upload URLs (often pointing directly to the evaluation service).
 * For the integrated app, always display uploads via the API Gateway.
 */
export function getDisplayUploadUrl(uploadUrl: string | undefined): string {
  const raw = (uploadUrl ?? '').trim();
  if (!raw) return '';

  // Already gateway-based
  if (raw.startsWith(UPLOADS_URL)) return raw;

  // If backend gave ".../uploads/<file>"
  const i = raw.indexOf('/uploads/');
  if (i >= 0) {
    const suffix = raw.slice(i + '/uploads/'.length);
    return `${UPLOADS_URL}/${suffix}`;
  }

  // If backend gave just a filename
  if (!raw.includes('/') && raw.length < 200) {
    return `${UPLOADS_URL}/${encodeURIComponent(raw)}`;
  }

  return raw;
}

