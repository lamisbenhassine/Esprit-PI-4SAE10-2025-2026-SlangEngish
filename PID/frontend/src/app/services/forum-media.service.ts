import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_BASE = '/api/forum';

/**
 * Upload via XMLHttpRequest : avec `provideHttpClient(withFetch())`, HttpClient + FormData
 * peut mal passer par le proxy dev (404 « Not Found »). XHR + multipart est le chemin fiable.
 */
@Injectable({ providedIn: 'root' })
export class ForumMediaService {
  upload(file: File): Observable<{ url: string }> {
    return new Observable(observer => {
      const url = `${API_BASE}/media/upload`;
      const xhr = new XMLHttpRequest();
      xhr.open('POST', url);
      xhr.responseType = 'json';
      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          observer.next(xhr.response as { url: string });
          observer.complete();
          return;
        }
        observer.error(
          new HttpErrorResponse({
            error: xhr.response,
            status: xhr.status,
            statusText: xhr.statusText || 'Error',
            url
          })
        );
      };
      xhr.onerror = () => {
        observer.error(
          new HttpErrorResponse({
            error: { error: 'Erreur réseau (vérifiez que le forum tourne sur le port 8040).' },
            status: 0,
            statusText: 'Unknown Error',
            url
          })
        );
      };
      const fd = new FormData();
      fd.append('file', file);
      xhr.send(fd);
    });
  }

  /** Message lisible (snackbar) quand l’upload échoue. */
  describeUploadError(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'Serveur forum injoignable. Démarrez le microservice forum (port 8040) et ng serve avec proxy.';
    }
    if (err.status === 413) {
      return 'Fichier trop volumineux pour le serveur.';
    }
    if (err.status === 404) {
      return 'Upload introuvable (404). Démarrez le microservice forum (8040), recompilez-le, et utilisez ng serve avec proxy.conf.json.';
    }
    const body = err.error;
    if (body && typeof body === 'object' && 'error' in body) {
      return String((body as { error: string }).error);
    }
    if (typeof body === 'string' && body.length > 0 && body.length < 300) {
      return body;
    }
    return `Échec du téléversement (HTTP ${err.status}).`;
  }
}
