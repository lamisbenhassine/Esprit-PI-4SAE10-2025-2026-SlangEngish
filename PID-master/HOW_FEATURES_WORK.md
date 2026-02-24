# How Key Features Work

This document explains **what we use**, **how we built it**, and **where to find the code** for four features: Fill-in-the-blanks drag and drop, tab-leave = 0, Ollama question generation, and automatic correction.

---

## 1. Drag and drop in Fill in the Blanks

### What we use

- **HTML5 Drag and Drop API** (no extra library): the native browser `draggable` attribute and the events `dragstart`, `dragover`, `drop`.
- **Angular** for template bindings and component state (word bank, slots, and syncing the answer to submit).

### How it works

1. **Data**
   - The question has `paragraphText` with placeholders `____` and a list of `blanks` (each with `correctWord` and `positionIndex`).
   - The component keeps a **state per question**: `fillBlankState: Map<questionId, { bank: string[], slots: string[] }>`.
   - `bank` = words that can still be dragged (shuffled correct words).
   - `slots` = one word per blank in order (what the user has dropped).

2. **Word bank (draggable)**
   - Each word is a `<span>` with `draggable="true"` and `(dragstart)="onFillBlankDragStart(word, $event)"`.
   - On drag start we call `event.dataTransfer.setData('text/plain', word)` and `event.dataTransfer.effectAllowed = 'move'`.

3. **Drop zones (blanks)**
   - The paragraph is split by `____` into segments; between segments we render a drop zone `<span>` with `(dragover)="onFillBlankDragOver($event)"` and `(drop)="onFillBlankDrop(slotIndex, $event)"`.
   - In `dragover` we call `event.preventDefault()` and set `event.dataTransfer.dropEffect = 'move'` so the browser shows a “move” cursor.
   - In `drop` we call `event.preventDefault()`, get the word with `event.dataTransfer.getData('text/plain')`, check it’s in the bank and the slot is empty, then move the word from bank to `slots[slotIndex]` and call `syncFillBlankAnswer(questionId)`.

4. **Submitting the answer**
   - `syncFillBlankAnswer` builds the answer string as the comma-separated list of `slots` and stores it in `answers` (e.g. for `submitAnswer`). So the backend receives one string like `"word1,word2,word3"`.

5. **Removing a word from a blank**
   - A small “remove” button on each filled slot calls `removeFillBlankFromSlot(slotIndex)`, which puts the word back in the bank and clears that slot, then syncs the answer again.

### Where to find the code

| Part | Location |
|------|----------|
| **Frontend – template (drag/drop UI)** | `frontend/src/app/frontoffice/take-evaluation/take-evaluation.component.html` — block “FILL_BLANK: word bank at top, paragraph with blanks for drag & drop” (word bank `draggable`, `dragstart`; slots `dragover`, `drop`). |
| **Frontend – component logic** | `frontend/src/app/frontoffice/take-evaluation/take-evaluation.component.ts` — `fillBlankState`, `ensureFillBlankState`, `getFillBlankBank`, `getFillBlankSlots`, `getFillBlankSegments`, `onFillBlankDragStart`, `onFillBlankDragOver`, `onFillBlankDrop`, `removeFillBlankFromSlot`, `syncFillBlankAnswer`, `isFillBlankComplete`. |
| **Backend – model (blanks)** | `backend/microservices/evaluation/.../model/FillBlankQuestion.java` (paragraph + blanks), `.../model/Blank.java` (`correctWord`, `positionIndex`). |
| **Backend – saving answer** | Submit is the same as other types: `EvaluationAttemptController` + `submitAnswer` in service; the stored answer is the comma-separated string that the frontend sends. |

---

## 2. When the student opens another tab → get 0 automatically

### What we use

- **Page Visibility API**: `document.visibilityState` and the `visibilitychange` event on `document`. When the user switches tab, minimizes the window, or leaves the app, the page becomes “hidden.”
- **Backend**: one extra endpoint that finishes the attempt with **score 0** and marks it SUBMITTED (so they can’t continue).

