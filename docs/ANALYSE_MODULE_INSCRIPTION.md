# Analyse du module Inscription – Gestion des inscriptions

Document d’analyse du code du microservice **inscription** (backend + frontend) et des **idées métier avancées** à mettre en œuvre.

---

## 1. Architecture actuelle

### 1.1 Backend (microservice inscription – port 8030)

| Domaine | Entités | Services principaux | APIs exposées |
|--------|---------|---------------------|----------------|
| **Offres / Plans** | `SubscriptionPlan` | `SubscriptionPlanService` | CRUD plans, GET par user/type |
| **Panier** | `Cart`, `CartItem` | `CartService` | GET/POST panier, add/remove item, clear |
| **Commandes** | `Order`, `OrderItem` | `OrderService` | create from cart, GET by user/id/number, DELETE |
| **Paiement** | `Payment` | `PaymentService` | process, verify user, update status |
| **Code promo** | `PromoCode` | `PromoCodeService` | validate, create, CRUD |
| **Statistiques** | – | `StatisticsService`, `RealTimeStatsService` | dashboard stats, WebSocket push |
| **Email / Marketing** | `EmailCampaign`, `EmailTemplate`, `EmailTracking` | `EmailService`, `EmailTemplateService`, `EmailCampaignService`, `MailingAutomationService` | campagnes, templates, tracking, welcome/course completion |
| **Utilisateurs** | `User` (local cache/sync) | `UserService` | CRUD users, sync avec user-service si besoin |

**Flux métier principaux :**

1. **Catalogue** → offres (plans) créées en backoffice, listées en frontoffice (offers-list, offer-detail).
2. **Panier** → ajout d’offres (add to cart), suppression, passage au checkout avec code promo (panier + checkout).
3. **Commande** → création à partir du panier (avec optionnel code promo), puis paiement simulé (process payment).
4. **Post-paiement** → stats temps réel (WebSocket), notifications admin.

### 1.2 Frontend

- **Frontoffice** : placement-form, offers-list, offer-detail, **cart**, **checkout** (code promo, choix moyen de paiement, étapes 1–2–3).
- **Backoffice** : subscription-management (CRUD offres), dashboard (stats), autres modules (users, forum, etc.).
- **Services** : `SubscriptionPlanService`, `CartService`, `OrderService`, `PaymentService`, `PromoService`, `StatsService`, `WebSocket`.

---

## 2. Ce qui existe déjà (résumé)

- CRUD offres (plans) + affichage catalogue + panier + checkout.
- Code promo (validation, application au total, création en base, usage limité).
- Commande créée depuis le panier (avec réduction promo), paiement simulé.
- Stats dashboard (revenus, commandes, paiements) + push WebSocket.
- Structure email : campagnes, templates, tracking, automatisations (welcome, course completion, level progression).
- Gestion utilisateurs locale (User, UserController) et clients vers d’autres microservices (UserServiceClient, ForumServiceClient).

---

## 3. Manques ou points à renforcer

| Zone | Constat | Piste |
|------|---------|--------|
| **Paiement réel** | Paiement simulé uniquement | Intégration Flouci (voir `INTEGRATION_PAIEMENT_FLOUCI.md`) |
| **Facturation** | Aucune facture / reçu PDF | Générer PDF après paiement, stockage + envoi email |
| **Lien commande ↔ accès** | Pas de notion "abonnement actif" par user/plan | Entité Subscription (user, plan, date début/fin, statut) |
| **Statistiques** | `planStats` en dur (mock) dans `StatisticsService` | Calculer par plan à partir des `Order`/`OrderItem` réels |
| **Emails post-achat** | Campagnes existantes mais pas déclenchées automatiquement après paiement | Déclencher email confirmation commande après `PaymentService.processPayment` |
| **Admin promo** | Pas d’écran backoffice pour gérer les codes promo | Page CRUD codes promo (liste, créer, modifier, désactiver, voir utilisations) |
| **Historique utilisateur** | Pas de page "Mes commandes" / "Mes abonnements" en frontoffice | Page listant les commandes de l’utilisateur + statut paiement |

---

## 4. Idées métier avancées pour aller plus loin

Au-delà du document **FONCTIONNALITES_AVANCEES_PROPOSITIONS.md**, voici des **idées métier avancées** concrètes, orientées "gestion inscription" et implémentables par étapes.

### 4.1 Abonnement et accès (métier cœur)

| Idée | Description courte | Bénéfice métier |
|------|--------------------|------------------|
| **Entité Subscription (abonnement actif)** | Après paiement réussi : créer une `Subscription` (userId, planId, dateDébut, dateFin, statut ACTIVE/EXPIRED/CANCELLED). Servir un endpoint `GET /api/inscription/subscriptions/me` pour afficher "Mes abonnements" et débloquer l’accès aux contenus par niveau/plan. | Savoir qui a accès à quoi et jusqu’à quand. |
| **Renouvellement et expiration** | Job (scheduler) qui détecte les abonnements qui expirent dans X jours, envoie un email de rappel, et met à jour le statut en EXPIRED à la date de fin. Option "renouveler" qui recrée une commande ou un lien de paiement. | Rétention et revenus récurrents. |
| **Niveau / plan recommandé** | À l’inscription (placement), enregistrer le niveau (A1, B2, etc.) et l’utiliser pour filtrer ou mettre en avant les plans "recommandés" sur la page offres. | Meilleure conversion. |

