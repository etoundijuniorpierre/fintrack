#!/bin/bash
set -euo pipefail

# Script pour configurer automatiquement la tâche CRON de sauvegarde
# pour FinTrack (tous les jours à 20h00)

# Récupérer le chemin absolu du répertoire actuel (scripts/)
SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_SCRIPT="$SCRIPTS_DIR/backup.sh"

# Vérifier si le script de backup existe
if [ ! -f "$BACKUP_SCRIPT" ]; then
    echo "Erreur : Le script de sauvegarde $BACKUP_SCRIPT est introuvable."
    exit 1
fi

# Rendre le script backup.sh exécutable par sécurité
chmod +x "$BACKUP_SCRIPT"

# La commande CRON à ajouter (0 20 * * * = 20h00 tous les jours)
# La sortie (succès ou erreur) est redirigée vers /var/log/fintrack-backup.log
CRON_JOB="0 20 * * * $BACKUP_SCRIPT >> /var/log/fintrack-backup.log 2>&1"

# Vérifier si la tâche existe déjà pour éviter les doublons
if crontab -l 2>/dev/null | grep -q "$BACKUP_SCRIPT"; then
    echo "Une tâche CRON pour le script de sauvegarde existe déjà."
    echo "Veuillez vérifier manuellement avec 'crontab -e'."
    exit 0
fi

# Ajouter la nouvelle tâche CRON (tout en conservant les anciennes)
echo "Ajout de la tâche CRON pour exécution à 20h00..."
(crontab -l 2>/dev/null || true; echo "$CRON_JOB") | crontab -

echo "Succès ! La tâche planifiée a été configurée avec succès."
echo "La sauvegarde s'exécutera automatiquement tous les jours à 20h00."
echo "Les logs seront disponibles dans /var/log/fintrack-backup.log"
