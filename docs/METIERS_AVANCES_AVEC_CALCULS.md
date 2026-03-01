# Métiers avancés avec calculs – Idées pour le module inscription

Ce document propose des **métiers avancés** qui reposent sur des **calculs, formules et règles métier** (et non sur la simple intégration d’API comme le paiement Stripe ou l’envoi d’emails). L’objectif est de montrer un vrai travail métier avec effort de modélisation et d’algorithmes, comme attendu par votre professeur.

---

## Référence : ce que font des plateformes similaires

### Duolingo
- **Modèle** : Freemium, plusieurs formules (mensuel, annuel, famille).
- **Calculs métier** : réduction annuelle (prix mensuel × 12 vs prix annuel), prix par utilisateur pour le plan famille (prix total / 6), comparaison “coût par mois” selon la durée.
- **Logique** : qui devrait s’abonner (recommandation selon temps d’étude, séries de jours, etc.) → scoring.

### Wall Street English
- **Modèle** : Tarification **personnalisée** selon niveau et objectifs.
- **Calculs métier** : prix calculé à partir du **niveau de départ**, du **niveau cible** et de la **durée** du parcours. Pas de grille fixe : formules (coefficients par niveau, par format cours, durée estimée pour atteindre l’objectif).
- **Logique** : “course length determined by your needs” → moteur de calcul de durée et de prix.