### 4.2 Ventes et promotion

| Idée | Description courte | Bénéfice métier |
|------|--------------------|------------------|
| **Bundles (packs)** | Entité `Bundle` (nom, liste de planIds, prix bundle). Au checkout, possibilité d’ajouter un bundle au panier comme un seul "item" avec prix réduit. | Augmentation du panier moyen. |
| **Offres limitées dans le temps** | Champs `validFrom` / `validUntil` sur `SubscriptionPlan` (ou table dédiée "promo plan"). Le catalogue n’affiche que les offres valides ; le backoffice permet de planifier des flash sales. | Urgence d’achat, campagnes ciblées. |
| **Parrainage** | Table `Referral` (parrainId, filleulId, code, statut). À l’inscription du filleul avec un code parrain : réduction (ex. -10 %) sur première commande ; au premier paiement du filleul, crédit ou réduction pour le parrain. | Croissance organique. |

### 4.3 Paiement et facturation

| Idée | Description courte | Bénéfice métier |
|------|--------------------|------------------|
| **Flouci** | Intégration complète (generate payment → redirect → webhook → vérification). | Paiement réel en Tunisie. |
| **Facture / reçu PDF** | Après paiement : génération d’un PDF (numéro de facture, lignes de commande, TVA si besoin), stockage (URL ou blob), envoi par email + lien de téléchargement dans "Mes commandes". | Conformité et satisfaction. |
| **Remboursement / annulation** | Statut sur `Order` ou `Payment` (REFUND_REQUESTED, REFUNDED). Workflow : utilisateur demande → admin valide → mise à jour statut + éventuellement annulation de la `Subscription`. | Confiance et gestion des litiges. |

### 4.4 Communication et rétention

| Idée | Description courte | Bénéfice métier |
|------|--------------------|------------------|
| **Email de confirmation de commande** | Dès que `Order` est créée et/ou `Payment` est "completed", appeler `EmailService` ou `MailingAutomationService` pour envoyer un email type "Votre commande #XXX a été enregistrée" avec récap (montant, plan, prochaines étapes). | Clarté et professionnalisme. |
| **Rappel panier abandonné** | Job qui liste les paniers avec des items non commandés depuis plus de 24h (ou 48h), envoie un email "Vous avez laissé des offres dans votre panier" avec lien vers le panier. | Récupération de ventes. |
| **Notification in-app post-paiement** | Après paiement réussi, envoyer une notification temps réel (WebSocket) à l’utilisateur : "Paiement validé – accédez à vos cours". | Expérience immédiate. |

### 4.5 Admin et pilotage

| Idée | Description courte | Bénéfice métier |
|------|--------------------|------------------|
| **CRUD codes promo en backoffice** | Page dédiée : liste des codes, créer / éditer (valeurs, dates, max uses), activer/désactiver, voir le nombre d’utilisations. | Maîtrise des campagnes promo. |
| **Statistiques par plan réelles** | Remplacer le mock dans `StatisticsService.getDashboardStats()` par des requêtes (OrderItem + Order) pour compter les ventes et le CA par type de plan ; exposer dans `DashboardStatsDTO.planStats` et graphiques. | Pilotage des ventes par offre. |
| **Export commandes (CSV/Excel)** | Endpoint ou bouton backoffice : export des commandes (période, filtres) avec colonnes (date, user, order number, total, promo, statut paiement). | Compta et reporting. |
| **Audit des changements** | Tables d’historique (ex. OrderHistory, PlanHistory) ou champs `updatedAt`/`updatedBy` sur les entités sensibles pour tracer les modifications (qui a modifié une offre, annulé une commande, etc.). | Traçabilité et conformité. |

### 4.6 Technique et UX

| Idée | Description courte | Bénéfice métier |
|------|--------------------|------------------|
| **Page "Mes commandes"** | Frontoffice : page listant les commandes de l’utilisateur connecté (numéro, date, montant, statut, détail des articles). Lien vers facture si disponible. | Autonomie et transparence. |
| **Page "Mes abonnements"** | Si entité Subscription : afficher les abonnements actifs et passés, date de fin, bouton "Renouveler" ou "Voir les offres". | Clarté de l’accès aux contenus. |
| **Multi-devises** | Champ préférence utilisateur ou détection locale (TND / EUR). Affichage des prix et totaux dans la devise choisie (avec taux de change configurable ou fixe). | Ouverture internationale. |

---

## 5. Ordre de mise en œuvre suggéré (métier avancé)

1. **Abonnement actif (Subscription)** + page "Mes abonnements" + déclencher création après paiement.
2. **Email de confirmation de commande** après paiement réussi.
3. **Statistiques par plan réelles** (remplacer le mock).
4. **Admin codes promo** (CRUD en backoffice).
5. **Facture PDF** après paiement.
6. **Flouci** (quand tu reviendras sur le paiement).
7. **Parrainage** ou **Bundles** selon priorité produit.
8. **Rappel panier abandonné** + **Remboursement** si besoin.

Tu peux utiliser ce document et **FONCTIONNALITES_AVANCEES_PROPOSITIONS.md** pour prioriser les prochains sprints ou sujets de PFE/métier.
