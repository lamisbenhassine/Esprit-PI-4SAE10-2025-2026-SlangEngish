# SlangEnglish -- Intelligent English Learning Platform

## Repository

https://github.com/lamisbenhassine/Esprit-PI-4SAE10-2025-2026-SlangEngish.git

------------------------------------------------------------------------

# Overview

**SlangEnglish** est une plateforme intelligente d'apprentissage de
l'anglais destinée principalement aux utilisateurs âgés de **18 à 45
ans**.

La plateforme combine **intelligence artificielle, microservices et
technologies web modernes** afin d'offrir une expérience d'apprentissage
interactive, personnalisée et sécurisée.

Elle permet aux utilisateurs : - d'apprendre l'anglais via des cours
interactifs - de participer à des quiz intelligents - d'échanger avec
des locuteurs natifs - d'assister à des sessions en live - de suivre
leur progression avec analyse IA

Le projet utilise une **architecture microservices** avec **Spring Boot
pour le backend** et **Angular pour le frontend**.

------------------------------------------------------------------------

# Features

## User Management

-   Inscription sécurisée
-   Authentification avec **Google ou Facebook**
-   Protection **Google reCAPTCHA**
-   **Forgot Password avec envoi d'email**
-   Gestion des rôles : utilisateur, tuteur, administrateur
-   Blocage / suspension de comptes par l'administrateur

------------------------------------------------------------------------

## Smart Placement & Recommendation

-   Test automatique de niveau **A1 → C2**
-   Analyse de certificat uploadé
-   Recommandation automatique d'abonnement
-   Catalogue intelligent de cours selon :
    -   domaine professionnel
    -   niveau linguistique
    -   objectifs d'apprentissage

------------------------------------------------------------------------

## Forum & Community Learning

-   Interaction avec des **locuteurs natifs**
-   Correction des messages et feedback rapide
-   Recommandation automatique :
    -   vidéos
    -   podcasts
    -   séries éducatives
-   Analyse des discussions pour adapter les ressources proposées

------------------------------------------------------------------------

## Streaming & Recordings

-   Cours **live avec tuteur**
-   Interaction en temps réel
-   Accès aux **enregistrements des sessions**
-   Consultation des cours à tout moment

------------------------------------------------------------------------

## Progress Tracking

-   Suivi détaillé de l'avancement :
    -   par cours
    -   par chapitre
    -   par activité
-   Tableau de bord personnel
-   Analyse du progrès d'apprentissage

------------------------------------------------------------------------

## Complaint Management (Machine Learning)

-   Dépôt de réclamations
-   Classification automatique avec **modèle Machine Learning**
-   Catégorisation des réclamations
-   Notification et réponse par l'administrateur

------------------------------------------------------------------------

# AI Quiz, Anti-Cheat & Certification

## Quiz Management

-   Création / modification / suppression de quiz
-   Définition de :
    -   durée
    -   date
    -   note maximale
-   Randomisation des questions et réponses

### Types de questions

-   QCM
-   Vrai / Faux
-   Réponse courte
-   Reading
-   Writing
-   Fill in the blanks

------------------------------------------------------------------------

## AI Integration

-   Analyse du comportement étudiant
-   Détection copier/coller
-   Analyse du temps de réponse
-   Score de crédibilité
-   Prédiction du niveau de l'étudiant

------------------------------------------------------------------------

## Anti-Cheat System

-   Détection changement d'onglet
-   Blocage copier/coller
-   Détection outils développeur
-   Surveillance activité suspecte
-   Attribution automatique **note = 0 en cas de fraude**
-   Génération rapport de triche

------------------------------------------------------------------------

## Certification

-   Test de niveau automatique
-   Attribution niveau :
    -   A1
    -   A2
    -   B1
    -   B2
    -   C1
    -   C2
-   Génération automatique de **certificat**
-   Historique des certifications

------------------------------------------------------------------------

# Tech Stack

## Frontend

-   Angular
-   TypeScript
-   HTML / CSS

## Backend

-   Spring Boot
-   Java

## Architecture

-   Microservices Architecture
-   REST APIs

## Security

-   Google OAuth
-   Facebook OAuth
-   JWT Authentication
-   Google reCAPTCHA

## AI & Data

-   Machine Learning models
-   Data analysis

------------------------------------------------------------------------

# Architecture

Le projet suit une **architecture microservices**, permettant :

-   Scalabilité
-   Maintenance facilitée
-   Déploiement indépendant des services

Principaux microservices :

-   User Service
-   Quiz Service
-   Complaint Service
-   Streaming Service
-   AI Recommendation Service
-   Notification Service

------------------------------------------------------------------------

# Academic Context

Ce projet a été développé dans le cadre d'un **projet universitaire à
ESPRIT** visant à concevoir une plateforme e-learning intelligente basée
sur **IA, microservices et technologies web modernes**.

------------------------------------------------------------------------

# Getting Started

## Prérequis

-   Node.js
-   Angular CLI
-   Java 17+
-   Maven
-   MySQL ou PostgreSQL

------------------------------------------------------------------------

## Installation

### Cloner le projet

git clone
https://github.com/lamisbenhassine/Esprit-PI-4SAE10-2025-2026-SlangEngish.git

### Backend

cd backend\
mvn spring-boot:run

### Frontend

cd frontend\
npm install\
ng serve

Application disponible sur :

http://localhost:4200

------------------------------------------------------------------------

# Contributors

-   Lamis Ben Hassine
-   Team SlangEnglish

------------------------------------------------------------------------

# Future Improvements

-   Chatbot IA pour conversation anglaise
-   Application mobile
-   Recommandation intelligente améliorée
-   Analyse avancée du comportement d'apprentissage
