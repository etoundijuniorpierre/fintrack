-- La catégorie de pièce jointe « PROCEDURE » (soumission de procédure) devient
-- « SOLUTION » (proposition de solution), en cohérence avec le relibellé du concept
-- côté incident-service. On réétiquette les lignes existantes.

UPDATE attachments_metadata
SET category = 'SOLUTION'
WHERE category = 'PROCEDURE';
