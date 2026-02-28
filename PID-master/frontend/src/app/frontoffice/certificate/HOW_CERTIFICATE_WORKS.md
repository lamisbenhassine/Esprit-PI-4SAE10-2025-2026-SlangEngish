# Certificate Feature – Full Explanation (Code + Logic)

This document explains **every part** of the SlangEnglish certificate: the rule, backend (DTO, repository, service, controller), and frontend (API, component, template, styles, routing, menu). Nothing is skipped.

---

## 1. The certificate rule (what we implement)

- A student **earns the certificate** when they have **passed at least 5 different evaluations** with **≥ 50%** in at least one **submitted** attempt per evaluation.
- **“Passed”** = for that evaluation, the student has at least one attempt with `status = SUBMITTED` and `score / maxScore >= 0.5` (maxScore = evaluation’s totalScore).
- **“5 different evaluations”** = we count **distinct evaluation IDs** that were passed, not total attempts. So 10 attempts on 2 evaluations still counts as 2.

---

## 2. Backend – Overview

The backend does three things:

1. **DTO**: Define the JSON we return (`eligible`, `passedCount`).
2. **Repository**: Load all SUBMITTED attempts for a user **with** the evaluation entity (so we have `totalScore`).
3. **Service**: From those attempts, compute how many distinct evaluations were “passed” (score ≥ 50% of totalScore), then set `eligible = (passedCount >= 5)`.
4. **Controller**: Expose `GET /api/certificate/eligibility/{userId}` and return the DTO.

---

## 3. Backend – DTO (response shape)

**File:** `backend/microservices/evaluation/src/main/java/com/evaluation/evaluation/dto/CertificateEligibilityResponse.java`

```java
package com.evaluation.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateEligibilityResponse {
    private boolean eligible;
    private int passedCount;
}
```

**Explanation:**

- This is the object sent back to the frontend.
- `eligible`: `true` when the user has passed **at least 5** different evaluations with ≥ 50%.
- `passedCount`: number of **distinct** evaluations the user has passed (with ≥ 50% in at least one submitted attempt).
- Lombok generates getters/setters and constructors; Spring serializes this to JSON like:  
  `{ "eligible": true, "passedCount": 5 }`.

---

## 4. Backend – Repository (load attempts with evaluation)

**File:** `backend/microservices/evaluation/src/main/java/com/evaluation/evaluation/repository/EvaluationAttemptRepository.java`

```java
@Query("SELECT a FROM EvaluationAttempt a JOIN FETCH a.evaluation WHERE a.userId = :userId AND a.status = :status")
List<EvaluationAttempt> findByUserIdAndStatusWithEvaluation(@Param("userId") Long userId, @Param("status") AttemptStatus status);
```

**Explanation:**

- We need **all SUBMITTED attempts** for a given user, and for each attempt we need the **evaluation** (to read `totalScore`).
- `EvaluationAttempt` has a `@ManyToOne` to `Evaluation`. Without `JOIN FETCH`, loading the evaluation would be lazy and could cause N+1 queries or a lazy load exception when we read `a.getEvaluation().getTotalScore()` in the service.
- `JOIN FETCH a.evaluation` loads the evaluation in the same query, so one DB round-trip gives us attempts + their evaluations.
- We filter by `userId` and `status = SUBMITTED` so we only consider finished attempts that were actually submitted (not IN_PROGRESS or EXPIRED).

---

## 5. Backend – Service interface (contract)

**File:** `backend/microservices/evaluation/src/main/java/com/evaluation/evaluation/service/EvaluationAttemptService.java`

Added method:

```java
com.evaluation.evaluation.dto.CertificateEligibilityResponse getCertificateEligibility(Long userId);
```

**Explanation:**

- The controller will call this method with the current user’s ID. The service is responsible for computing eligibility and passed count.

---

## 6. Backend – Service implementation (eligibility logic)

**File:** `backend/microservices/evaluation/src/main/java/com/evaluation/evaluation/service/impl/EvaluationAttemptServiceImpl.java`