### How it works

1. **Frontend**
   - In `ngOnInit` we register `document.addEventListener('visibilitychange', this.visibilityListener)`.
   - The listener calls `onVisibilityChange()`. It runs only when `document.visibilityState === 'hidden'` (user left the tab/window).
   - We guard so we don’t run twice or when there’s no active attempt: if `violationHandled`, `finishing`, no `attempt.id`, or still `loading`, we return.
   - We set `violationHandled = true` and `finishing = true`, then call the API `finishAttemptWithZero(this.attempt.id)`.
   - On success we navigate to the results page (same as normal finish) with `attemptId` in the query. On error we reset the flags and show a message.
   - In `ngOnDestroy` we remove the listener with `document.removeEventListener('visibilitychange', this.visibilityListener)`.

2. **Backend**
   - The endpoint loads the attempt; if status is not `IN_PROGRESS`, it returns the attempt as-is (no double finish).
   - It loads all `StudentAnswer` for that attempt, sets each `scoreAwarded` to `0.0`, saves them, then sets the attempt’s `score` to `0.0`, `endTime`, `status = SUBMITTED`, and saves the attempt.

So: **leave tab → visibility hidden → one-time call to finish-with-zero → backend sets all answer scores and attempt score to 0 and marks SUBMITTED → frontend redirects to results.**

### Where to find the code

| Part | Location |
|------|----------|
| **Frontend – listener and API call** | `frontend/src/app/frontoffice/take-evaluation/take-evaluation.component.ts` — `ngOnInit` / `ngOnDestroy`, `visibilityListener`, `onVisibilityChange()`, and the call to `this.api.finishAttemptWithZero(this.attempt.id)`. |
| **Frontend – API method** | `frontend/src/app/core/services/evaluation-api.service.ts` — `finishAttemptWithZero(attemptId)`: POST to `.../attempts/{attemptId}/finish-with-zero`. |
| **Backend – controller** | `backend/microservices/evaluation/.../controller/EvaluationAttemptController.java` — `POST /{attemptId}/finish-with-zero` → `evaluationAttemptService.finishAttemptWithZero(attemptId)`. |
| **Backend – service interface** | `backend/microservices/evaluation/.../service/EvaluationAttemptService.java` — method `finishAttemptWithZero(Long attemptId)`. |
| **Backend – service implementation** | `backend/microservices/evaluation/.../service/impl/EvaluationAttemptServiceImpl.java` — `finishAttemptWithZero`: load attempt, set all answer scores to 0, set attempt score 0 and status SUBMITTED, save. |

---

## 3. Generation of questions by Ollama

### What we use

- **Ollama** (local, free): runs the model (e.g. `llama3.2`) and exposes an HTTP API compatible with “chat completions” (e.g. `http://localhost:11434/v1/chat/completions`).
- **Backend**: a service that (1) extracts text from the uploaded PDF, (2) sends that text to Ollama with a prompt asking for 10 reading questions, (3) parses the JSON response and creates `ReadingQuestion` entities.

### How it works

1. **Frontend**
   - In the backoffice, on the evaluation’s “Questions” page, the teacher uploads a PDF for a Reading question and can click a button like “Generate 10 questions with AI.”
   - That calls `generateReadingQuestionsFromPdf({ evaluationId, pdfUrl, instructions, pointsPerQuestion })` (POST to `/api/reading-questions/generate-from-pdf`).

2. **Backend – controller**
   - `ReadingQuestionController` receives the request (evaluationId, pdfUrl, instructions, pointsPerQuestion), validates it, and calls `readingQuestionService.generateFromPdf(...)`.

3. **Backend – generateFromPdf (ReadingQuestionServiceImpl)**
   - Resolves the PDF file from `pdfUrl` (filename) using the same upload directory (e.g. `UploadPathConfig`).
   - Calls `pdfTextExtractionService.extractText(pdfPath)` to get raw text from the PDF (e.g. via a PDF library).
   - Calls `aiQuestionGeneratorService.generateQuestions(text)` to get a list of question strings from Ollama.
   - If the list is empty, throws (e.g. “Ollama not running / model not pulled”).
   - For each question string, creates a `ReadingQuestion` (questionText, pdfUrl, instructions, points, questionOrder), links it to the evaluation, and saves it. Returns the list of created questions.

