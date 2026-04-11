-- =============================================================================
-- Démo : plusieurs étudiants, tuteurs, posts textuels (fil), discussions privées
-- MySQL 8+ — exécuter dans l’ordre (userdb puis forumdb).
-- Adapter les noms de bases si besoin (userdb / forumdb).
-- =============================================================================
-- AVANT : arrêter les microservices ou accepter les erreurs de contrainte
--         si des lignes existent déjà. Les INSERT utilisent ON DUPLICATE KEY
--         sur la clé primaire `id` pour les utilisateurs.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1) BASE USER — microservice user (port 8010)
-- ---------------------------------------------------------------------------
USE userdb;

-- 16 apprenants (id 1–10 + 13–18) + 4 tuteurs (id 11–12 + 19–20)
-- Mot de passe en clair "pass" (à des fins démo uniquement — ne pas utiliser en prod)
INSERT INTO users (id, first_name, last_name, email, password, account_role, english_level, subscription_status) VALUES
(1,  'Alex',      'Benali',     'alex.demo@test.local',       'pass', 'STUDENT', 'B2', 'ACTIVE'),
(2,  'Sara',      'Mansour',    'sara.demo@test.local',       'pass', 'STUDENT', 'A2', 'ACTIVE'),
(3,  'Youssef',   'Khelil',     'youssef.demo@test.local',    'pass', 'STUDENT', 'B1', 'ACTIVE'),
(4,  'Inès',      'Trabelsi',   'ines.demo@test.local',       'pass', 'STUDENT', 'C1', 'ACTIVE'),
(5,  'Mehdi',     'Gharbi',     'mehdi.demo@test.local',      'pass', 'STUDENT', 'A1', 'ACTIVE'),
(6,  'Leïla',     'Hamdi',      'leila.demo@test.local',      'pass', 'STUDENT', 'B2', 'ACTIVE'),
(7,  'Karim',     'Saidi',      'karim.demo@test.local',      'pass', 'STUDENT', 'B1', 'ACTIVE'),
(8,  'Nour',      'Jlassi',     'nour.demo@test.local',       'pass', 'STUDENT', 'A2', 'ACTIVE'),
(9,  'Amine',     'Frikha',     'amine.demo@test.local',      'pass', 'STUDENT', 'C2', 'ACTIVE'),
(10, 'Salma',     'Bouaziz',    'salma.demo@test.local',      'pass', 'STUDENT', 'B2', 'ACTIVE'),
(11, 'Marie',     'Dupont',     'marie.tutor@test.local',     'pass', 'TUTOR',   'C2', 'ACTIVE'),
(12, 'James',     'Wilson',     'james.tutor@test.local',     'pass', 'TUTOR',   'C1', 'ACTIVE'),
(13, 'Issra',     'Belhabib',   'issra.demo@test.local',      'pass', 'STUDENT', 'B1', 'ACTIVE'),
(14, 'Mouna',     'Ayadi',      'mouna.demo@test.local',      'pass', 'STUDENT', 'A2', 'ACTIVE'),
(15, 'Rim',       'Ben Amor',   'rim.demo@test.local',        'pass', 'STUDENT', 'B2', 'ACTIVE'),
(16, 'Walid',     'Cherif',     'walid.demo@test.local',      'pass', 'STUDENT', 'C1', 'ACTIVE'),
(17, 'Sami',      'Karray',     'sami.demo@test.local',       'pass', 'STUDENT', 'A1', 'ACTIVE'),
(18, 'Aya',       'Mahjoub',    'aya.demo@test.local',        'pass', 'STUDENT', 'C2', 'ACTIVE'),
(19, 'Nadia',     'Ben Salem',  'nadia.tutor@test.local',     'pass', 'TUTOR',   'C2', 'ACTIVE'),
(20, 'Omar',      'Haddad',     'omar.tutor@test.local',      'pass', 'TUTOR',   'C1', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  first_name = VALUES(first_name),
  last_name = VALUES(last_name),
  email = VALUES(email),
  password = VALUES(password),
  account_role = VALUES(account_role),
  english_level = VALUES(english_level),
  subscription_status = VALUES(subscription_status);

-- ---------------------------------------------------------------------------
-- 2) BASE FORUM — microservice forum (port 8040)
-- ---------------------------------------------------------------------------
USE forumdb;

