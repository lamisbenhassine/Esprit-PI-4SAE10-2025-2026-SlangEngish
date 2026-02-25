# Fonctionnalités avancées – Propositions

Idées de fonctionnalités à ajouter au module inscription / Slang-English, au-delà du **code promo** déjà implémenté.

---

## Déjà implémenté

- **Code promo** : application d’une réduction (pourcentage ou montant fixe) au checkout, avec dates de validité, montant minimum, nombre d’utilisations max.

### Créer un code promo de test

**Via l’API (POST)**  
`POST http://localhost:8030/api/inscription/promo` avec un body JSON, par exemple :

- **-10 %** (pourcentage) :
```json
{
  "code": "WELCOME10",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "minPurchaseAmount": 20,
  "maxUses": 100,
  "active": true
}
```

- **-5 €** (montant fixe) :
```json
{
  "code": "OFFRE5",
  "discountType": "FIXED_AMOUNT",
  "discountValue": 5,
  "minPurchaseAmount": 30,
  "maxUses": 50,
  "active": true
}
```

Optionnel : `validFrom` / `validUntil` au format ISO (ex. `"2025-03-01T00:00:00"`).  
Ensuite, au checkout, saisir le code (ex. `WELCOME10`) et cliquer sur « Appliquer ».

---

## 1. Parcours utilisateur & fidélité

| Fonctionnalité | Description | Priorité |
|----------------|-------------|----------|
| **Parrainage** | L’utilisateur parrain envoie un lien ; le filleul a -10 % sur sa première commande, le parrain reçoit un avantage (réduction ou crédit). | Élevée |
| **Points de fidélité** | Chaque achat donne des points ; échange contre réduction ou contenu (ex. 100 pts = -5 €). | Moyenne |
| **Abonnement récurrent** | Renouvellement automatique (mensuel/annuel) avec rappel avant échéance et possibilité d’annuler. | Élevée (si modèle abo) |
| **Niveaux / badges** | Statuts (Bronze, Silver, Gold) selon dépenses ou activité ; avantages associés (codes promo dédiés, support prioritaire). | Basse |

---

## 2. Offres & catalogue

| Fonctionnalité | Description | Priorité |
|----------------|-------------|----------|
| **Offres groupées (bundles)** | Packs (ex. « Débutant + Vocabulaire ») à un prix bundle inférieur à la somme des prix. | Élevée |
| **Offres limitées dans le temps** | Flash sales (ex. -30 % pendant 48 h) avec date de début/fin côté back-office. | Moyenne |
| **Liste de souhaits** | L’utilisateur met des plans en « favoris » et reçoit une alerte en cas de promo. | Moyenne |
| **Comparateur de plans** | Page qui affiche 2–3 plans côte à côte (prix, contenu, niveau) pour aider au choix. | Basse |

---

## 3. Paiement & facturation

| Fonctionnalité | Description | Priorité |
|----------------|-------------|----------|
| **Intégration Flouci** | Paiement en ligne (Tunisie) via l’API Flouci (voir `INTEGRATION_PAIEMENT_FLOUCI.md`). | Élevée |
| **Facturation / reçus** | Génération de PDF (reçu ou facture) après paiement, avec téléchargement et envoi par email. | Élevée |
| **Paiement en plusieurs fois** | Proposition « 2x sans frais » ou 3x (si partenaire le permet). | Moyenne |
| **Remboursement partiel / annulation** | Workflow : demande de remboursement → validation admin → mise à jour statut commande / paiement. | Moyenne |

---

## 4. Notifications & communication

| Fonctionnalité | Description | Priorité |
|----------------|-------------|----------|
| **Emails transactionnels** | Confirmation de commande, échec de paiement, rappel panier abandonné, renouvellement abonnement. | Élevée |
| **Notifications in-app** | Notifications déjà partiellement en place ; étendre (nouvelle offre, code promo personnel, alerte expiration abo). | Moyenne |
| **SMS / WhatsApp** | Rappel de paiement ou code de validation (selon infrastructure et coûts). | Basse |

---

## 5. Admin & analytics

| Fonctionnalité | Description | Priorité |
|----------------|-------------|----------|
| **Gestion des codes promo (CRUD)** | Interface admin pour créer / modifier / désactiver des codes, voir le nombre d’utilisations. | Élevée |
| **Tableau de bord ventes** | CA par période, par plan, taux d’utilisation des codes promo, panier moyen. | Élevée |
| **Export des commandes** | Export CSV/Excel (commandes, montants, codes promo utilisés) pour compta ou reporting. | Moyenne |
| **A/B test sur offres** | Variantes de libellé ou de prix pour mesurer impact sur conversions. | Basse |

---

## 6. UX & technique

| Fonctionnalité | Description | Priorité |
|----------------|-------------|----------|
| **Panier sauvegardé** | Persistance du panier (déjà partiellement fait) ; reprise après reconnexion ou changement d’appareil. | Moyenne |
| **Checkout en une page** | Résumer panier + code promo + moyen de paiement sur une seule page pour réduire les abandons. | Moyenne |
| **Multi-devises** | Afficher les prix en TND et EUR (ou autre) selon préférence ou localisation. | Basse |
| **Accessibilité (a11y)** | Labels, contraste, navigation clavier, annonces screen reader sur formulaire et messages d’erreur. | Moyenne |

---

## Ordre de mise en œuvre suggéré

1. **Code promo** – fait.  
2. **Intégration Flouci** – quand vous reviendrez sur le paiement.  
3. **Facturation / reçus PDF** – forte valeur perçue.  
4. **Gestion admin des codes promo** – pour exploiter la fonctionnalité au quotidien.  
5. **Emails transactionnels** – confirmation de commande en premier.  
6. **Parrainage** ou **bundles** – selon la stratégie produit.

Vous pouvez utiliser cette liste pour prioriser les prochains sprints ou pour proposer des sujets de projet / PFE.
