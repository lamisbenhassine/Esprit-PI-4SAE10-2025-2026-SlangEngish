-- Script SQL pour ajouter la colonne image_url à la table subscription_plan
-- Exécutez ce script dans votre base de données MySQL si la colonne n'existe pas déjà

USE inscriptiondb;

-- Vérifier si la colonne existe déjà (optionnel)
-- Si vous obtenez une erreur "Duplicate column name", c'est que la colonne existe déjà

ALTER TABLE subscription_plan 
ADD COLUMN IF NOT EXISTS image_url MEDIUMTEXT;

-- Vérifier la structure de la table
DESCRIBE subscription_plan;

