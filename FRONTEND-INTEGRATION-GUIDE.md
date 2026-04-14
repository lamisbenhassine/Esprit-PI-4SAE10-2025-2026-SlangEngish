# Frontend integration guide — Notebook + Evaluation

Use this checklist when **copying this Angular project’s features into another Angular app** (e.g. your friend’s project with authentication).  
**Do not** copy Angular files into Java backends — only into the other app’s `src/`.

**Assumption:** the other project already has the **notebook** and **evaluation** microservices and an **API gateway** that exposes `/notebook` and `/evaluation` (and `/uploads` if you use file uploads).

---

## 1. Files and folders to copy (paths relative to `frontend/`)

### 1.1 Configuration (merge, do not blindly overwrite)

| Path | Detail |
|------|--------|
| `src/app/api.config.ts` | Defines `GATEWAY_URL`, `USER_API_URL`, `EVALUATION_API_URL`, `NOTEBOOK_API_URL`, `UPLOADS_URL`. Merge into the target project’s config and set the correct gateway host/port. |
| `proxy.conf.json` (repo root, next to `angular.json`) | Dev proxy entries for `/user`, `/evaluation`, `/notebook`, `/uploads`. Merge with their existing proxy rules. |
| `angular.json` | If you hit **component style budget** errors on Smart Notebook, you may need higher `anyComponentStyle` limits under `production.budgets` (optional). |

---

### 1.2 Core — services, models, utils (shared by notebook + evaluation UI)

| Path | Detail |
|------|--------|
| `src/app/core/services/notebook-api.service.ts` | HTTP client for Smart Notebook (notes, AI helpers, shared notes). |
| `src/app/core/services/evaluation-api.service.ts` | HTTP client for evaluations (list, take attempt, submit, etc.). |
| `src/app/core/services/current-user.service.ts` | Stores `userId` for API calls; evaluation list / take / notebook use it. **Adapt** to their auth or replace with their user service. |
| `src/app/core/models/evaluation.model.ts` | Types: `Evaluation`, `Question`, `EvaluationAttempt`, etc. |
| `src/app/core/models/index.ts` | Re-exports models (optional if you import models directly). |
| `src/app/core/utils/upload-url.util.ts` | Builds public URLs for uploaded images/PDFs via gateway. Used by evaluations list + take evaluation. |

---

### 1.3 Auth (Smart Notebook depends on this)

| Path | Detail |
|------|--------|
| `src/app/services/auth.service.ts` | Used by `SmartNotebookComponent` to read the logged-in user. **Copy** or **replace** imports in `smart-notebook.component.ts` with their equivalent (`AuthService.getCurrentUser()` pattern). |

If their auth lives elsewhere, keep their service and only change the **imports + constructor** in `smart-notebook.component.ts`.

---

### 1.4 Smart Notebook (full feature)

Copy the **entire folder**:

| Path |
|------|
| `src/app/frontoffice/smart-notebook/` |

Contains: `smart-notebook.component.ts`, `.html`, `.css`.

---

### 1.5 Evaluation — student (front office)

Copy these **folders** entirely (all files: `.ts`, `.html`, `.css`, and any `.md` helpers):

| Path | Detail |
|------|--------|
| `src/app/frontoffice/evaluations-list/` | List of evaluations, filters, deadline dialog. |
| `src/app/frontoffice/evaluations-list/deadline-warning-dialog.component.*` | Included in folder above. |
| `src/app/frontoffice/take-evaluation/` | Take an evaluation (questions, timer behavior, etc.). |
| `src/app/frontoffice/evaluation-results/` | Results after submission. |

---

### 1.6 Certificates (optional — only if you use QR / PDF certificate flow)

| Path |
|------|
| `src/app/frontoffice/certificate/` |

Includes certificate page + verify route component. Requires **`angularx-qrcode`** in `package.json` and `QRCodeModule` in the Angular module.

---

### 1.7 Back office — teacher / admin (optional)

Only if the target app has a **back office** module and you need to create/manage evaluations from the UI:

| Path |
|------|
| `src/app/backoffice/evaluations-management/` |
| `src/app/backoffice/evaluation-form/` |
| `src/app/backoffice/evaluation-questions/` |
| `src/app/backoffice/evaluation-attempts/` |