4. **Backend – AiQuestionGeneratorService**
   - Builds a prompt: “Based on the following text … generate exactly 10 reading comprehension questions. Return ONLY a JSON array of 10 strings.”
   - Sends a POST to `ai.ollama.url` (default `http://localhost:11434/v1/chat/completions`) with body `{ "model": "llama3.2", "messages": [{ "role": "user", "content": "<prompt + text>" }], "stream": false }`.
   - Reads the response: `choices[0].message.content` is the model output string.
   - Parses that string as a JSON array of strings (with a fallback if the model wraps it in markdown or uses a different format). Returns up to 10 question texts.

So: **teacher uploads PDF and clicks “Generate” → backend reads PDF text → sends text to Ollama → parses 10 questions → creates 10 Reading questions in DB → frontend shows them.**

### Where to find the code

| Part | Location |
|------|----------|
| **Frontend – button and API call** | `frontend/src/app/backoffice/evaluation-questions/evaluation-questions.component.ts` — `generateReadingWithAi()` and the call to `this.api.generateReadingQuestionsFromPdf({...})`. |
| **Frontend – template (button / hint)** | `frontend/src/app/backoffice/evaluation-questions/evaluation-questions.component.html` — the block with “Generate 10 questions from this PDF” and the hint about Ollama (e.g. “ollama pull llama3.2”). |
| **Frontend – API method** | `frontend/src/app/core/services/evaluation-api.service.ts` — `generateReadingQuestionsFromPdf(params)`: POST to `.../reading-questions/generate-from-pdf`. |
| **Backend – controller** | `backend/microservices/evaluation/.../controller/ReadingQuestionController.java` — `POST /generate-from-pdf` and call to `readingQuestionService.generateFromPdf(...)`. |
| **Backend – orchestration (PDF → text → AI → save)** | `backend/microservices/evaluation/.../service/impl/ReadingQuestionServiceImpl.java` — method `generateFromPdf(Long evaluationId, String pdfUrl, String instructions, double pointsPerQuestion)`: resolve PDF path, extract text, call AI service, create and save Reading questions. |
| **Backend – PDF text extraction** | `backend/microservices/evaluation/.../service/PdfTextExtractionService.java` — `extractText(Path pdfPath)` (reads PDF and returns plain text). |
| **Backend – Ollama call and parsing** | `backend/microservices/evaluation/.../service/AiQuestionGeneratorService.java` — `generateQuestions(String pdfText)`: build prompt, POST to `ai.ollama.url`, parse response, return list of question strings. |
| **Backend – config** | `backend/microservices/evaluation/.../resources/application.properties` — `ai.ollama.url`, `ai.ollama.model` (optional). |

---

## 4. Automatic correction of the evaluation

### What we use

- **Backend only**: when the attempt is **finished** (student clicks “Finish” or it’s finished with 0 after tab leave), the service loads all submitted `StudentAnswer`s and, for each question type that can be auto-graded, compares the student’s answer to the correct one and sets `scoreAwarded`. The total attempt score is the sum of these.

### How it works

1. **When correction runs**
   - When the frontend calls **finish attempt** (normal or finish-with-zero), the backend runs the logic that sets `attempt.setScore(...)` and saves. For **normal** finish, that score is computed by `calculateScore(attempt)`. For **finish-with-zero**, the score is set to 0 directly (no calculation).