-- Référence espace forum général (créé au démarrage par ForumSpaceInitializer)
SET @space_general := (SELECT id FROM forum_space WHERE type = 'GENERAL' AND space_key = 'GENERAL' LIMIT 1);

-- Posts textuels uniquement (fil) — une ligne par INSERT pour compatibilité MySQL
INSERT INTO forum_topic (
  title, description, author_id, space_id, category, is_public,
  cover_image_url, cover_video_url, pinned, locked, views, created_at, updated_at
)
SELECT 'Premiers pas en anglais conversationnel',
       'Salut tout le monde ! Je partage ici des astuces pour oser parler : penser en phrases courtes, ne pas chercher la perfection, et écouter des podcasts 10 minutes par jour. Qui veut s’entraîner avec moi ?',
       1, @space_general, 'GENERAL', 1, NULL, NULL, 0, 0, 12, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY
FROM DUAL
WHERE @space_general IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM forum_topic WHERE title = 'Premiers pas en anglais conversationnel');

INSERT INTO forum_topic (
  title, description, author_id, space_id, category, is_public,
  cover_image_url, cover_video_url, pinned, locked, views, created_at, updated_at
)
SELECT 'Expression du jour : "It rings a bell"',
       'Traduction : « ça me dit quelque chose ». Exemple : That name rings a bell — I think we met in Tunis. À utiliser à l’oral pour sonner naturel.',
       2, @space_general, 'GENERAL', 1, NULL, NULL, 0, 0, 8, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY
FROM DUAL
WHERE @space_general IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM forum_topic WHERE title = 'Expression du jour : "It rings a bell"');

INSERT INTO forum_topic (
  title, description, author_id, space_id, category, is_public,
  cover_image_url, cover_video_url, pinned, locked, views, created_at, updated_at
)
SELECT 'Groupe de révision — niveau B1',
       'On se fixe un créneau pour échanger en anglais sur le thème du travail et des projets. Répondez sous ce post avec vos disponibilités.',
       3, @space_general, 'GENERAL', 1, NULL, NULL, 0, 0, 15, NOW() - INTERVAL 12 HOUR, NOW() - INTERVAL 12 HOUR
FROM DUAL
WHERE @space_general IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM forum_topic WHERE title = 'Groupe de révision — niveau B1');

INSERT INTO forum_topic (
  title, description, author_id, space_id, category, is_public,
  cover_image_url, cover_video_url, pinned, locked, views, created_at, updated_at
)
SELECT 'Ressources gratuites pour la prononciation',
       'YouGlish, Forvo, et répéter après des extraits courts de séries. Le texte compte autant que le son : articuler sans sur-artificialiser l’accent.',
       4, @space_general, 'GENERAL', 1, NULL, NULL, 0, 0, 20, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 6 HOUR
FROM DUAL
WHERE @space_general IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM forum_topic WHERE title = 'Ressources gratuites pour la prononciation');

INSERT INTO forum_topic (
  title, description, author_id, space_id, category, is_public,
  cover_image_url, cover_video_url, pinned, locked, views, created_at, updated_at
)
SELECT 'Motivation du lundi',
       'Small steps every day > rare big sessions. Qui a tenu une habitude cette semaine ? Racontez en 2 phrases en anglais.',
       5, @space_general, 'GENERAL', 1, NULL, NULL, 0, 0, 5, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR
FROM DUAL
WHERE @space_general IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM forum_topic WHERE title = 'Motivation du lundi');

-- Publication « officielle » par une tutrice (user id 11, account_role TUTOR)
INSERT INTO forum_topic (
  title, description, author_id, space_id, category, is_public,
  cover_image_url, cover_video_url, pinned, locked, views, created_at, updated_at
)
SELECT 'Conseils de la semaine — votre tutrice Marie',
       'Bonjour à tous, cette semaine travaillez 15 minutes par jour sur les temps du passé (past simple vs present perfect). Envoyez-moi vos phrases en message privé si vous voulez un retour rapide.',
       11, @space_general, 'GENERAL', 1, NULL, NULL, 1, 0, 42, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 4 HOUR