You must also merge routes in `backoffice-routing.module.ts` and declarations in `backoffice.module.ts` (same idea as front office).

---

## 2. Wire-up in the target Angular app (manual steps)

### 2.1 Routes

In their **`frontoffice-routing.module.ts`** (or equivalent), add child routes under the main layout, for example:

- `evaluations` → `EvaluationsListComponent`
- `evaluations/:id/take` → `TakeEvaluationComponent`
- `evaluations/:id/results` → `EvaluationResultsComponent`
- `notebook` → `SmartNotebookComponent`
- Optional: `certificate`, `certificate/verify`

Copy the pattern from this repo’s `src/app/frontoffice/frontoffice-routing.module.ts`.

### 2.2 Module declarations

In **`frontoffice.module.ts`** (or standalone routes):

- Add every copied component to `declarations` (or `imports` if standalone).
- **Imports** used in this project for these features include at least:
  - `SharedModule` (or equivalent Material exports)
  - `MatTabsModule` — Smart Notebook tabs
  - `MatProgressBarModule` — evaluations / UI
  - `QRCodeModule` — only if you copied certificate components

See this repo’s `src/app/frontoffice/frontoffice.module.ts` for the exact list.

### 2.3 Navigation

Update their **navbar / sidebar / layout** HTML to add links, e.g.:

- `/frontoffice/evaluations` (adjust base path to match their router)
- `/frontoffice/notebook`

Paths depend on how they load `FrontofficeModule` (e.g. `path: 'student', loadChildren: ...`).

### 2.4 `package.json` dependencies

Ensure the target project includes at least:

- `@angular/material`, `@angular/cdk` (and `BrowserAnimationsModule` in `app.config` or `AppModule`)
- `angularx-qrcode` — **only** if certificate feature is copied
- `html2pdf.js` — **only** if certificate PDF download is used

Align versions with their Angular major version (this repo uses Angular 18).

### 2.5 `npm` scripts

If they use `ng serve` with a proxy, ensure `angular.json` → `serve.options.proxyConfig` points to the merged `proxy.conf.json`.

---

## 3. Backend alignment (reminder — not copied from frontend)

The frontend expects:

| Gateway path | Typical use |
|--------------|-------------|
| `/notebook/**` | Smart Notebook microservice |
| `/evaluation/**` | Evaluation microservice |
| `/uploads/**` | Static uploads (evaluation images/PDFs) |
| `/user/**` | Users service (if your app calls it) |

Their gateway **Spring Cloud Gateway** routes must match these prefixes. Wrong paths → 404 in the browser.

---

## 4. Auth integration checklist

| Item | Action |
|------|--------|
| `CurrentUserService` | After login, set `userId` from their JWT/session (same as this app or adapt `notebook`/`evaluation` components). |
| `AuthService` | Smart Notebook reads current user for `userId`; point to their auth facade. |
| API calls | Notebook and evaluation APIs use `userId` query/body — ensure the logged-in user id matches backend expectations. |

---

## 5. Quick copy summary (minimal student app)

**Minimum copy list** for “evaluations + notebook” in the student UI:

1. `src/app/api.config.ts` (merge)  
2. `proxy.conf.json` (merge)  
3. `src/app/core/services/notebook-api.service.ts`  
4. `src/app/core/services/evaluation-api.service.ts`  
5. `src/app/core/services/current-user.service.ts` (or adapter)  
6. `src/app/core/models/evaluation.model.ts` + `index.ts`  
7. `src/app/core/utils/upload-url.util.ts`  
8. `src/app/services/auth.service.ts` (or change notebook imports)  
9. `src/app/frontoffice/smart-notebook/`  
10. `src/app/frontoffice/evaluations-list/`  
11. `src/app/frontoffice/take-evaluation/`  
12. `src/app/frontoffice/evaluation-results/`  

Then: routes, `declarations`, `imports`, menu links, auth wiring.

---

## 6. Verify after integration

1. `ng serve` with proxy → open notebook page → list/create note (check browser Network calls to `/notebook/...`).  
2. Open evaluations list → load evaluations (`/evaluation/...`).  
3. Take evaluation → submit → results page.  
4. If something fails, check **gateway URL** in `api.config.ts` and **CORS** on the gateway for the Angular dev origin.

---

*Generated for project PID — paths are relative to the repository `frontend/` folder unless stated otherwise.*
