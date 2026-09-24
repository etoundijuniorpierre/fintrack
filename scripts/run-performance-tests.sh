#!/bin/bash

# Script d'Exécution des Tests de Performance
# Implémentation Phase 4 & 6.1

echo "🚀 Exécution des Tests de Performance - Phase 4 & 6.1"
echo "=============================================="

# Couleurs pour la sortie
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # Aucune Couleur

# Fonction pour afficher le texte coloré
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Vérifier si Docker est en cours d'exécution
if ! docker info > /dev/null 2>&1; then
    print_error "Docker n'est pas en cours d'exécution. Veuillez d'abord démarrer Docker."
    exit 1
fi

print_status "Démarrage du conteneur Redis pour les tests..."

# Démarrer le conteneur Redis pour les tests
docker run -d --name fintrack-redis-test -p 6380:6379 redis:7-alpine > /dev/null 2>&1

# Attendre que Redis soit prêt
sleep 3

print_status "Exécution des Tests Backend..."

# Tests Backend
cd backend/user-service

print_status "Exécution des tests du User Service avec couverture de code..."
mvn clean test jacoco:report -Dspring.profiles.active=test -Dspring.data.redis.port=6380

if [ $? -eq 0 ]; then
    print_success "Tests backend terminés avec succès"
    
    # Vérifier la couverture
    if [ -f "target/site/jacoco/index.html" ]; then
        print_status "Rapport de couverture généré sous : target/site/jacoco/index.html"
        
        # Extraire le pourcentage de couverture (analyse basique)
        if command -v grep > /dev/null 2>&1; then
            COVERAGE=$(grep -o 'Total[^%]*%' target/site/jacoco/index.html | head -1 | grep -o '[0-9]*%' | head -1)
            if [ ! -z "$COVERAGE" ]; then
                print_status "Couverture de test actuelle : $COVERAGE"
                
                # Vérifier si la couverture atteint l'objectif (80%)
                COVERAGE_NUM=$(echo $COVERAGE | sed 's/%//')
                if [ "$COVERAGE_NUM" -ge 80 ]; then
                    print_success "Objectif de couverture atteint ! ($COVERAGE >= 80%)"
                else
                    print_warning "Couverture sous l'objectif : $COVERAGE < 80%"
                fi
            fi
        fi
    fi
else
    print_error "Échec des tests backend"
    docker stop fintrack-redis-test > /dev/null 2>&1
    docker rm fintrack-redis-test > /dev/null 2>&1
    exit 1
fi

cd ../../

print_status "Exécution des Tests Frontend..."

# Tests Frontend
cd frontend

print_status "Installation des dépendances..."
npm ci --silent

print_status "Exécution des tests frontend avec couverture de code..."
npm run test:coverage

if [ $? -eq 0 ]; then
    print_success "Tests frontend terminés avec succès"
    
    # Vérifier si le rapport de couverture existe
    if [ -f "coverage/index.html" ]; then
        print_status "Rapport de couverture frontend généré sous : coverage/index.html"
    fi
else
    print_error "Échec des tests frontend"
    docker stop fintrack-redis-test > /dev/null 2>&1
    docker rm fintrack-redis-test > /dev/null 2>&1
    exit 1
fi

cd ../

print_status "Exécution des Benchmarks de Performance..."

# Tests de Performance
print_status "Test de performance Redis..."
docker exec fintrack-redis-test redis-benchmark -q -n 1000 -c 10 -P 5

print_status "Nettoyage des conteneurs de test..."
docker stop fintrack-redis-test > /dev/null 2>&1
docker rm fintrack-redis-test > /dev/null 2>&1

print_success "Tous les tests sont terminés avec succès !"

echo ""
echo "📊 Résumé des Améliorations de Performance :"
echo "============================================"
echo "✅ Cache Redis implémenté pour les données utilisateur"
echo "✅ Index de base de données ajoutés pour de meilleures performances de requêtes"
echo "✅ Optimisations Frontend avec memoization et chargement différé (lazy loading)"
echo "✅ Tableau virtualisé pour les grands ensembles de données"
echo "✅ Couverture de tests améliorée avec des tests supplémentaires"
echo ""
echo "🎯 Prochaines Étapes :"
echo "- Surveiller les taux de succès du cache en production"
echo "- Analyser les performances des requêtes avec les nouveaux index"
echo "- Mesurer les améliorations de la taille des paquets frontend"
echo "- Mettre en place des tableaux de bord de suivi des performances"
echo ""

print_success "Phase 4 (Performance) et Phase 6.1 (Couverture de Tests) terminées ! 🎉"