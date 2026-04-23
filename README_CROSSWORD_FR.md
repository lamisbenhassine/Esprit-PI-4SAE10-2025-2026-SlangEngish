## Smart Notebook - IA Coach, Crossword, Share (Documentation detaillee FR)

Ce README explique en detail les 3 fonctions demandees:

1. **IA Pronunciation Coach** (comment l'IA compare `targetText` et `heardText`)
2. **Crossword** (comment les mots se placent, surtout quand il y a des lettres communes)
3. **Share Notebook** (comment une note est partagee et importee)

Le but est de donner une lecture claire par couches: **model -> service -> controller -> frontend service -> composant UI**.

---

## 1) IA Coach (Pronunciation Coach)

### 1.1 Fichiers principaux

#### Backend
- `backend/microservices/notebook/src/main/java/esprit/notebook/controller/NotebookController.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/service/PronunciationCoachService.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/service/OllamaChatService.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/dto/PronunciationCoachRequest.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/dto/PronunciationCoachResponse.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/dto/PronunciationCoachItem.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/config/AppConfig.java`

#### Frontend
- `frontend/src/app/core/services/notebook-api.service.ts`
- `frontend/src/app/frontoffice/smart-notebook/smart-notebook.component.ts`
- `frontend/src/app/frontoffice/smart-notebook/smart-notebook.component.html`

### 1.2 Modele / DTO (ce qui circule)

- `PronunciationCoachRequest`
  - `targetText`: la phrase cible que l'etudiant doit prononcer
  - `heardText`: la transcription speech-to-text captee par le navigateur

- `PronunciationCoachResponse`
  - `overallSummary`: resume encourageant global
  - `idealSentence`: phrase ideale a pratiquer
  - `items[]`: corrections ciblees (`issue`, `correction`, `tip`)
  - `overallTips[]`: conseils generaux de rythme/clarte/prononciation
  - `rawCoachText`: fallback texte brut si le JSON IA n'est pas parse

### 1.3 Controller (API exposee)

Dans `NotebookController`:

- `GET /notebook/ai/pronunciation-coach`
  - endpoint de probe (verifier rapidement que la feature est bien deployee)

- `POST /notebook/ai/pronunciation-coach`
  - appelle `pronunciationCoachService.coach(targetText, heardText)`
  - retourne un `PronunciationCoachResponse`

### 1.4 Service detaille (comment l'IA travaille)

Dans `PronunciationCoachService`, fonction centrale:

- `coach(String targetText, String heardText)`

Etapes internes:

1. **Validation de l'entree**
   - si `heardText` est vide: retour immediat avec message "Say something first..."
2. **Limitation de taille**
   - `targetText` limite a 4000 chars
   - `heardText` limite a 8000 chars
3. **Construction du prompt**
   - via `buildPrompt(target, heard)`
   - prompt force un JSON strict en sortie
4. **Appel modele local**
   - `ollama.chat(prompt)` via `OllamaChatService`
5. **Parsing du JSON**
   - `parseResponse(rawText)` extrait `overallSummary`, `items`, `overallTips`, etc.
6. **Fallback**
   - si Ollama ne repond pas: message explicite
   - si parse JSON echoue: `rawCoachText` rempli

### 1.5 Nom du modele IA et configuration

Dans `OllamaChatService`:

- proprietes:
  - `ai.ollama.enabled` (default `true`)
  - `ai.ollama.url` (default `http://localhost:11434/v1/chat/completions`)
  - `ai.ollama.model` (default **`llama3.2`**)

Donc le nom du modele utilise par defaut est: **`llama3.2`**.

### 1.6 Frontend (pipeline coach)

Dans `smart-notebook.component.ts`:

- `toggleCoachMic(...)`
  - demarre/arrete speech recognition
  - au stop, lance automatiquement l'analyse

- `runCoachPipeline(...)`
  - lance en parallele:
    - `api.grammar(heard)`
    - `api.pronunciationCoach(target, heard)`
  - fusionne les resultats avec `forkJoin`
  - met a jour:
    - `coachGrammarResult`
    - `coachFeedback`

- `speakCoachScriptShort(...)`
  - lit une version vocale courte du retour coach (TTS navigateur)

Dans `notebook-api.service.ts`:

- `pronunciationCoach(targetText, heardText)`
  - POST sur `/ai/pronunciation-coach`

---

## 2) Crossword (jeu de mots croises)

### 2.1 Fichiers principaux

#### Backend
- `backend/microservices/notebook/src/main/java/esprit/notebook/model/Game.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/model/GameEntry.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/model/GameEntryProgress.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/dto/GameDtos.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/service/GameService.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/controller/NotebookController.java`

#### Frontend
- `frontend/src/app/backoffice/notebook-games/notebook-games.component.ts`
- `frontend/src/app/backoffice/notebook-games/notebook-games.component.html`
- `frontend/src/app/frontoffice/smart-notebook/smart-notebook.component.ts`
- `frontend/src/app/frontoffice/smart-notebook/smart-notebook.component.html`
- `frontend/src/app/core/services/notebook-api.service.ts`

### 2.2 Modeles (base de donnees)

- `Game`
  - metadata jeu: `title`, `description`, `teacherId`, `type`, `published`

- `GameEntry`
  - `clue`, `answerNorm`, `teacherHint`
  - position crossword:
    - `cwRow`, `cwCol` (depart)
    - `cwDir` (`ACROSS` ou `DOWN`)
    - `cwNumber` (numero affiche)

- `GameEntryProgress`
  - progression etudiant:
    - `attempts`, `hintLevel`, `solved`, `lastAnswer`

### 2.3 DTO retour frontend

Dans `GameDtos.GameDetail`:

- `gridRows`, `gridCols`
- `cellMaskRows`: masque plateau (`1` jouable, `0` noire)
- `entries[]` avec:
  - `row`, `col`, `dir`, `number`, `answerLength`, `clue`

Important: `answerNorm` reste backend, il n'est pas expose.

### 2.4 Creation de jeu (flow)

1. Teacher cree le jeu dans backoffice (`notebook-games.component.ts`)
2. Front appelle `api.createGame(...)`
3. API `POST /notebook/games` (`NotebookController.createGame`)
4. `GameService.create(...)`:
   - valide les entrees
   - normalise les reponses (`normalizeAnswer`)
   - genere la grille (`CrosswordLayout.generate`)
   - assigne numeros (`assignNumbers`)
   - sauvegarde
5. Front student charge details (`GET /notebook/games/{id}`)

### 2.5 Comment le placement fonctionne avec lettres communes

L'algorithme est dans `GameService.CrosswordLayout`.

#### Fonction cle
- `generate(entries, rows, cols)`

#### Regle principale

Pour un mot `w`, l'algo cherche d'abord des **lettres communes** avec celles deja posees.

Dans `findBestPlacement(...)`:

1. Parcours chaque cellule deja remplie `(r,c)` dans la grille
2. Pour chaque lettre `w[i]` du mot courant:
   - si `w[i] == grid[r][c]`, tentative de croisement
3. Tentative en `ACROSS`:
   - startRow = `r`
   - startCol = `c - i`
4. Tentative en `DOWN`:
   - startRow = `r - i`
   - startCol = `c`
5. Chaque tentative passe par `canPlace(...)`
6. Si plusieurs placements valides:
   - score calcule par `intersections(...)`
   - on garde le meilleur score (plus de lettres deja alignees)

#### Pourquoi c'est efficace

Le score favorise les positions avec le plus de croisements, donc la grille devient plus compacte et plus "crossword".

### 2.6 Validation stricte du placement

Dans `canPlace(...)`, le mot est accepte seulement si:

- il reste dans la grille
- il ne remplace jamais une lettre differente
- il ne colle pas un mot parallel sans vrai croisement
- les cellules juste avant et apres le mot sont vides

Cela evite les faux croisements et les collages invalides.

### 2.7 Si aucun croisement possible

Toujours dans `generate(...)`:

1. fallback en `ACROSS` (premiere place valide)
2. sinon fallback en `DOWN`

Donc l'algorithme est glouton (greedy): rapide et robuste, meme si pas mathematiquement optimal.

### 2.8 Comment le frontend gere les cases communes

Dans `smart-notebook.component.ts`:

- stockage unique des lettres: `cwCells` avec cle `row:col`
- calcul des coordonnees d'un mot: `cwEntryCoords(entryId)`
- lecture de reponse courante: `cwEntryAnswer(entryId)`

Quand deux mots partagent une case, ils lisent la **meme cle** `row:col`.
Donc si l'utilisateur tape la lettre dans cette case, la mise a jour est automatique pour les deux mots.

### 2.9 Verification d'une reponse

- Front construit la reponse depuis les cellules
- appelle `POST /notebook/games/{gameId}/entries/{entryId}/answer`
- backend `GameService.submitAnswer(...)` compare a `answerNorm`
- retourne: `correct`, `solved`, `attempts`, `hintLevel`

---

## 3) Share Notebook (partage de notes)

### 3.1 Fichiers principaux

#### Backend
- `backend/microservices/notebook/src/main/java/esprit/notebook/model/Note.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/dto/NoteDto.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/repository/NoteRepository.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/service/NotebookService.java`
- `backend/microservices/notebook/src/main/java/esprit/notebook/controller/NotebookController.java`

#### Frontend
- `frontend/src/app/core/services/notebook-api.service.ts`
- `frontend/src/app/frontoffice/smart-notebook/smart-notebook.component.ts`
- `frontend/src/app/frontoffice/smart-notebook/smart-notebook.component.html`

### 3.2 Modele `Note` (BDD)

Dans `Note.java`:

- champs de base: `userId`, `title`, `content`
- partage:
  - `shared` (visible dans l'espace partage)
  - `shareId` (identifiant public stable pour import)
  - `sharedAt` (date de publication)
- ranking:
  - `shareScore` (score qualite calcule)

### 3.3 Repository

Dans `NoteRepository`:

- `findByUserIdOrderByUpdatedAtDesc(userId)` -> notes privees user
- `findBySharedTrueOrderBySharedAtDescUpdatedAtDesc()` -> espace partage
- `findByShareId(shareId)` -> import d'une note partagee

### 3.4 Service detaille

Dans `NotebookService`, fonctions importantes:

- `share(userId, id)`
  - verifie proprietaire
  - met `shared = true`
  - remplit `sharedAt` si absent
  - regenere `shareId` si ancien enregistrement invalide

- `unshare(userId, id)`
  - retire la note de l'espace partage (`shared = false`)

- `listShared(viewerUserId)`
  - retourne toutes les notes partagees (actuellement visibles par tous)

- `importShared(viewerUserId, shareId)`
  - charge la note source partagee
  - cree une copie privee `(copy)` chez l'etudiant
  - la copie est `shared = false` par defaut

- `computeShareScore(content)`
  - heuristique simple:
    - nombre de mots
    - diversite vocabulaire
    - longueur de contenu
  - score plafonne a 1000

### 3.5 API (controller)

Dans `NotebookController`:

- `POST /notebook/notes/{id}/share`
- `DELETE /notebook/notes/{id}/share`
- `GET /notebook/shared-notes`
- `POST /notebook/shared-notes/{shareId}/import`

### 3.6 Frontend partage

Dans `notebook-api.service.ts`:

- `shareNote(...)`
- `unshareNote(...)`
- `listSharedNotes(...)`
- `importSharedNote(...)`

Dans `smart-notebook.component.ts`:

- `shareSelectedNote()`
- `unshareSelectedNote()`
- `loadSharedNotes()`
- `importSharedNote(note)`
- `sharedNotesView` (search + sort recent/score)

Dans `smart-notebook.component.html`:

- onglet `Shared` affiche:
  - liste notes partagees
  - preview
  - bouton "Add to my notebook"

---

## 4) Resume architecture (simple)

- **Model**: stocke les donnees persistantes (`Note`, `Game`, `GameEntry`, `GameEntryProgress`)
- **Service**: porte la logique metier (`NotebookService`, `GameService`, `PronunciationCoachService`)
- **Controller**: expose les endpoints REST (`NotebookController`)
- **Frontend API service**: encapsule les appels HTTP (`NotebookApiService`)
- **Composants Angular**: experience utilisateur (Smart Notebook + Backoffice games)

---

## 5) Endpoints utiles (memo rapide)

### IA Coach
- `GET /notebook/ai/pronunciation-coach`
- `POST /notebook/ai/pronunciation-coach`

### Crossword
- `POST /notebook/games`
- `GET /notebook/games`
- `GET /notebook/games/teacher?teacherId=...`
- `GET /notebook/games/{gameId}`
- `GET /notebook/games/{gameId}/progress?userId=...`
- `POST /notebook/games/{gameId}/entries/{entryId}/answer`
- `POST /notebook/games/{gameId}/entries/{entryId}/hint?userId=...`

### Share Notebook
- `POST /notebook/notes/{id}/share?userId=...`
- `DELETE /notebook/notes/{id}/share?userId=...`
- `GET /notebook/shared-notes?userId=...`
- `POST /notebook/shared-notes/{shareId}/import?userId=...`

---

## 6) Conclusion

La logique est bien separee:

- L'**IA Coach** fait une comparaison cible vs parole, structure le retour, et gere les fallback.
- Le **Crossword** place les mots avec priorite aux lettres communes via `findBestPlacement + canPlace + intersections`.
- Le **Share Notebook** utilise `shared/shareId` pour publier puis importer une copie privee.

Cette structure permet de maintenir facilement chaque module sans melanger les responsabilites.

