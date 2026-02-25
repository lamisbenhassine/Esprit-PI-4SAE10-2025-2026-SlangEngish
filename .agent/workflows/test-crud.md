---
description: How to verify the microservices architecture and CRUD functionality
---

### 1. Préparation du Backend
Assurez-vous que l'infrastructure microservices est opérationnelle :
1. Démarrez **Eureka Server** (Port 8761).
2. Démarrez l'**API Gateway** (Port 8098).
3. Démarrez les microservices **Forum** (Port 8040) et **Inscription** (Port 8041).
4. Vérifiez dans le dashboard Eureka (`http://localhost:8761`) que tous les services sont enregistrés (UP).
5. Assurez-vous que votre base de données **MySQL** est lancée.

### 2. Lancement du Frontend
1. Ouvrez un terminal dans le dossier `frontend`.
2. Exécutez la commande : `npm start` ou `ng serve`.
3. Accédez à l'application via `http://localhost:4200`.

### 3. Test des Fonctionnalités CRUD (Backoffice)
1. **Accès** : Connectez-vous et allez dans le **Backoffice**.
2. **Forum Management** :
   - Cliquez sur "Forum Management" dans le menu latéral.
   - Vous devriez voir la liste des sujets récupérés depuis le backend.
   - Testez la **Suppression** d'un sujet (vérifiez qu'il disparaît après confirmation).
3. **Subscription Management** :
   - Cliquez sur "Subscription Mgmt".
   - Vérifiez que les types d'abonnements s'affichent correctement.

### 4. Vérification Frontoffice (Template)
1. Retournez dans le **Frontoffice**.
2. Cliquez sur l'icône **Forum** :
   - Vérifiez que les sujets "Publics" créés ou restants s'affichent dans le "Forum Général".
3. Cliquez sur l'icône **Subscription** :
   - Vérifiez que les offres d'abonnement sont bien chargées dynamiquement depuis l'API.

### 5. Debugging (Si rien ne s'affiche)
- Ouvrez la console du navigateur (F12) -> Onglet **Network**.
- Rafraîchissez la page et regardez les appels vers `http://localhost:8098/api/...`.
- Si vous avez une erreur **404**, le Gateway ne trouve pas le service.
- Si vous avez une erreur **CORS**, vérifiez que `@CrossOrigin("*")` est présent sur vos contrôleurs Spring Boot.
