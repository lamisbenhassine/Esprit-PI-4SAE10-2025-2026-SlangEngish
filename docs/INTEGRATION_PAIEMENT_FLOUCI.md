# Intégration du paiement Flouci – Étapes à suivre

Ce guide décrit comment connecter votre module inscription/checkout à l’API Flouci (agréée BCT, Tunisie).

---

## 1. Prérequis côté Flouci

1. **Créer un compte marchand**
   - Inscription : https://app.flouci.com
   - Téléphone, et pièce RNE (Registre National des Entreprises) si demandé.
   - Accès **sandbox** (test) immédiat.

2. **Récupérer les clés API**
   - Dans le portail Flouci : section **Développeurs** ou **API**.
   - Vous obtiendrez :
     - **PUBLIC_KEY** (ou APP_PUBLIC)
     - **SECRET_KEY** (ou APP_SECRET / PRIVATE_KEY)
   - En test, utilisez les clés du sandbox.

3. **Documentation officielle**
   - https://docs.flouci.com  
   - Génération de paiement : https://docs.flouci.com/api-reference/generate-transaction  
   - Vérification : https://docs.flouci.com/api-reference/verify-transaction  

---

## 2. Flux de paiement à mettre en place

```
[Frontend] Créer commande → [Backend] createOrderFromCart()
                ↓
[Frontend] Demander lien de paiement → [Backend] appelle Flouci generate_payment
                ↓
[Backend] Reçoit link + payment_id, les renvoie au frontend
                ↓
[Frontend] Redirige l’utilisateur vers link (page Flouci)
                ↓
[Utilisateur] Paie sur Flouci (wallet, carte, etc.)
                ↓
[Flouci] Appelle votre webhook + redirige vers success_link ou fail_link
                ↓
[Backend] Webhook : vérifier avec verify_payment, puis mettre à jour Payment + Order
[Frontend] Page success/fail affiche le résultat
```

---

## 3. Étapes techniques (backend – microservice inscription)

### 3.1 Configuration

- **Fichier** : `application.properties` (ou variables d’environnement)
- **Variables à ajouter** (ne pas commiter les clés en clair ; utiliser env ou un vault) :

```properties
# Flouci API (sandbox)
flouci.api.base-url=https://developers.flouci.com
flouci.api.public-key=VOTRE_PUBLIC_KEY
flouci.api.secret-key=VOTRE_SECRET_KEY

# URLs de retour (votre app)
flouci.success-link=http://localhost:4200/frontoffice/inscription/checkout/success
flouci.fail-link=http://localhost:4200/frontoffice/inscription/checkout/fail
flouci.webhook-url=https://VOTRE_DOMAINE_PUBLIC/api/inscription/payment/webhook/flouci
```

- En local, le webhook Flouci ne peut pas appeler `localhost`. Options :
  - Utiliser un tunnel (ngrok, etc.) et mettre cette URL dans `flouci.webhook-url`, ou
  - En dev : ne pas renseigner le webhook et mettre à jour le paiement uniquement quand l’utilisateur revient sur `success_link` (voir 3.5).

### 3.2 Dépendance HTTP (si pas déjà présent)

- Dans `pom.xml` du microservice inscription, avoir un client HTTP (Spring `RestTemplate` ou `WebClient`). Exemple avec `RestTemplate` :

```xml
<!-- Déjà fourni par Spring Boot si vous avez spring-boot-starter-web -->
```

### 3.3 Service Flouci (nouveau)

- **Classe** : `esprit.inscription.service.FlouciPaymentService` (ou `integration.FlouciApiService`).
- **Responsabilités** :
  - **Générer un paiement** : `POST https://developers.flouci.com/api/v2/generate_payment`
    - Headers : `Content-Type: application/json`, `Authorization: Bearer PUBLIC_KEY:SECRET_KEY`
    - Body : `amount` (millimes), `success_link`, `fail_link`, `webhook`, `developer_tracking_id` (ex. `orderId` ou `orderNumber`)
  - **Vérifier un paiement** : `GET https://developers.flouci.com/api/v2/verify_payment/{payment_id}`
    - Même `Authorization`
  - Gérer les réponses (lien de redirection, `payment_id`, statut SUCCESS/PENDING/FAILURE/EXPIRED).

- **Important** : les montants Flouci sont en **millimes** (1 TND = 1000 millimes). Si vos commandes sont en euros, définir une règle de conversion (ex. 1 EUR = 3,3 TND puis * 1000 en millimes) ou ne proposer Flouci que pour des prix en TND.

### 3.4 Endpoints à ajouter / modifier (PaymentController)

- **POST** `.../payment/initiate-flouci` (ou `/payment/flouci/create`)
  - Body : `orderId` (et éventuellement `orderNumber`).
  - Logique :
    - Vérifier que la commande existe et n’est pas déjà payée.
    - Calculer le montant en millimes.
    - Appeler `FlouciPaymentService.generatePayment(...)` avec `developer_tracking_id` = `orderId` (ou orderNumber).
    - Enregistrer en base un `Payment` avec `status = "pending"`, `transactionId = payment_id` Flouci, `method = "Flouci"`.
    - Retourner au frontend : `{ "paymentUrl": "https://flouci.com/pay/...", "paymentId": "..." }`.