FROM DUAL
WHERE @space_general IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM forum_topic WHERE title = 'Conseils de la semaine — votre tutrice Marie');

-- ---------------------------------------------------------------------------
-- Forums par NIVEAU (B1, B2) — page /frontoffice/forum/levels (abonnement requis)
-- ---------------------------------------------------------------------------
SET @sid_b1 := (SELECT id FROM forum_space WHERE type = 'LEVEL' AND space_key = 'B1' LIMIT 1);
SET @sid_b2 := (SELECT id FROM forum_space WHERE type = 'LEVEL' AND space_key = 'B2' LIMIT 1);

INSERT INTO forum_topic (
  title, description, author_id, space_id, category, is_public,
  cover_image_url, cover_video_url, pinned, locked, views, created_at, updated_at
)
SELECT 'Club B1 — planifier un voyage',
       'Phrases utiles à l’aéroport, à l’hôtel et pour demander votre chemin. Partagez vos astuces !',
       2, @sid_b1, 'B1', 0, NULL, NULL, 0, 0, 9, NOW() - INTERVAL 8 HOUR, NOW() - INTERVAL 8 HOUR
FROM DUAL
WHERE @sid_b1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM forum_topic t WHERE t.category = 'B1' AND t.title = 'Club B1 — planifier un voyage');

INSERT INTO forum_topic (
  title, description, author_id, space_id, category, is_public,
  cover_image_url, cover_video_url, pinned, locked, views, created_at, updated_at
)
SELECT 'Atelier B1 — emails professionnels courts',
       'Modèles de sujets de mail et formules de politesse. Correction bienveillante par les pairs.',
       11, @sid_b1, 'B1', 0, NULL, NULL, 1, 0, 31, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR
FROM DUAL
WHERE @sid_b1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM forum_topic t WHERE t.category = 'B1' AND t.title = 'Atelier B1 — emails professionnels courts');

INSERT INTO forum_topic (
  title, description, author_id, space_id, category, is_public,
  cover_image_url, cover_video_url, pinned, locked, views, created_at, updated_at
)
SELECT 'Forum B2 — débats & nuance',
       'Sujets d’actualité : argumentez avec précision (connecteurs, concession, contre-argument).',
       4, @sid_b2, 'B2', 0, NULL, NULL, 0, 0, 18, NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 5 HOUR
FROM DUAL
WHERE @sid_b2 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM forum_topic t WHERE t.category = 'B2' AND t.title = 'Forum B2 — débats & nuance');

-- ---------------------------------------------------------------------------
-- Discussions privées — collègues (PEER) et avec tuteur (WITH_TUTOR)
-- participant_low_id = min(id1,id2), participant_high_id = max(id1,id2)
-- Suppression des anciennes lignes de démo (ids 1–9) pour pouvoir ré-exécuter
-- ---------------------------------------------------------------------------
DELETE FROM direct_message WHERE conversation_id BETWEEN 1 AND 9;
DELETE FROM direct_conversation WHERE id BETWEEN 1 AND 9;

INSERT INTO direct_conversation (id, participant_low_id, participant_high_id, kind, created_at, updated_at) VALUES
(1, 1, 2, 'PEER',        NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 1 HOUR),
(2, 1, 3, 'PEER',        NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 30 MINUTE),
(3, 1, 11, 'WITH_TUTOR', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 10 MINUTE),
(4, 2, 11, 'WITH_TUTOR', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 HOUR),
(5, 4, 12, 'WITH_TUTOR', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 45 MINUTE),
(6, 13, 19, 'WITH_TUTOR', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 20 MINUTE),
(7, 14, 20, 'WITH_TUTOR', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 55 MINUTE),
(8, 15, 19, 'WITH_TUTOR', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 35 MINUTE),
(9, 16, 20, 'WITH_TUTOR', NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 8 MINUTE);

