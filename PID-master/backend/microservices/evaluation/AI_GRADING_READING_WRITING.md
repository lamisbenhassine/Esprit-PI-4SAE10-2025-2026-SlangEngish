# Automatic grading for Reading and Writing (Ollama / AI)

Reading and Writing questions are now **automatically graded** using **Ollama** (local AI). You need to have Ollama installed and running with a model (e.g. `llama3.2`).

---

## 1. Prerequisites

- **Ollama** installed from [ollama.com](https://ollama.com)
- A model pulled, e.g.:
  ```bash
  ollama pull llama3.2
  ```
- Ollama service running (default: `http://localhost:11434`)

The evaluation microservice uses the same Ollama URL and model as for generating Reading questions from PDF (see `application.properties`: `ai.ollama.url`, `ai.ollama.model`).

---

## 2. When does automatic grading run?

When a student **finishes an attempt** (clicks "Finish & Submit"):

1. The backend loads all submitted answers for that attempt.
2. For each **Reading** and **Writing** question:
   - If the student wrote a non-empty text answer, the backend calls **Ollama** to grade it.
   - The AI is given: the question text, the student's answer, the maximum points for the question, and (for Reading) the question's instructions as context.
   - Ollama returns a **score from 0 to max points** (decimals allowed, e.g. 1.5).
3. That score is stored in `scoreAwarded` for the answer and included in the attempt's total score.
4. MCQ, MSQ, and Fill-in-the-blanks are still graded as before (no AI).

If Ollama is **not available** or the AI response cannot be parsed, the Reading/Writing answer is given a score of **0** and a warning is logged. The rest of the attempt is still saved.

---

## 3. Backend code

### 3.1 Service interface

**File:** `service/AiGradingService.java`

- Method: `gradeTextAnswer(questionText, studentAnswer, maxPoints, context)`.
- `context` can be the Reading question's instructions (or empty for Writing).

### 3.2 Service implementation

**File:** `service/impl/AiGradingServiceImpl.java`

- Calls Ollama's chat API with a **fair-grading prompt** that instructs the model to:
  - Award **full points** when the student's answer is correct or substantially correct (even if wording differs).
  - Give **high partial credit** for mostly correct answers or good understanding with minor gaps.
  - Give **some credit** for partially correct or incomplete but relevant answers.
  - Give **0 only** when the answer is wrong, irrelevant, or off-topic.
  - Prefer the higher score when in doubt.
- Truncates long question/answer text. Parses the score from the reply (supports formats like a plain number, "Score: 8", "10/10", etc.) and clamps it to [0, maxPoints].
- On any exception (e.g. Ollama not running), returns 0.0 and logs a warning.
- **Debug logging:** at DEBUG level, the raw AI response and parsed score are logged (enable `logging.level.com.evaluation.evaluation.service.impl.AiGradingServiceImpl=DEBUG` to troubleshoot grading).

### 3.3 Where it is used

**File:** `service/impl/EvaluationAttemptServiceImpl.java`

- In `calculateScore(attempt)`, in the switch on question type:
  - **READING** and **WRITING**: get text answer and question text; for Reading, load instructions as context; call `aiGradingService.gradeTextAnswer(...)`; set `answer.setScoreAwarded(aiScore)` and add to totalScore.
- After the loop, each answer is saved with `studentAnswerRepository.save(a)`.

---

## 4. Configuration

Same as for PDF question generation in `application.properties`:

- `ai.ollama.url=http://localhost:11434/v1/chat/completions`
- `ai.ollama.model=llama3.2`

---

## 5. Summary

| Question type | Grading method |
|---------------|----------------|
| MCQ, MSQ, FILL_BLANK | Existing automatic logic |
| **READING** | **Automatic via Ollama (AI)** |
| **WRITING** | **Automatic via Ollama (AI)** |

If Ollama is not running or the AI call fails, Reading/Writing answers get 0 and the attempt is still submitted.