**Import added:**

```java
import com.evaluation.evaluation.dto.CertificateEligibilityResponse;
```

**Method implementation:**

```java
@Override
public CertificateEligibilityResponse getCertificateEligibility(Long userId) {
    List<EvaluationAttempt> submitted = evaluationAttemptRepository.findByUserIdAndStatusWithEvaluation(userId, AttemptStatus.SUBMITTED);
    java.util.Set<Long> passedEvaluationIds = new java.util.HashSet<>();
    for (EvaluationAttempt a : submitted) {
        if (a.getScore() == null) continue;
        Evaluation ev = a.getEvaluation();
        if (ev == null || ev.getTotalScore() == null || ev.getTotalScore() <= 0) continue;
        double pct = a.getScore() / ev.getTotalScore();
        if (pct >= 0.5) {
            passedEvaluationIds.add(ev.getId());
        }
    }
    int passedCount = passedEvaluationIds.size();
    boolean eligible = passedCount >= 5;
    return new CertificateEligibilityResponse(eligible, passedCount);
}
```

**Explanation step by step:**

1. **Load attempts:**  
   `findByUserIdAndStatusWithEvaluation(userId, SUBMITTED)` returns all submitted attempts for that user, with evaluation loaded.

2. **Collect passed evaluation IDs:**  
   We use a `Set<Long>` so each evaluation is counted **once** even if the user passed it in multiple attempts.

3. **For each attempt:**  
   - Skip if `score` is null (shouldn’t happen for SUBMITTED, but safe).  
   - Skip if evaluation or `totalScore` is null or ≤ 0 (avoid division by zero).  
   - Compute `pct = score / totalScore`.  
   - If `pct >= 0.5` (50% or more), add `ev.getId()` to the set.

4. **Result:**  
   - `passedCount` = number of distinct evaluations passed.  
   - `eligible` = `passedCount >= 5`.  
   - Return a new `CertificateEligibilityResponse(eligible, passedCount)`.

---

## 7. Backend – Controller (REST endpoint)

**File:** `backend/microservices/evaluation/src/main/java/com/evaluation/evaluation/controller/CertificateController.java`

```java
package com.evaluation.evaluation.controller;

import com.evaluation.evaluation.dto.CertificateEligibilityResponse;
import com.evaluation.evaluation.service.EvaluationAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate")
@RequiredArgsConstructor
public class CertificateController {

    private final EvaluationAttemptService evaluationAttemptService;

    @GetMapping("/eligibility/{userId}")
    public ResponseEntity<CertificateEligibilityResponse> getEligibility(@PathVariable Long userId) {
        return ResponseEntity.ok(evaluationAttemptService.getCertificateEligibility(userId));
    }
}
```

**Explanation:**

- **URL:** `GET /api/certificate/eligibility/{userId}`  
  Example: `GET /api/certificate/eligibility/2`.
- The gateway routes `/api/**` to the evaluation service, so the frontend calls `http://localhost:8080/api/certificate/eligibility/2` (gateway port).
- The controller only delegates to the service and returns the DTO with status 200.

---

## 8. Frontend – API service (calling the backend)

**File:** `frontend/src/app/core/services/evaluation-api.service.ts`

**Constants / base URL:**  
`const API_URL = 'http://localhost:8080/api';`

**New methods:**

```typescript
getUserById(id: number): Observable<User> {
  return this.http.get<User>(`${API_URL}/users/${id}`);
}

getCertificateEligibility(userId: number): Observable<{ eligible: boolean; passedCount: number }> {
  return this.http.get<{ eligible: boolean; passedCount: number }>(`${API_URL}/certificate/eligibility/${userId}`);
}
```

**Explanation:**

- **getUserById(id):**  
  Used to get the current user’s `name` and `surname` for the certificate text. The backend already has `GET /api/users/{id}` (UserController).

- **getCertificateEligibility(userId):**  
  Calls our new endpoint and returns an observable of `{ eligible, passedCount }`. The component will subscribe and set `eligible`, `passedCount`, and then show either the certificate or the “not yet eligible” view.