2. **calculateScore (automatic grading)**
   - The method iterates over each `StudentAnswer` of the attempt and, depending on `question.getQuestionType()`:
     - **MCQ**: the answer must have exactly one selected option; that option’s `isCorrect` is checked. If true, `scoreAwarded = question.getPoints()`, else 0.
     - **MSQ**: all correct options must be selected and no incorrect ones. The code loads options for the question, gets correct option IDs, and checks that the selected set equals the correct set (same size, all correct selected, none incorrect). If so, full points; else 0.
     - **FILL_BLANK**: the answer is stored as a comma-separated string (order of blanks). The code loads the `FillBlankQuestion` and its `Blank`s (sorted by `positionIndex`), splits the student’s string by comma, and compares each word to the corresponding `correctWord` (trim, case-insensitive). If all match, full points; else 0.
     - **READING / WRITING**: in the default implementation no automatic points are given (typically for manual grading later). So `scoreAwarded` stays null or 0 unless you add logic or a teacher sets it.
   - After deciding each answer’s result, the code sets `answer.setScoreAwarded(...)` and adds the points to `totalScore`. At the end, `attempt.setScore(totalScore)` and the attempt and answers are saved (in `finishAttempt`).

3. **Result**
   - The attempt’s `score` is the sum of all `scoreAwarded` for that attempt. The results page (and any stats) read this score from the attempt.

So: **finish attempt → load all answers → for each answer run the rule for its question type (MCQ / MSQ / FILL_BLANK) → set scoreAwarded and sum → save attempt score.**

### Where to find the code

| Part | Location |
|------|----------|
| **Backend – finish attempt (normal)** | `backend/microservices/evaluation/.../service/impl/EvaluationAttemptServiceImpl.java` — `finishAttempt(Long attemptId)`: load attempt and answers, set end time and status, call `calculateScore(attempt)`, set `attempt.setScore(totalScore)`, save. |
| **Backend – automatic scoring logic** | Same file — private method `calculateScore(EvaluationAttempt attempt)`: loop over `attempt.getStudentAnswers()`, switch on `question.getQuestionType()`, apply MCQ / MSQ / FILL_BLANK rules, set `answer.setScoreAwarded(...)` and add to total. |
| **Backend – controller** | `backend/microservices/evaluation/.../controller/EvaluationAttemptController.java` — `POST /{attemptId}/finish` calls `finishAttempt`, and `POST /{attemptId}/finish-with-zero` calls `finishAttemptWithZero` (no calculation, just 0). |
| **Frontend – finishing** | `frontend/src/app/frontoffice/take-evaluation/take-evaluation.component.ts` — `finishAttempt()` calls `this.api.finishAttempt(this.attempt.id)` and then navigates to results. (Tab-leave uses `finishAttemptWithZero` as in section 2.) |
| **Frontend – API** | `frontend/src/app/core/services/evaluation-api.service.ts` — `finishAttempt(attemptId)` and `finishAttemptWithZero(attemptId)`. |

---

## Summary table

| Feature              | Main tech / idea                          | Frontend (main files)                                      | Backend (main files)                                                                 |
|----------------------|--------------------------------------------|------------------------------------------------------------|--------------------------------------------------------------------------------------|
| Fill-in-blanks D&D   | HTML5 Drag and Drop + Angular state         | `take-evaluation.component.html` + `take-evaluation.component.ts` | `FillBlankQuestion`, `Blank`; submit same as other types                            |
| Tab leave → 0        | Page Visibility API + finish-with-zero API | `take-evaluation.component.ts` + `evaluation-api.service.ts`       | `EvaluationAttemptController` + `EvaluationAttemptServiceImpl.finishAttemptWithZero` |
| Ollama questions     | Ollama HTTP API + PDF text extraction      | `evaluation-questions.component.ts/html` + `evaluation-api.service.ts` | `ReadingQuestionController`, `ReadingQuestionServiceImpl.generateFromPdf`, `AiQuestionGeneratorService`, `PdfTextExtractionService` |
| Auto correction      | Server-side scoring on finish              | Calls `finishAttempt` / `finishAttemptWithZero`            | `EvaluationAttemptServiceImpl.finishAttempt` + `calculateScore`                     |

All paths are relative to the project root (e.g. `PID-master` or your repo root). Backend Java packages are under `backend/microservices/evaluation/src/main/java/com/evaluation/evaluation/...`.
