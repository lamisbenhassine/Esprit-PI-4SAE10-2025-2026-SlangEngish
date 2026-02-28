# How the Warnings and "Days Left" Work (Evaluations List)

This file explains how the **warning boxes** (7 days / 3 days) and the **"X days left"** label on each evaluation card are implemented.

---

## 1. Where the data comes from

Each evaluation has a **deadline**: `dateEnd` (e.g. `"2025-02-28T23:59:00"`).  
The list is loaded in `load()` and stored in `this.evaluations`. All logic uses this list and the **current time** to compute "time left" and who is in the warning lists.

---

## 2. How "days left" is computed (in the TypeScript file)

We use one function that, given a deadline string, returns a short text like **"1 day left"** or **"3 hours left"**.

### Code (evaluations-list.component.ts)

```typescript
/** Human-readable time left until deadline (e.g. "1 day left", "3 hours left"). */
getTimeLeftDisplay(dateEnd: string | undefined): string {
  if (!dateEnd) return '—';
  const end = new Date(dateEnd).getTime();   // deadline in milliseconds
  const now = this.now.getTime();            // current time in milliseconds
  const diffMs = end - now;                  // time left in ms

  if (diffMs <= 0) return 'Expired';

  const diffHours = diffMs / (60 * 60 * 1000);
  const diffDays = diffMs / (24 * 60 * 60 * 1000);

  if (diffHours < 1) return 'Less than 1 hour left';
  if (diffHours < 24) return `${Math.floor(diffHours)} hour${Math.floor(diffHours) === 1 ? '' : 's'} left`;
  if (diffDays < 2) return '1 day left';
  if (diffDays < 7) return `${Math.floor(diffDays)} days left`;
  return `${Math.floor(diffDays)} days left`;
}
```

### How it works

| Condition              | Example                    | Result                |
|------------------------|----------------------------|------------------------|
| No `dateEnd`           | -                          | `"—"`                 |
| Deadline already passed| end &lt; now               | `"Expired"`           |
| Less than 1 hour       | 30 min left                | `"Less than 1 hour left"` |
| 1–23 hours             | 5 hours left               | `"5 hours left"`      |
| 1 day (24h to &lt;48h) | tomorrow                   | `"1 day left"`        |
| 2–6 days               | 3 days left                 | `"3 days left"`       |
| 7+ days                | 10 days left                | `"10 days left"`      |

So for **each card**, we call `getTimeLeftDisplay(e.dateEnd)` and show that text. Evaluations with the same deadline (or same “bucket”, e.g. “1 day”) will show the same text.

---

## 3. How the 7-day and 3-day warning lists are built (TypeScript)

We have two **getters** that filter `this.evaluations` into lists used only for the warning boxes.

### 3.1 Evaluations ending in less than 7 days

```typescript
/** Evaluations whose deadline is in less than 7 days (for warning). */
get evaluationsDeadlineUnder7Days(): Evaluation[] {
  const now = this.now.getTime();
  const sevenDaysMs = 7 * 24 * 60 * 60 * 1000;   // 7 days in milliseconds
  return this.evaluations.filter(e => {
    if (!e.dateEnd) return false;
    const end = new Date(e.dateEnd).getTime();
    // Include only if: deadline is in the future AND within 7 days
    return end >= now && (end - now) < sevenDaysMs;
  });
}
```

- **Condition:** `end >= now` → not expired.  
- **Condition:** `(end - now) < sevenDaysMs` → less than 7 days from now.  
So this list = **all evaluations due in the next 7 days** (not past, not more than 7 days away).

### 3.2 Evaluations ending in 3 days or less (red warning)

```typescript
/** Evaluations whose deadline is in 3 days or less (for red urgent warning). */
get evaluationsDeadlineUnder3Days(): Evaluation[] {
  const now = this.now.getTime();
  const threeDaysMs = 3 * 24 * 60 * 60 * 1000;   // 3 days in milliseconds
  return this.evaluations.filter(e => {
    if (!e.dateEnd) return false;
    const end = new Date(e.dateEnd).getTime();
    // Include only if: deadline is in the future AND within 3 days
    return end >= now && (end - now) <= threeDaysMs;
  });
}
```

- Same idea, but with **3 days** and `<=` so “exactly 3 days left” is included.  
This list = **evaluations due in 3 days or less** (names shown in the red warning).

---

## 4. How the template uses this (HTML)

### 4.1 Red warning (3 days or less)

- Shown only when **not loading** and **there is at least one** evaluation in `evaluationsDeadlineUnder3Days`.
- The box lists **each evaluation by name**, and for each we show the same “time left” text and the “until” date.

```html
<!-- Urgent: evaluations ending in 3 days or less (red warning with names) -->
<div class="deadline-warning deadline-warning-urgent"
     *ngIf="!loading && evaluationsDeadlineUnder3Days.length > 0">
  <div class="deadline-warning-header">
    <mat-icon>error</mat-icon>
    <span>Ending in 3 days or less</span>
  </div>
  <p class="deadline-warning-intro">The following evaluations are due very soon:</p>
  <ul class="deadline-warning-list">
    <li *ngFor="let e of evaluationsDeadlineUnder3Days">
      <strong>{{ e.title }}</strong> — {{ getTimeLeftDisplay(e.dateEnd) }} (until {{ formatDate(e.dateEnd) }})
    </li>
  </ul>
</div>
```

So: **warning with red style** + **names** from `evaluationsDeadlineUnder3Days` + **days/hours left** from `getTimeLeftDisplay(e.dateEnd)`.

