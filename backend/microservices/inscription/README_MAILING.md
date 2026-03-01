# 📧 Mailing Avancé - Métier Intégré dans Inscription

## 🎯 **Overview**

Le mailing est maintenant intégré comme **métier avancé** directement dans le microservice `inscription`, offrant des fonctionnalités d'emailing sophistiquées pour l'école anglaise.

## 🚀 **Fonctionnalités Principales**

### **📊 Gestion de Campagnes**
- **Création** de campagnes email complètes
- **Personnalisation** par niveau CEFR (A1-C2)
- **Automatisation** des workflows
- **Tracking** complet des performances

### **🎨 Templates Personnalisés**
- **Welcome** - Accueil nouveaux utilisateurs
- **Course Completion** - Félicitations et progression
- **Reactivation** - Campagnes de retour
- **Level Progression** - Passage au niveau supérieur
- **Trial Expiration** - Expiration d'essai
- **Promotional** - Offres spéciales

### **🤖 Automatisation Intelligente**
- **Welcome Series** - Onboarding 7 jours
- **Course Completion** - Notifications automatiques
- **Reactivation** - Détection inactivité 30 jours
- **Trial Expiration** - Alertes 3 jours avant
- **Level Progression** - Recommandations N+1

---

## 🏗️ **Architecture Intégrée**

### **Entités Principales**
```java
EmailCampaign      // Campagnes email
EmailTemplate      // Templates HTML
EmailTracking      // Tracking des emails
User              // Utilisateurs avec niveaux CEFR
```

### **Services Métier**
```java
EmailCampaignService     // Gestion campagnes
EmailTemplateService     // Templates et personnalisation
EmailService            // Envoi et tracking
MailingAutomationService // Automatisation intelligente
```

### **Contrôleurs REST**
```java
EmailCampaignController    // API campagnes
EmailTemplateController    // API templates  
EmailTrackingController    // API tracking
```

---

## 📧 **Templates par Défaut**

### **1. Welcome Template**
```html
<h1>Welcome to English Academy, {{firstName}}!</h1>
<p>Your {{englishLevel}} level journey begins here!</p>
```

### **2. Course Completion**
```html
<h1>Congratulations, {{firstName}}!</h1>
<p>You've completed your {{englishLevel}} course!</p>
```

### **3. Reactivation**
```html
<h1>We miss you, {{firstName}}!</h1>
<p>Here's what's new for {{englishLevel}} learners...</p>
```

### **4. Level Progression**
```html
<h1>Ready for {{nextLevel}}, {{firstName}}?</h1>
<p>Continue your success from {{englishLevel}}!</p>
```

### **5. Trial Expiration**
```html
<h1>Your trial expires in {{daysLeft}} days!</h1>
<p>Don't lose your {{englishLevel}} progress!</p>
```

---

## 🔄 **Workflows Automatisés**

### **👋 Welcome Workflow**
```java
@EventListener
public void handleUserRegistered(User user) {
    automationService.sendWelcomeCampaign(user);
}
```

### **🎓 Course Completion**
```java
@EventListener  
public void handleCourseCompleted(User user, String level) {
    automationService.sendCourseCompletionCampaign(user, level);
    // Suggérer niveau suivant
    String nextLevel = getNextLevel(level);
    automationService.sendLevelProgressionCampaign(user, nextLevel);
}
```

### **⏰ Trial Expiration**
```java
@Scheduled(cron = "0 0 */12 * * *")
public void checkTrialExpirations() {
    List<User> expiringUsers = userRepository.findUsersWithExpiringTrials(LocalDateTime.now().plusDays(3));
    expiringUsers.forEach(user -> {
        int daysLeft = user.getDaysUntilTrialExpiry();
        automationService.sendTrialExpirationCampaign(user, daysLeft);
    });
}
```

### **🔄 Reactivation**
```java
@Scheduled(cron = "0 0 9 * * *")
public void dailyReactivationCampaign() {
    automationService.sendReactivationCampaigns();
}
```

---

## 📊 **API Endpoints**

### **Campagnes**
```http
GET    /api/inscription/email-campaigns
POST   /api/inscription/email-campaigns
GET    /api/inscription/email-campaigns/{id}
PUT    /api/inscription/email-campaigns/{id}
DELETE /api/inscription/email-campaigns/{id}
POST   /api/inscription/email-campaigns/{id}/launch
POST   /api/inscription/email-campaigns/{id}/pause
POST   /api/inscription/email-campaigns/{id}/resume
```

### **Templates**
```http
GET    /api/inscription/email-templates
POST   /api/inscription/email-templates
GET    /api/inscription/email-templates/{id}
PUT    /api/inscription/email-templates/{id}
DELETE /api/inscription/email-templates/{id}
POST   /api/inscription/email-templates/create-defaults
```

### **Tracking**
```http
GET    /api/inscription/email-tracking/pixel/{emailId}
GET    /api/inscription/email-tracking/link/{emailId}
POST   /api/inscription/email-tracking/conversion/{emailId}
POST   /api/inscription/email-tracking/bounce/{emailId}
POST   /api/inscription/email-tracking/unsubscribe/{emailId}
```

---

## 🎯 **Segments Intelligents**