---

## 9. Frontend – Certificate component (TypeScript)

**File:** `frontend/src/app/frontoffice/certificate/certificate.component.ts`

**Constants at top:**

```typescript
const PLATFORM_NAME = 'SlangEnglish';
const REQUIRED_PASSED = 5;
const MIN_PERCENT = 50;
```

**Component state:**

```typescript
loading = true;
eligible = false;
passedCount = 0;
studentName = '';
certificateDate = '';
```

**Explanation:**

- `loading`: true until we get the eligibility response (and optionally user).
- `eligible` / `passedCount`: come from the API.
- `studentName`: from `getUserById` (name + surname).
- `certificateDate`: formatted “today” for the certificate (e.g. “Monday, February 23, 2025”).

**ngOnInit:**

```typescript
ngOnInit(): void {
  this.load();
}
```

Calls `load()` once when the page is opened.

**load():**

```typescript
load(): void {
  this.loading = true;
  const userId = this.currentUser.getUserId();
  this.api.getCertificateEligibility(userId).subscribe({
    next: (res) => {
      this.eligible = res.eligible;
      this.passedCount = res.passedCount;
      this.certificateDate = new Date().toLocaleDateString('en-US', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
      this.loadStudentName(userId);
      this.loading = false;
    },
    error: () => {
      this.loading = false;
      this.snackBar.open('Failed to load certificate status', 'Close', { duration: 3000 });
      this.router.navigate(['/frontoffice/evaluations']);
    }
  });
}
```

**Explanation:**

- Gets `userId` from `CurrentUserService`.
- Calls `getCertificateEligibility(userId)`. On success:  
  - Sets `eligible` and `passedCount`.  
  - Sets `certificateDate` to a long, readable date (e.g. “Monday, February 23, 2025”).  
  - Calls `loadStudentName(userId)` to fetch name/surname (async, so the certificate may show “Student” briefly then update).  
  - Sets `loading = false`.  
- On error: stop loading, show snackbar, redirect to evaluations list.

**loadStudentName(userId):**

```typescript
private loadStudentName(userId: number): void {
  this.api.getUserById(userId).subscribe({
    next: (user) => {
      this.studentName = [user.name, user.surname].filter(Boolean).join(' ') || 'Student';
    },
    error: () => {
      this.studentName = 'Student';
    }
  });
}
```

**Explanation:**

- Fetches the user by ID.  
- Builds display name from `name` and `surname`; if both missing, uses `'Student'`.  
- On error, still use `'Student'` so the certificate never breaks.

**Getters used in the template:**

```typescript
get platformName(): string {
  return PLATFORM_NAME;
}

get congratulationMessage(): string {
  if (this.passedCount >= REQUIRED_PASSED) {
    return `Congratulations! You have successfully passed ${this.passedCount} evaluation${this.passedCount === 1 ? '' : 's'} with ${MIN_PERCENT}% or above.`;
  }
  return `Keep going! You have passed ${this.passedCount} out of ${REQUIRED_PASSED} evaluations with ${MIN_PERCENT}% or above.`;
}

get requiredPassed(): number {
  return REQUIRED_PASSED;
}
```

**Explanation:**

- **platformName:** Always “SlangEnglish” (used in header and seal).
- **congratulationMessage:**  
  - If eligible: “Congratulations! You have successfully passed X evaluations with 50% or above.”  
  - If not: “Keep going! You have passed X out of 5 evaluations with 50% or above.”
- **requiredPassed:** Returns 5 so the template can show “X/5” and “Pass (5 − X) more…”.

**Navigation:**

```typescript
backToEvaluations(): void {
  this.router.navigate(['/frontoffice/evaluations']);
}
```

Used by both the certificate view and the “not eligible” view.

---

## 10. Frontend – Certificate template (HTML)

**File:** `frontend/src/app/frontoffice/certificate/certificate.component.html`

**Structure in words:**

1. **Loading:**  
   If `loading`, show a spinner and “Loading…”.

