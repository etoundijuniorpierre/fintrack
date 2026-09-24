#!/bin/bash
# Vérifie l'état de santé de tous les conteneurs

echo "Vérification de l'état de santé des services FinTrack..."
docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

echo "\nVérification détaillée de la connectivité des services :"
for service in user-service incident-service document-service notification-service reporting-service audit-service nginx postgresql mongodb; do
    echo -n "$service: "
    status=$(docker inspect -f '{{.State.Health.Status}}' fintrack-$service 2>/dev/null || echo "unknown")
    echo "$status"
done
