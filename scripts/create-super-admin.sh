#!/bin/bash
# Création du super administrateur

echo "Création du super administrateur..."
docker exec fintrack-user-service java -jar app.jar --create-super-admin
echo "Super administrateur créé avec succès"