2. **When not loading:**  
   - **If eligible:**  
     - Show the certificate card (platform name, “Certificate of Achievement”, “This is to certify that”, student name, congratulation message, date, seal with SlangEnglish).  
     - Show “Back to evaluations” button.  
   - **If not eligible:**  
     - Show “Certificate not yet available”, the same congratulation message (X/5), a progress bar, hint “Pass N more…”, and “Go to evaluations” button.

**Relevant snippets:**

**Loading:**

```html
<div class="loading-state" *ngIf="loading">
  <mat-icon class="spin">refresh</mat-icon>
  <p>Loading...</p>
</div>
```

**Certificate (eligible):**

```html
<div class="certificate-wrapper" *ngIf="eligible">
  <div class="certificate">
    <div class="certificate-border">
      <div class="certificate-inner">
        <div class="certificate-header">
          <div class="platform-logo">{{ platformName }}</div>
          <div class="certificate-badge">
            <mat-icon>verified</mat-icon>
            <span>Certificate of Achievement</span>
          </div>
        </div>
        <div class="certificate-body">
          <p class="certificate-label">This is to certify that</p>
          <h1 class="student-name">{{ studentName }}</h1>
          <p class="congrats-message">{{ congratulationMessage }}</p>
          <p class="certificate-date">{{ certificateDate }}</p>
        </div>
        <div class="certificate-footer">
          <div class="seal">
            <mat-icon>school</mat-icon>
            <span>{{ platformName }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
  <button mat-raised-button class="back-btn" (click)="backToEvaluations()">
    <mat-icon>arrow_back</mat-icon>
    Back to evaluations
  </button>
</div>
```

- **platformName** = “SlangEnglish” (header + seal).  
- **studentName** = from API (name + surname).  
- **congratulationMessage** = success or progress text.  
- **certificateDate** = long date set in `load()`.

**Not eligible:**

```html
<div class="not-eligible" *ngIf="!eligible">
  <div class="not-eligible-card">
    <mat-icon class="icon-lock">lock</mat-icon>
    <h2>Certificate not yet available</h2>
    <p class="progress-text">{{ congratulationMessage }}</p>
    <div class="progress-bar-wrap">
      <div class="progress-fill" [style.width.%]="(passedCount / requiredPassed) * 100"></div>
    </div>
    <p class="hint">Pass {{ requiredPassed - passedCount }} more evaluation(s) with 50% or above to unlock your {{ platformName }} certificate.</p>
    <button mat-raised-button class="back-btn" (click)="backToEvaluations()">
      <mat-icon>assignment</mat-icon>
      Go to evaluations
    </button>
  </div>
</div>
```

- Progress bar width = `(passedCount / 5) * 100` percent.  
- Hint uses `requiredPassed - passedCount` (how many more to pass).

---

## 11. Frontend – Certificate styles (CSS)

**File:** `frontend/src/app/frontoffice/certificate/certificate.component.css`

**What each part does:**

- **.certificate-page:**  
  Container with padding and a short fade-in animation.

- **.loading-state:**  
  Centered spinner and “Loading…” text; `.spin` rotates the icon.

- **.certificate-wrapper:**  
  Max width 720px, centered; wraps the whole certificate block.

- **.certificate:**  
  Parchment-like gradient background (`#faf8f3` → `#f5f0e6`), rounded corners, shadow so it looks like a sheet of paper.

- **.certificate-border:**  
  Gold border (`#c9a227`), inner padding; `::before` adds a subtle inner border for a “frame” effect.

- **.certificate-header:**  
  Centered; platform name (SlangEnglish) in large serif; “Certificate of Achievement” with verified icon; bottom border.

- **.platform-logo:**  
  Uppercase, letter-spacing, Georgia/serif, dark color.

- **.certificate-body:**  
  “This is to certify that”, then student name (big serif), then congratulation message, then date in italic.

- **.student-name:**  
  Large (36px), bold, serif.

- **.certificate-footer:**  
  Top border; contains the “seal” (circle with gradient, school icon, “SlangEnglish” text).