In addition, **every evaluation that has 3 days or less left** is also styled in **red in the list**: the evaluation card gets a red top strip, red border/shadow, and the "time left" badge on the card is red. So the warning lists the names, and the same evaluations are visually highlighted in red in the grid below.

### 4.2 Red styling on evaluation cards (3 days or less)

For each evaluation card we check if its deadline is in 3 days or less. If yes, we add the class `eval-card-urgent` to the card and `eval-time-left-urgent` to the "time left" badge. That way the card and the badge are shown in red, not only the warning box.

**TypeScript – helper used by the template:**

```typescript
/** True if this evaluation's deadline is in 3 days or less (used to style the card in red). */
isDeadlineUnder3Days(e: Evaluation): boolean {
  if (!e.dateEnd) return false;
  const now = this.now.getTime();
  const end = new Date(e.dateEnd).getTime();
  const threeDaysMs = 3 * 24 * 60 * 60 * 1000;
  return end >= now && (end - now) <= threeDaysMs;
}
```

**HTML – card and time-left badge get urgent class when 3 days or less:**

```html
<div class="eval-card" *ngFor="let e of paginatedEvaluations" (click)="startEvaluation(e)"
     [class.eval-card-urgent]="isDeadlineUnder3Days(e)">
  <div class="card-top-strip"></div>
  <div class="eval-image" ...>
    ...
    <span class="eval-time-left"
          [class.expired]="getTimeLeftDisplay(e.dateEnd) === 'Expired'"
          [class.eval-time-left-urgent]="isDeadlineUnder3Days(e)">
      {{ getTimeLeftDisplay(e.dateEnd) }}
    </span>
    ...
  </div>
  ...
</div>
```

**CSS – red styles for urgent cards and badge:**

- **Card:** `.eval-card-urgent` has a red border and red-tinted box-shadow. `.eval-card-urgent .card-top-strip` uses a red gradient (`#dc2626` → `#ef4444`) and is slightly taller (6px).
- **Time-left badge:** `.eval-time-left.eval-time-left-urgent` has a red background (`rgba(220, 38, 38, 0.95)`) and a red shadow so the "X days left" text on the card is clearly in red.

So: the **same evaluations** that appear in the red warning (by name) are the ones that get the **red card** and **red "time left" badge** in the list.

### 4.3 Yellow warning (less than 7 days)

Same idea with the 7-day list and a yellow style (class `deadline-warning` only, no `deadline-warning-urgent`):

```html
<!-- Warning: evaluations ending in less than 7 days -->
<div class="deadline-warning"
     *ngIf="!loading && evaluationsDeadlineUnder7Days.length > 0">
  <div class="deadline-warning-header">
    <mat-icon>warning</mat-icon>
    <span>Evaluations ending in less than 7 days</span>
  </div>
  <ul class="deadline-warning-list">
    <li *ngFor="let e of evaluationsDeadlineUnder7Days">
      <strong>{{ e.title }}</strong> — {{ getTimeLeftDisplay(e.dateEnd) }} (until {{ formatDate(e.dateEnd) }})
    </li>
  </ul>
</div>
```

### 4.4 "Days left" on each evaluation card

For **each card** we call `getTimeLeftDisplay(e.dateEnd)` and show it in a span. If the result is `"Expired"`, we add the class `expired` so it can be styled (e.g. grey).

```html
<span class="eval-time-left" [class.expired]="getTimeLeftDisplay(e.dateEnd) === 'Expired'">
  {{ getTimeLeftDisplay(e.dateEnd) }}
</span>
```

So:
- **Same deadline** → same “time left” text (e.g. all with 1 day left show **"1 day left"**).
- **Expired** → text is “Expired” and the badge uses the `expired` class (grey).
- **3 days or less left** → the badge gets the `eval-time-left-urgent` class (red), and the card gets `eval-card-urgent` (red top strip and border). See section 4.2.

---

## 5. Why evaluations with the same deadline appear next to each other

In the TypeScript, **filtered evaluations are sorted by `dateEnd`** (ascending = soonest first):

```typescript
get filteredEvaluations(): Evaluation[] {
  const list = this.evaluations.filter(e => this.matchesSearch(e) && this.matchesFilter(e));
  return list.slice().sort((a, b) => {
    const endA = a.dateEnd ? new Date(a.dateEnd).getTime() : 0;
    const endB = b.dateEnd ? new Date(b.dateEnd).getTime() : 0;
    return endA - endB;   // sooner deadline first → same deadline next to each other
  });
}
```

So evaluations with the **same** (or very close) **deadline** end up **next to each other** in the list, and each card shows the same “days left” text when the deadline is the same.

---

## 6. Summary

| What you see              | How it’s done |
|---------------------------|---------------|
| “1 day left” on a card    | `getTimeLeftDisplay(e.dateEnd)` in the card template. |
| Red warning + names       | Getter `evaluationsDeadlineUnder3Days`; template shows it only when length > 0 and lists `e.title` + time left. |
| **Red card** (3 days or less) | `isDeadlineUnder3Days(e)` adds class `eval-card-urgent` (red top strip, border, shadow). |
| **Red "time left" badge** on those cards | `isDeadlineUnder3Days(e)` adds class `eval-time-left-urgent` (red background). |
| Yellow warning + names    | Getter `evaluationsDeadlineUnder7Days`; same idea in the template. |
| Same deadline next to each other | `filteredEvaluations` is sorted by `dateEnd` (ascending). |

All logic uses only **current time** and **evaluation.dateEnd**; no backend changes are required.