Source : [Wall Street English – Prices](https://www.wallstreetenglish.com/prices/), [Duolingo Pricing](https://support.duolingo.com/hc/en-us/articles/207362396-How-much-does-Duolingo-cost).

---

## Plan pour atteindre au minimum 3 métiers avancés

Votre professeur demande **au minimum 3 métiers avancés**. Vous avez **validé le 1er** (style Duolingo). Il reste à en choisir **2 parmi les 4 proposés ci‑dessous** pour atteindre (ou dépasser) le minimum.

---

### Métier 1 (validé) – Style Duolingo – **en cours d’implémentation**

**Contenu métier (calculs)** :
- **Réduction annuelle** : comparaison prix mensuel × 12 vs prix annuel → économie en % et en montant.
- **Plan famille** : prix total / nombre de places (ex. 6) → prix par utilisateur.
- **Coût par mois** : selon la durée du plan (1, 3, 6, 12 mois) → affichage « X TND/mois ».
- **Scoring « qui devrait s’abonner »** : recommandation selon temps d’étude, objectif, niveau (ex. « recommandé si vous étudiez 20+ min/jour »).

**À implémenter** : service(s) avec ces formules, endpoints (ex. quote annuel vs mensuel, coût par mois, score recommandation), et affichage côté catalogue / checkout.

---

### 3 autres métiers idéaux pour la gestion inscription

Pour les **2e et 3e métiers** (et au-delà), voici **3 choix idéaux** pour votre contexte, avec calculs clairs et visibles.

| # | Métier | Pourquoi idéal | Où sont les détails |
|---|--------|----------------|---------------------|
| **A** | **Prorata (changement d’abonnement)** | Formules simples (crédit = temps restant × prix/jour ; complément à payer). Très parlant pour un prof (upgrade/downgrade en cours de période). | Section **Métier 2** + explication « pour choisir » (prorata) |
| **B** | **Points de fidélité** | Chaque achat → points ; points → réduction. Paliers (silver/gold), plafond 30 % du panier. Calculs très explicites (points par euro, conversion). | Section **Métier 6** + explication « pour choisir » (points fidélité) |
| **C** | **Prix des bundles (packs)** | Prix du pack calculé à partir des plans (réduction max, marge min). Formules métier pures, pas d’API. | Section **Métier 5** + explication « pour choisir » (bundles) |

---

### Un 4e métier (au cas où vous en preniez un 3e)

| # | Métier | Pourquoi utile |
|---|--------|----------------|
| **D** | **Répartition du revenu (revenue recognition)** | Revenu d’un abonnement 12 mois réparti mois par mois ; revenu différé. Logique comptable, calculs dans le temps. Détails en **Métier 3**. |

---

### Ordre suggéré

1. **Métier 1 (Duolingo)** – en cours ✅  
2. Choisir **2 parmi A, B, C** (ou 1 parmi A,B,C + D) pour avoir **3 métiers au total** (ou 4 si vous en faites 1 + 3).

---

## Idées de métiers avancés (avec calculs explicites)

Chaque idée ci‑dessous contient : **objectif métier**, **entrées/sorties**, **formules ou règles**, et **effort attendu**. Tu peux en choisir une ou deux à implémenter pour montrer le “métier avancé”.

---

### 1. Moteur de tarification personnalisée (type Wall Street English)

**Objectif** : Ne pas afficher un seul prix fixe par plan, mais **calculer un prix (et une durée)** selon le niveau de l’utilisateur et son objectif.

**Entrées** :
- Niveau actuel (ex. A1, A2, B1, B2, C1, C2)  
- Niveau cible  
- Format (ex. online, full, premium)  
- Option : nombre de mois souhaité ou objectif de date

**Sorties** :
- Durée recommandée en mois (ou en jours)  
- Prix calculé  
- Éventuellement : décomposition (prix par mois, par niveau “parcouru”)

**Règles / formules (exemple)** :

- **Durée estimée** (mois) pour aller de `niveauDépart` à `niveauCible` :  
  - On associe à chaque niveau un “nombre de mois standard” (ex. A1=1, A2=2, B1=3, B2=4, C1=5, C2=6).  
  - `moisRecommandés = max(1, somme des mois pour chaque niveau entre départ et cible)`  
  - Exemple : A2 → B2 → 2+3+4 = 9 mois.

- **Prix de base par niveau** (ex. en TND) :  
  - `base(niveau) = 30 + (ordre du niveau × 15)` (ex. A1=30, B2=30+4×15=90).

- **Prix total** :  
  - `prixTotal = base(niveauCible) × moisRecommandés × coefficientFormat`  
  - `coefficientFormat` : online = 1.0, full = 1.2, premium = 1.5.

- **Prix par mois** (pour affichage) :  
  - `prixParMois = prixTotal / moisRecommandés`.

**Implémentation suggérée** :
- Service `TarificationPersonnaliseeService` avec méthodes du type `calculerDureeRecommandee(niveauDepart, niveauCible)`, `calculerPrix(niveauDepart, niveauCible, format)`.
- Endpoint(s) back : par exemple `GET /api/inscription/plans/quote?levelFrom=A2&levelTo=B2&format=full` qui renvoie `{ dureeMois, prixTotal, prixParMois }`.
- Côté front : sur la page offres ou après le test de niveau, afficher “Pour atteindre B2 à partir de A2, nous recommandons 9 mois – Prix total : X TND (Y TND/mois)”.

**Pourquoi c’est du métier avancé** : formules explicites, règles métier (niveaux, formats), pas une simple lecture de prix en base. Effort de modélisation + calcul.

---

### 2. Prorata et changement d’abonnement (upgrade / downgrade)

**Objectif** : Gérer le passage d’un plan à un autre **en cours de période** (upgrade ou downgrade) avec calcul de **crédit** (temps non consommé) et **complément à payer** ou remboursement.

**Entrées** :
- Abonnement actuel : plan A, date début, date fin, prix payé.  
- Nouveau plan B (prix pour la même durée type, ex. 30 jours).  
- Date du changement (aujourd’hui).

**Règles / formules** :

- **Jours restants** sur l’ancien plan :  
  - `joursRestants = dateFin - dateChangement` (en jours).

- **Crédit (valeur du temps non utilisé)** :  
  - `credits = (prixPayeA / nombreTotalJoursA) × joursRestants`  
  - On considère que la valeur est linéaire dans le temps.

- **Coût du nouveau plan** pour la même période restante (ou pour une période standard) :  
  - Option 1 : même durée restante.  
    - `prixProRataB = (prixMensuelB / 30) × joursRestants`.  
  - Option 2 : nouvelle période complète à partir du changement.  
    - `prixProRataB = prixMensuelB` (on démarre une nouvelle période).

- **Montant à payer (ou à rembourser)** :  
  - `aPayer = max(0, prixProRataB - credits)` (upgrade : souvent > 0).  
  - Si downgrade : `credits > prixProRataB` → remboursement ou crédit pour la prochaine facture : `remboursement = credits - prixProRataB`.

**Implémentation suggérée** :
- Entité ou DTO : `ChangementAbonnementRequest` (planIdActuel, planIdNouveau, dateChangement).  
- Service `ProrataService` : `calculerCredits(abonnement)`, `calculerMontantChangement(abonnement, nouveauPlan, dateChangement)` qui retourne `{ credits, prixProRataNouveauPlan, aPayer, remboursement }`.  
- Endpoint : `POST /api/inscription/subscriptions/calculate-upgrade` (ou `/calculate-change`) avec body + réponse en JSON.  
- Optionnel : appliquer le changement (créer nouvelle commande, ajuster dates, enregistrer le remboursement ou le complément).

**Pourquoi c’est du métier avancé** : calculs de prorata, gestion des deux sens (upgrade/downgrade), règles de crédit et de facturation. Pas une simple API externe.

---

### 3. Répartition du revenu (revenue recognition) – type comptabilité

**Objectif** : Pour un abonnement payé d’avance (ex. 12 mois), **répartir le revenu sur chaque mois** (reconnaissance du revenu) et suivre le “revenu différé” (deferred revenue).

**Entrées** :
- Commande : montant total payé, date de début d’abonnement, durée en mois (ex. 12).

**Règles / formules** :

- **Revenu mensuel reconnu** (constant sur la durée) :  
  - `revenuParMois = montantTotal / dureeMois`.

- Pour chaque mois `m` (de 1 à `dureeMois`) :  
  - `moisDebut = dateDebut.plusMonths(m-1)`  
  - Revenu reconnu pour le mois `m` = `revenuParMois`.

- **Revenu différé à une date donnée** :  
  - Nombre de mois déjà “consommés” depuis `dateDebut` jusqu’à la date du jour : `moisEcoules`.  
  - `revenuDejaReconnu = revenuParMois × min(moisEcoules, dureeMois)`  
  - `revenuDiffere = montantTotal - revenuDejaReconnu`.

**Implémentation suggérée** :
- Table ou entité `RevenueRecognition` : `orderId`, `mois` (période, ex. 2026-01), `montantReconnu`.  
- Service `RevenueRecognitionService` :  
  - `repartirRevenu(order)` : crée les lignes pour chaque mois.  
  - `getRevenuDiffere(orderId, aLaDate)` : retourne le montant encore différé.  
- Job planifié (ou appelé après paiement) : pour chaque commande “abonnement”, appeler `repartirRevenu`.  
- Endpoint reporting : `GET /api/inscription/stats/revenue-recognition?year=2026&month=1` → revenu reconnu sur la période.

**Pourquoi c’est du métier avancé** : logique comptable, formules de répartition dans le temps, notion de revenu différé. Calcul pur, pas API.

---

### 4. Score de recommandation d’offre (moteur de recommandation métier)

**Objectif** : Pour un utilisateur connecté, **classer les offres** selon un score métier (niveau, budget, objectif), pas un simple tri par prix.

**Entrées** :
- Niveau utilisateur (ex. A2).  
- Historique : montant déjà dépensé, plans achetés.  
- Objectif optionnel : “exam”, “business”, “general”.  
- Liste des plans disponibles (prix, niveau cible, type, durée).

**Règles / formules (exemple)** :

- **Score d’adéquation niveau** :  
  - Plan avec `niveauCible` = niveau user → 100.  
  - Un niveau au‑dessus → 80, deux au‑dessus → 60.  
  - En dessous → 30 (trop facile).  
  - `scoreNiveau(plan, userLevel)` = valeur entre 0 et 100.

- **Score “prix adapté”** (éviter d’afficher des plans trop chers si l’utilisateur n’a jamais dépensé beaucoup) :  
  - `moyenneHistorique = somme(ordre.total) / nombreCommandes` (ou 0 si pas d’historique).  
  - Si `plan.prix <= moyenneHistorique * 1.5` → 100, sinon décroissant (ex. 100 - (plan.prix - seuil)).  
  - `scorePrix(plan, historique)` = valeur 0–100.

- **Score durée / objectif** :  
  - Si objectif = “exam” et plan durée courte → bonus ; si “long terme” et plan 12 mois → bonus.  
  - `scoreObjectif(plan, objectif)` = 0–100.

- **Score global (pondéré)** :  
  - `score(plan) = 0.5 * scoreNiveau + 0.3 * scorePrix + 0.2 * scoreObjectif`.  
  - Trier les plans par `score` décroissant et retourner le top 5 ou 10.

**Implémentation suggérée** :
- Service `RecommandationOffreService` : `calculerScores(plans, userId)` ou `calculerScores(plans, niveau, historique, objectif)`.  
- Méthodes privées : `scoreNiveau`, `scorePrix`, `scoreObjectif`.  
- Endpoint : `GET /api/inscription/plans/recommended?userId=1` ou avec paramètres niveau/objectif.  
- Front : section “Recommandé pour vous” basée sur ce score.

**Pourquoi c’est du métier avancé** : formules de scoring, pondérations, utilisation de l’historique et du profil. Algorithme métier, pas une API tierce.

---

### 5. Économie des bundles (prix pack avec contraintes métier)

**Objectif** : Calculer le **prix d’un pack** (bundle) à partir des prix des plans inclus, en respectant des **règles métier** : réduction minimale, marge minimale, ou plafond de réduction.

**Entrées** :
- Liste des plans du bundle (ids ou objets avec prix).  
- Règles : réduction max autorisée (ex. 30 %), marge cible (ex. 20 %).

**Règles / formules** :

- **Prix total si achetés séparément** :  
  - `prixSomme = somme(plan.prix)`.

- **Prix plancher du bundle** (pour respecter la marge) :  
  - `prixMinimum = prixSomme × (1 - reductionMax)`  
  - Ex. réduction max 30 % → `prixMinimum = 0.7 × prixSomme`.

- **Prix cible (avec marge)** :  
  - Coût “logique” = `prixSomme × (1 - reductionVoulue)`.  
  - Pour une marge de 20 % sur ce coût : `prixCible = prixMinimum / (1 - marge)`  
  - Ou : `prixCible = prixSomme × (1 - reductionMax) / (1 - marge)`.

- **Réduction affichée** :  
  - `reductionPourcent = (1 - prixBundle / prixSomme) × 100`.  
  - Vérifier que `reductionPourcent <= reductionMax`.

**Implémentation suggérée** :
- Entité `Bundle` : id, nom, liste des planIds, `reductionMax`, `margeCible`.  
- Service `BundlePricingService` : `calculerPrixBundle(bundleId)` qui lit les plans, applique les formules, retourne `{ prixSomme, prixBundle, reductionPourcent, economie }`.  
- Endpoint : `GET /api/inscription/bundles/{id}/price`.  
- Création de commande : si l’utilisateur choisit le bundle, la commande enregistre le `prixBundle` calculé (et éventuellement les lignes détaillées).

**Pourquoi c’est du métier avancé** : contraintes (réduction max, marge), formules de prix dérivées. Logique métier pure.

---

### 6. Points de fidélité et réduction calculée

**Objectif** : Chaque achat donne des **points** ; les points sont **convertis en réduction** selon des règles (seuils, plafonds, paliers).

**Entrées** :
- Montant de la commande (après promo éventuelle).  
- Historique des points de l’utilisateur (total accumulé, déjà utilisés).  
- Règles : X points par euro dépensé, Y points = 1 € de réduction, plafond d’utilisation par commande.

**Règles / formules** :

- **Attribution** :  
  - `pointsGagnes = floor(montantCommande * pointsParEuro)`  
  - Ex. 10 points / 1 € → commande 49 € → 490 points.  
  - Paliers : si “silver” (total historique > 5000 points) : `pointsGagnes *= 1.05` ; si “gold” (> 15000) : `pointsGagnes *= 1.10`.

- **Redemption (utilisation des points)** :  
  - Règle : 100 points = 5 € de réduction.  
  - `reductionMaxAutorisee = min(plafondParCommande, soldePoints / 100 * 5)`.  
  - L’utilisateur choisit d’utiliser N points : `reduction = min((N/100)*5, reductionMaxAutorisee, montantCommande * 0.3)` (ex. plafond 30 % du panier).  
  - Nouveau total : `totalFinal = montantCommande - reduction`.  
  - Points déduits : `N` (ou le nombre correspondant à la réduction appliquée).

**Implémentation suggérée** :
- Entité `LoyaltyAccount` : userId, totalPoints, pointsUtilises.  
- Entité `LoyaltyTransaction` : type (EARNED / REDEEMED), orderId, points, date.  
- Service `LoyaltyService` : `calculerPointsGagnes(order)`, `calculerReductionPossible(userId, orderTotal)`, `appliquerRedemption(userId, orderId, pointsUtilises)`.  
- Endpoint : `GET /api/inscription/loyalty/balance/{userId}`, `POST /api/inscription/orders/apply-loyalty` (avec orderId et points à utiliser).  
- Au checkout : afficher “Vous avez X points (Y € de réduction max)” et appliquer la réduction au total.

**Pourquoi c’est du métier avancé** : calcul des points (avec paliers), conversion points → euros, plafonds. Pas une API externe, tout en formules.

---

## Synthèse pour votre professeur

| Métier avancé | Type de calcul | Effort |
|---------------|----------------|--------|
| 1. Tarification personnalisée | Durée et prix selon niveaux + format | Formules, moteur de calcul |
| 2. Prorata upgrade/downgrade | Crédit temps restant, complément à payer | Arithmétique + règles métier |
| 3. Répartition du revenu | Revenu par mois, revenu différé | Comptabilité, répartition dans le temps |
| 4. Score de recommandation | Scoring pondéré (niveau, prix, objectif) | Algorithme de recommandation |
| 5. Prix des bundles | Prix sous contraintes (réduction max, marge) | Formules de prix dérivées |
| 6. Points de fidélité | Points gagnés, conversion en réduction, paliers | Règles + calculs |

Tous ces métiers reposent sur des **calculs et des règles métier explicites** (formules, seuils, pondérations), et non sur la simple utilisation d’une API externe (paiement, mailing, etc.). Ils sont inspirés par des logiques réelles (Duolingo, Wall Street English) et peuvent être implémentés dans votre module inscription avec des services dédiés et des endpoints pour exposer les résultats des calculs.

---

## Ordre suggéré pour en réaliser un ou deux

1. **Tarification personnalisée** (1) ou **Score de recommandation** (4) : très parlant pour une plateforme d’apprentissage, formules claires.  
2. **Prorata** (2) ou **Points de fidélité** (6) : démontrent une vraie logique de gestion d’abonnement / fidélité avec calculs.

Vous pouvez présenter ce document à votre professeur pour valider le choix du (ou des) métier(s) avancé(s) à développer dans le projet.

---

## Métier 1 : déjà couvert dans votre projet

Vous avez déjà un **moteur de recommandation par niveau** : quand l’utilisateur choisit le niveau B1 dans le formulaire d’inscription, l’application affiche l’**offre conviviale** adaptée. Cela correspond en partie au métier 1 (tarification / recommandation selon le niveau). Vous pouvez donc considérer ce métier comme fait et choisir **un ou deux autres** parmi 2, 3, 4, 5 et 6 ci‑dessous.

---

## Explication des 5 autres métiers – pour choisir

Voici une explication **simplifiée** de chaque métier : en une phrase, avec un **exemple chiffré**, ce que **voit l’utilisateur** dans l’app, et ce que **vous codez**. Cela permet de prendre une décision en connaissance de cause.

---

### Métier 2 : Prorata (changement d’abonnement en cours de période)

**En une phrase**  
Quand un utilisateur veut **passer d’un plan à un autre** avant la fin de sa période (ex. de Starter à Premium), on calcule combien il a déjà « consommé », combien on lui **crédite** pour le temps restant, et combien il doit **payer en plus** (ou être remboursé) pour le nouveau plan.

**Exemple chiffré**  
- Il a payé **79 TND** pour 30 jours (Starter). Il change au jour **10**. Il reste 20 jours.  
- Crédit = (79 / 30) × 20 = **52,67 TND**.  
- Le nouveau plan (Premium) coûte 159 TND pour 30 jours. Pour les 20 jours restants : (159 / 30) × 20 = **106 TND**.  
- **À payer maintenant** = 106 − 52,67 = **53,33 TND** (il « garde » les 20 jours restants sur le nouveau plan).

**Ce que voit l’utilisateur**  
Une page « Changer d’offre » ou un bouton « Passer au Premium » qui affiche : « Crédit pour vos 20 jours restants : 52,67 TND. Complément à payer pour le Premium : 53,33 TND. »

**Ce que vous codez**  
- Un **service** qui calcule : jours restants, crédit, prix prorata du nouveau plan, montant à payer (ou remboursement).  
- Un **endpoint** du type « calculer le coût du changement » (entrée : abonnement actuel + nouveau plan ; sortie : crédit, à payer, détail).  
- Optionnel : appliquer le changement (créer une nouvelle commande, mettre à jour les dates).

**Pourquoi c’est du métier avancé**  
Calculs **arithmétiques** et **règles métier** (valorisation du temps restant, prorata, upgrade vs downgrade). Aucune API externe.

---

### Métier 3 : Répartition du revenu (revenue recognition)

**En une phrase**  
Quand un client paie **d’avance** pour 12 mois, on ne compte pas tout le revenu le jour du paiement : on le **répartit mois par mois** (chaque mois on « reconnaît » une part du revenu). On peut aussi afficher combien reste en **revenu différé** (pas encore reconnu).

**Exemple chiffré**  
- Commande : **360 TND** pour 12 mois (30 TND/mois).  
- Chaque mois on reconnaît : 360 / 12 = **30 TND**.  
- Après 3 mois : revenu déjà reconnu = 90 TND, **revenu différé** = 360 − 90 = **270 TND**.

**Ce que voit l’utilisateur**  
Souvent **rien** côté utilisateur ; c’est surtout pour le **backoffice** : un écran ou un rapport « Revenu reconnu en janvier 2026 : X TND », « Revenu différé total : Y TND ».

**Ce que vous codez**  
- Une **table** (ou entité) : pour chaque commande et chaque mois, une ligne « montant reconnu ce mois ».  
- Un **service** : à la création de la commande (ou au paiement), répartir le montant sur les N mois ; à une date donnée, calculer le revenu déjà reconnu et le revenu différé.  
- Un **endpoint** (ex. pour le dashboard admin) : « Revenu reconnu par mois » ou « Revenu différé par commande ».

**Pourquoi c’est du métier avancé**  
Logique **comptable** (répartition dans le temps, notion de revenu différé). Uniquement des **calculs**, pas d’API.

---

### Métier 4 : Score de recommandation d’offre

**En une phrase**  
Au lieu d’afficher les offres par prix ou par date, on les **classe** selon un **score** calculé pour chaque plan : adéquation avec le niveau de l’utilisateur, prix par rapport à son historique d’achats, objectif (examen, business, etc.). L’utilisateur voit en premier les offres « les plus pertinentes pour lui ».

**Exemple chiffré**  
- Utilisateur niveau B1, a déjà dépensé en moyenne 80 TND, objectif « exam ».  
- Plan A (B1, 79 TND, 30 j) → score niveau 100, score prix 95, score objectif 70 → **score global** = 0,5×100 + 0,3×95 + 0,2×70 ≈ **92**.  
- Plan B (C1, 159 TND, 90 j) → score niveau 60, score prix 40, score objectif 80 → score global ≈ **61**.  
- On affiche d’abord le plan A, puis le plan B.

**Ce que voit l’utilisateur**  
Une section « Recommandé pour vous » ou un tri « Par pertinence » sur la page des offres, avec les plans classés par score (sans afficher le score, juste l’ordre).

**Ce que vous codez**  
- Un **service** avec des formules : score niveau (plan vs user), score prix (plan vs historique), score objectif (plan vs objectif), puis score global pondéré.  
- Un **endpoint** du type « plans recommandés pour l’utilisateur X » (ou avec paramètres niveau / objectif).  
- Côté front : appeler cet endpoint et afficher les plans dans l’ordre retourné.

**Pourquoi c’est du métier avancé**  
**Algorithmes de scoring** (plusieurs critères, pondérations). Utilisation du **profil et de l’historique**. Pas une API externe.

**Différence avec votre « offre conviviale »**  
Ici le classement est **multi-critères** (niveau + prix + objectif) et peut utiliser l’**historique des commandes**. Votre offre conviviale actuelle est plutôt « un plan mis en avant selon le niveau ». Le métier 4 va plus loin (scoring explicite, tri de toute la liste).

---

### Métier 5 : Prix des bundles (packs)

**En une phrase**  
Un **bundle** = plusieurs plans vendus ensemble (ex. « Débutant + Vocabulaire »). Le prix du bundle n’est pas fixé à la main : il est **calculé** à partir des prix des plans, en respectant une **réduction max** (ex. 30 %) et une **marge min** (ex. 20 %). Ainsi le prix reste cohérent et rentable.

**Exemple chiffré**  
- Bundle = Plan A (79 TND) + Plan B (99 TND) → **prix si achetés séparément** = 178 TND.  
- Règle : réduction max 30 %, marge cible 20 %.  
- Prix plancher = 178 × 0,70 = **124,60 TND**.  
- Prix cible avec marge = 124,60 / 0,80 ≈ **155,75 TND** → on affiche par ex. **155 TND** pour le bundle.  
- Réduction affichée : (1 − 155/178) × 100 ≈ **13 %** (en dessous de 30 %, c’est ok).

**Ce que voit l’utilisateur**  
Sur la page offres : un **pack** « Débutant + Vocabulaire – 155 TND au lieu de 178 TND (−13 %) », avec le détail des plans inclus.

**Ce que vous codez**  
- Une **entité** Bundle (nom, liste des plans, réduction max, marge).  
- Un **service** qui calcule : somme des prix, prix plancher, prix cible, réduction en %.  
- Un **endpoint** « prix du bundle X » qui renvoie (prix total séparé, prix bundle, réduction %).  
- Côté panier/commande : possibilité d’ajouter un bundle (une ligne « Bundle Y » au prix calculé).

**Pourquoi c’est du métier avancé**  
**Formules de prix** sous **contraintes** (réduction max, marge). Logique métier pure, pas d’API.

---

### Métier 6 : Points de fidélité et réduction

**En une phrase**  
Chaque achat donne des **points** (ex. 10 points par euro). Les points peuvent être **échangés contre une réduction** (ex. 100 points = 5 €). Des **paliers** (silver, gold) donnent plus de points par achat. On impose un **plafond** (ex. max 30 % du panier en réduction points).

**Exemple chiffré**  
- Commande 49 TND → **490 points** (10 pts/€).  
- Utilisateur a 1200 points. Règle : 100 points = 5 €. Réduction max = min(1200/100×5, 30 % du panier) = min(60 €, 14,70 €) = **14,70 €**.  
- Il utilise 294 points (14,70 €) → nouveau total 49 − 14,70 = **34,30 TND**, il reste 906 points.

**Ce que voit l’utilisateur**  
- Sur le **checkout** : « Vous avez 1200 points (réduction max 14,70 €). Utiliser [champ] points. »  
- Après paiement : « Vous avez gagné 490 points. Total : 1690 points. »

**Ce que vous codez**  
- **Tables** : compte de points par utilisateur, historique des mouvements (gain / utilisation).  
- Un **service** : calcul des points gagnés (avec paliers silver/gold), calcul de la réduction possible (plafond 30 %), application de la réduction au total et déduction des points.  
- **Endpoints** : solde de points, « appliquer X points sur la commande ».  
- Au checkout : afficher le solde, permettre d’utiliser des points, recalculer le total.

**Pourquoi c’est du métier avancé**  
**Calculs** (points par euro, conversion points → euros, paliers, plafonds). Règles métier explicites, pas d’API.

---

## Tableau récapitulatif pour choisir

| Métier | En bref | Exemple type | Côté utilisateur visible | Difficulté |
|--------|---------|--------------|---------------------------|------------|
| **2. Prorata** | Crédit temps restant + complément à payer pour changer de plan | 79 TND sur 30 j, changement au jour 10 → crédit 52,67 TND | Page « Changer d’offre » avec montants calculés | Moyenne |
| **3. Revenu différé** | Répartir le revenu mois par mois, suivre le différé | 360 TND / 12 mois → 30 TND reconnus par mois | Surtout backoffice (rapports) | Moyenne |
| **4. Score recommandation** | Classer les offres par pertinence (niveau + prix + objectif) | Score 92 vs 61 → ordre d’affichage | Section « Recommandé pour vous » | Moyenne |
| **5. Bundles** | Prix du pack calculé (réduction max, marge) | 2 plans 79+99 → bundle 155 TND (−13 %) | Offre « Pack » avec prix et % | Moyenne |
| **6. Points fidélité** | Points par achat, conversion en réduction, paliers | 49 € → 490 pts ; 1200 pts → 14,70 € de réduction | Checkout « Utiliser X points » + solde | Moyenne à plus |

Vous pouvez en choisir **un** (le plus parlant pour vous ou pour votre prof) ou **deux** (ex. 2 + 6, ou 4 + 5) pour montrer plusieurs types de calculs métier.
