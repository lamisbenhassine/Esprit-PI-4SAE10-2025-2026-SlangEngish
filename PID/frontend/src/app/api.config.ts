import { environment } from '../environments/environment';

/** Base URL of the API Gateway (dev: http://localhost:8100/api, prod: /api). */
export const API_URL = environment.apiUrl || 'http://localhost:8100/api';

/** Gestion Cours microservice via gateway. */
export const GESTION_COURS_API_BASE = API_URL;
export const API_CHAPTERS_BASE = `${API_URL}/chapters`;

/** Gestion Classes microservice via gateway. */
export const GESTION_CLASSES_API_BASE = API_URL;

/** Evaluation microservice via gateway. */
export const EVALUATION_API_URL = `${API_URL}/evaluation`;

/** Smart Notebook microservice via gateway. */
export const NOTEBOOK_API_URL = `${API_URL}/notebook`;

/** Public uploads served by evaluation service via gateway. */
export const UPLOADS_URL = `${API_URL}/uploads`;

/** Forum microservice (paths under /api/forum on the gateway). */
export const FORUM_API_BASE = `${API_URL}/forum`;

/** Inscription microservice (paths under /api/inscription on the gateway). */
export const INSCRIPTION_API_BASE = `${API_URL}/inscription`;