-- Messages dans ces conversations (conversation_id doit exister)
INSERT INTO direct_message (conversation_id, sender_id, content, created_at) VALUES
(1, 1, 'Salut Sara ! Tu es dispo pour réviser le vocabulaire du voyage vendredi ?', NOW() - INTERVAL 3 DAY),
(1, 2, 'Oui Alex, 18h ça me va. On fait une liste de 20 mots ensemble ?', NOW() - INTERVAL 3 DAY + INTERVAL 5 MINUTE),
(1, 1, 'Parfait, j’envoie un doc partagé ce soir.', NOW() - INTERVAL 2 DAY),

(2, 3, 'Youssef ici — tu avais parlé d’un groupe B1, je suis partant.', NOW() - INTERVAL 2 DAY),
(2, 1, 'Super, j’ajoute Inès et on fixe un créneau.', NOW() - INTERVAL 2 DAY + INTERVAL 15 MINUTE),

(3, 1, 'Bonjour Marie, j’aimerais un retour sur ma présentation orale.', NOW() - INTERVAL 5 DAY),
(3, 11, 'Bonjour Alex, envoie-moi ton texte (2 min) et on corrige l’intonation ensemble.', NOW() - INTERVAL 5 DAY + INTERVAL 20 MINUTE),
(3, 1, 'Je vous envoie le fichier PDF dans la foulée.', NOW() - INTERVAL 4 DAY),
(3, 11, 'Reçu — attention aux liaisons sur « an interesting idea ». À bientôt !', NOW() - INTERVAL 10 MINUTE),

(4, 2, 'Marie, pouvez-vous valider mon exercice de past perfect ?', NOW() - INTERVAL 4 DAY),
(4, 11, 'Oui Sara : la phrase 3 est correcte ; en 5 préférez « had already left ».', NOW() - INTERVAL 3 DAY),

(5, 4, 'Mr Wilson, question sur les phrasal verbs en contexte pro.', NOW() - INTERVAL 1 DAY),
(5, 12, 'Hi Inès — list 5 verbs you use weekly; we’ll build sentences tomorrow.', NOW() - INTERVAL 45 MINUTE),

(6, 13, 'Bonjour Nadia, pouvez-vous corriger mon texte argumentatif ?', NOW() - INTERVAL 2 DAY),
(6, 19, 'Oui Issra, envoie ta version et je te donne un feedback structuré.', NOW() - INTERVAL 1 DAY),

(7, 14, 'Omar, j’ai besoin d’aide pour l’expression orale A2.', NOW() - INTERVAL 3 DAY),
(7, 20, 'Bien sûr Mouna, on commence par des dialogues guidés simples.', NOW() - INTERVAL 55 MINUTE),

(8, 15, 'Nadia, j’aimerais améliorer mes transitions en rédaction.', NOW() - INTERVAL 1 DAY),
(8, 19, 'Très bonne idée. Commence par “however / moreover / therefore”.', NOW() - INTERVAL 35 MINUTE),

(9, 16, 'Omar, puis-je préparer une présentation de 5 min pour feedback ?', NOW() - INTERVAL 6 HOUR),
(9, 20, 'Excellent, envoie-moi le plan et on fait une simulation orale.', NOW() - INTERVAL 8 MINUTE);

-- Recalage AUTO_INCREMENT pour les prochains enregistrements créés par l’appli
ALTER TABLE direct_conversation AUTO_INCREMENT = 100;
ALTER TABLE direct_message AUTO_INCREMENT = 100;

-- =============================================================================
-- Vérifications rapides (optionnel)
-- =============================================================================
-- USE userdb;  SELECT id, first_name, last_name, account_role FROM users ORDER BY id;
-- USE forumdb; SELECT id, title FROM forum_topic ORDER BY id DESC LIMIT 10;
-- USE forumdb; SELECT * FROM direct_conversation;
-- USE forumdb; SELECT * FROM direct_message ORDER BY conversation_id, id;
-- =============================================================================
-- Front Angular : currentUserId = 1 → inbox montrera les conv. 1, 2, 3
-- Onglet Collègues : 1↔2, 1↔3 | Onglet Tuteur : 1↔11, 2↔11, etc.
-- =============================================================================
