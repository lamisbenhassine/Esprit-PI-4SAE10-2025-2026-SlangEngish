-- Corrige l'erreur "Data truncated for column 'status'" en passant la colonne en VARCHAR.
-- À exécuter une fois dans ta base db_school_platform (MySQL).

USE db_school_platform;

ALTER TABLE applications
  MODIFY COLUMN status VARCHAR(50) NOT NULL;