- **.seal:**  
  Round badge with purple gradient, white text/icon.

- **.back-btn:**  
  Purple gradient button, used for “Back to evaluations” and “Go to evaluations”.

- **.not-eligible / .not-eligible-card:**  
  Centered card with lock icon, title, progress text, progress bar, hint, and button.

- **.progress-bar-wrap / .progress-fill:**  
  Grey track; fill is a gradient (purple) and width is bound in the template to `(passedCount/requiredPassed)*100`.

- **@media (max-width: 600px):**  
  Reduces padding and font sizes so the certificate stays readable on small screens.

---

## 12. Frontend – Routing (how we reach the certificate page)

**File:** `frontend/src/app/frontoffice/frontoffice-routing.module.ts`

**Import:**

```typescript
import { CertificateComponent } from './certificate/certificate.component';
```

**Route (inside the layout children):**

```typescript
{ path: 'certificate', component: CertificateComponent }
```

**Explanation:**

- Frontoffice routes are under the layout (sidebar + content).  
- Path `certificate` means URL: `/frontoffice/certificate`.  
- When the user goes to that URL, Angular loads `CertificateComponent` inside the layout’s router outlet.

---

## 13. Frontend – Module declaration

**File:** `frontend/src/app/frontoffice/frontoffice.module.ts`

**Import:**

```typescript
import { CertificateComponent } from './certificate/certificate.component';
```

**Declaration:**

```typescript
declarations: [
  // ... other components
  CertificateComponent
],
```

**Explanation:**

- So Angular knows about `CertificateComponent` and can use it in the route and in templates.  
- The certificate template uses `mat-icon` and `mat-raised-button`, which come from `SharedModule` (or Material modules) already imported in the frontoffice module.

---

## 14. Frontend – Sidebar menu (how the user opens the certificate)

**File:** `frontend/src/app/frontoffice/layout/layout.component.ts`

**Menu item added:**

```typescript
frontofficeMenuItems: MenuItem[] = [
  { id: 'dashboard', label: 'Dashboard', icon: 'dashboard', routerLink: '/frontoffice/dashboard' },
  { id: 'evaluations', label: 'Evaluations', icon: 'assignment', routerLink: '/frontoffice/evaluations' },
  { id: 'certificate', label: 'My Certificate', icon: 'workspace_premium', routerLink: '/frontoffice/certificate' },
  { id: 'profile', label: 'Profile', icon: 'person', routerLink: '/frontoffice/profile' },
  // ...
];
```

**Explanation:**

- “My Certificate” appears in the sidebar with a trophy-style icon (`workspace_premium`).  
- Clicking it navigates to `/frontoffice/certificate`, which loads the certificate component.  
- The component then calls the API and shows either the certificate (if eligible) or the progress view (if not).

---

## 15. End-to-end flow (summary)

1. **User** clicks “My Certificate” in the sidebar → navigates to `/frontoffice/certificate`.
2. **CertificateComponent** runs `ngOnInit()` → `load()`.
3. **load()** gets `userId` from `CurrentUserService`, then calls **getCertificateEligibility(userId)**.
4. **Backend** receives `GET /api/certificate/eligibility/{userId}`:
   - Loads all SUBMITTED attempts for that user with evaluation (repository).
   - For each attempt, if `score / evaluation.totalScore >= 0.5`, adds that evaluation’s ID to a set (service).
   - `passedCount` = set size; `eligible` = (passedCount >= 5).
   - Returns `{ eligible, passedCount }`.
5. **Frontend** receives the response:
   - Sets `eligible`, `passedCount`, and `certificateDate`.
   - Calls **getUserById(userId)** to get name/surname and sets `studentName`.
   - Sets `loading = false`.
6. **Template**:
   - If `eligible`: shows the certificate (SlangEnglish, student name, congratulation message, date, seal) and “Back to evaluations”.
   - If not: shows “Certificate not yet available”, progress message, progress bar (passedCount/5), hint, and “Go to evaluations”.

That is the complete certificate feature from rule to UI, with every file and logic step covered.