- **GET** `.../payment/webhook/flouci` ou **POST** `.../payment/webhook/flouci`
  - Flouci peut appeler en GET ou POST selon leur doc ; adapter selon ce qu’ils envoient.
  - Logique :
    - Récupérer l’identifiant du paiement (query param ou body selon Flouci).
    - Appeler `FlouciPaymentService.verifyPayment(paymentId)`.
    - Si `success` et statut `SUCCESS` : mettre à jour `Payment` (status = `completed`) et éventuellement la commande (ex. `order.paid = true`).
    - Répondre 200 pour confirmer la réception à Flouci.

- **GET** `.../payment/verify/{orderId}` (optionnel mais utile)
  - Pour la page “success” : le frontend appelle cet endpoint avec `orderId` ; le backend appelle `verify_payment` pour le `payment_id` stocké, puis met à jour et renvoie le statut (paid / pending / failed). Utile si le webhook n’est pas encore reçu ou en dev sans webhook.

### 3.5 Persistance

- **Entité `Payment`** : vous avez déjà `orderId`, `amount`, `method`, `status`, `transactionId`.  
  - Pour Flouci : `method = "Flouci"`, `transactionId = payment_id` Flouci.
- Optionnel : champ `flouci_payment_id` si vous voulez distinguer l’id Flouci du `transactionId` métier.

---

## 4. Étapes techniques (frontend – Angular)

### 4.1 Après “Confirm Order”

- Après création de la commande (étape 2 du checkout), au lieu d’afficher directement le formulaire carte bancaire simulé :
  - Appeler un nouvel endpoint du backend, ex. : `POST /api/inscription/payment/initiate-flouci` avec `{ "orderId": orderId }`.
  - Recevoir `paymentUrl` (et éventuellement `paymentId`).

### 4.2 Redirection vers Flouci

- Ouvrir la page Flouci dans la fenêtre courante (ou nouvelle fenêtre) :
  - `window.location.href = paymentUrl;`
- L’utilisateur paie sur Flouci ; Flouci le redirige vers votre `success_link` ou `fail_link`.

### 4.3 Pages de retour

- **Route** : `/frontoffice/inscription/checkout/success` (et éventuellement `fail`).
- **Success** :
  - Afficher un message de succès.
  - Optionnel : appeler `GET /api/inscription/payment/verify/{orderId}` pour confirmer côté backend (et mettre à jour le statut si le webhook n’a pas encore été reçu).
  - Lien vers “Mes commandes” ou “Continuer”.
- **Fail** :
  - Message d’échec et proposition de réessayer ou de choisir un autre moyen de paiement.

### 4.4 Adapter le checkout actuel

- Dans `checkout.component.ts` :
  - Une fois la commande créée (step 2), proposer “Payer avec Flouci” qui appelle `initiate-flouci` puis redirige.
  - Vous pouvez garder “Carte bancaire” / “PayPal” comme simulation, ou les remplacer par “Flouci” uniquement pour la production.

---

## 5. Résumé des tâches à faire

| # | Où | Tâche |
|---|----|--------|
| 1 | Flouci | Créer compte, récupérer PUBLIC_KEY et SECRET_KEY (sandbox). |
| 2 | Backend | Ajouter `flouci.*` dans `application.properties` (ou env). |
| 3 | Backend | Créer `FlouciPaymentService` (generate + verify). |
| 4 | Backend | Ajouter `POST .../payment/initiate-flouci` et enregistrer un `Payment` pending. |
| 5 | Backend | Ajouter endpoint webhook Flouci + vérification + mise à jour Payment/Order. |
| 6 | Backend | (Optionnel) `GET .../payment/verify/{orderId}` pour la page success. |
| 7 | Frontend | Après création de commande, appeler initiate-flouci et rediriger vers `paymentUrl`. |
| 8 | Frontend | Créer routes/pages success et fail. |
| 9 | Prod | Remplacer les clés sandbox par les clés production et utiliser une URL publique pour le webhook. |

---

## 6. Exemple de corps pour Generate Payment (Flouci)

```json
{
  "amount": 159000,
  "success_link": "http://localhost:4200/frontoffice/inscription/checkout/success",
  "fail_link": "http://localhost:4200/frontoffice/inscription/checkout/fail",
  "webhook": "https://votre-domaine.com/api/inscription/payment/webhook/flouci",
  "developer_tracking_id": "123"
}
```

- `amount` : 159000 millimes = 159 TND.  
- `developer_tracking_id` : votre `orderId` (ou orderNumber) pour retrouver la commande dans le webhook / page success.

---

## 7. Sécurité

- Ne jamais exposer SECRET_KEY côté frontend : toute communication avec Flouci doit passer par votre backend.
- Stocker les clés dans des variables d’environnement ou un coffre (ex. Spring Cloud Config, Vault).
- En production, utiliser HTTPS pour success_link, fail_link et webhook.

Une fois ces étapes suivies, vous aurez un flux complet : création de commande → génération du lien Flouci → redirection → paiement → webhook + page success/fail.
