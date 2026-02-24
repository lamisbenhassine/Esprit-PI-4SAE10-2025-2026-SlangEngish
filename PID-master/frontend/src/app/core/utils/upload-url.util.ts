/**
 * Normalizes upload URLs for display so photo and PDF load correctly.
 * Converts evaluation service port (8020) to gateway port (8080).
 */
export function getDisplayUploadUrl(uploadUrl: string | undefined): string {
  if (!uploadUrl?.trim()) return '';
  return uploadUrl.replace(/localhost:8020/, 'localhost:8080');
}