### **Par Niveau CEFR**
- **A1_LEARNERS** - Débutants complets
- **A2_LEARNERS** - Débutants avancés  
- **B1_LEARNERS** - Intermédiaires
- **B2_LEARNERS** - Intermédiaires avancés
- **C1_LEARNERS** - Avancés
- **C2_LEARNERS** - Experts

### **Comportementaux**
- **TRIAL_EXPIRING** - Essai expirant bientôt
- **HIGH_VALUE_USERS** - Revenu > 500€
- **INACTIVE_30_DAYS** - Inactifs 30 jours
- **COMPLETION_CANDIDATES** - 80%+ complétion

---

## 📈 **Analytics & KPIs**

### **Metrics de Campagne**
```java
double openRate = campaign.getOpenRate();      // Taux d'ouverture
double clickRate = campaign.getClickRate();    // Taux de clic
double conversionRate = campaign.getConversionRate(); // Taux de conversion
```

### **Tracking Utilisateur**
```java
// Tracking des emails envoyés
EmailTracking tracking = new EmailTracking();
tracking.setEmailId(UUID.randomUUID().toString());
tracking.setUserId(user.getId());
tracking.setCampaign(campaign);
```

---

## 🧪 **Tests**

### **Tests d'Intégration**
```bash
# Lancer les tests du mailing
mvn test -Dtest=MailingIntegrationTest

# Tests avec profil de test
mvn test -Dspring.profiles.active=test
```

### **Scénarios de Test**
- ✅ Création de campagne
- ✅ Personnalisation template
- ✅ Workflow d'automatisation
- ✅ Tracking des performances
- ✅ Segmentation par niveau

---

## 🚀 **Utilisation**

### **1. Créer Templates par Défaut**
```bash
curl -X POST http://localhost:8081/api/inscription/email-templates/create-defaults
```

### **2. Lancer Campaign Welcome**
```java
User newUser = userRepository.save(user);
automationService.handleUserRegistered(newUser);
```

### **3. Personnaliser Template**
```java
String personalized = templateService.personalizeContent(template, user);
```

### **4. Tracking Email**
```html
<!-- Dans le template HTML -->
<img src="http://localhost:8081/api/inscription/email-tracking/pixel/{{emailId}}" />
<a href="http://localhost:8081/api/inscription/email-tracking/link/{{emailId}}?url=https://example.com">
    Cliquez ici
</a>
```

---

## 🔧 **Configuration**

### **Application Properties**
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}

mailing:
  templates:
    base-path: classpath:templates/emails/
  tracking:
    base-url: http://localhost:8081
```

### **Scheduler**
```java
@EnableScheduling
@Configuration
public class SchedulerConfig {
    // Active les tâches automatisées
}
```

---

## 🎓 **Spécifiques École Anglaise**

### **Niveaux CEFR**
- **A1**: "I can introduce myself..."
- **A2**: "I can describe experiences..."
- **B1**: "I can understand main points..."
- **B2**: "I can interact with fluency..."
- **C1**: "I can express ideas fluently..."
- **C2**: "I can understand virtually everything..."

### **Progression Logique**
```java
private String getNextLevel(String currentLevel) {
    switch (currentLevel.toUpperCase()) {
        case "A1": return "A2";
        case "A2": return "B1";
        case "B1": return "B2";
        case "B2": return "C1";
        case "C1": return "C2";
        case "C2": return null; // Niveau max
        default: return "B1";
    }
}
```

### **Personnalisation Avancée**
```java
String personalizedContent = content
    .replace("{{firstName}}", user.getFirstName())
    .replace("{{englishLevel}}", user.getEnglishLevel())
    .replace("{{nextLevel}}", getNextLevel(user.getEnglishLevel()))
    .replace("{{completionRate}}", user.getCompletionRate() + "%");
```

---

## 📝 **Best Practices**

### **✅ Recommandations**
1. **Templates** - Utiliser variables `{{}}` pour personnalisation
2. **Tracking** - Inclure pixel de tracking dans tous les emails
3. **Automatisation** - Configurer les scheduled tasks
4. **Segments** - Créer segments par niveau CEFR
5. **Testing** - Tester avec profils différents

### **⚠️ Points d'Attention**
- Configuration SMTP correcte
- Templates HTML responsives
- Variables correctement échappées
- Rate limiting respecté
- GDPR compliance

---

## 🔄 **Intégration Existant**

### **Avec Incription**
- ✅ Utilise les entités `User` existantes
- ✅ S'intègre avec `SubscriptionPlan`
- ✅ Compatible avec `Order` et `Payment`
- ✅ Utilise `Cart` pour personnalisation

### **Avec Forum**
- ✅ Peut notifier les nouveaux posts
- ✅ Segmentation par niveau d'accès
- ✅ Réactivation utilisateurs inactifs

---

## 🎯 **Résultats Attendus**

### **📈 Métriques d'Engagement**
- **Open Rate**: 25-35% (welcome)
- **Click Rate**: 5-10% (progression)
- **Conversion Rate**: 2-5% (trial → paid)
- **Reactivation Rate**: 10-15% (inactive users)

### **🎓 Impact Pédagogique**
- **Rétention** améliorée de 20%
- **Progression** plus rapide
- **Engagement** augmenté
- **Satisfaction** utilisateur

---

**Votre système de mailing avancé est maintenant parfaitement intégré dans le microservice inscription !** 🎉

Prêt à tester avec vos utilisateurs d'école anglaise ! 🚀
