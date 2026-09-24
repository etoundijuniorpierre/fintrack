# FinTrack

## 🎯 Objectif

En agence **FINSTAR**, lorsqu’un problème survient — qu’il soit informatique, financier ou opérationnel — il n’existe pas toujours un moyen clair, structuré et traçable pour le gérer du début jusqu’à sa résolution. C’est précisément à ce besoin que répond **FinTrack**.

FinTrack est un outil simple qui permet de :

- **Déclarer un incident** et l'affecter au bon service.
- **Suivre son évolution** en temps réel (en attente, en cours, résolu, bloqué).
- **Garantir une traçabilité complète** pour les audits et contrôles (historique des actions).
- **Centraliser les cas spécifiques**, comme les incidents de paiement.
- **Aider à la décision** grâce à des rapports identifiant les points faibles et les services sollicités.

En résumé, FinTrack permet de ne plus subir les incidents, mais de les maîtriser : on les enregistre, on les suit, on les résout, et surtout, on en tire des enseignements.

---

## 🏗️ Architecture et Fonctionnalités par Service

FinTrack repose sur une architecture en **microservices** conteneurisés afin d'assurer modularité, haute performance et scalabilité. L'intégralité des fonctionnalités est répartie selon la logique métier de chaque service :

### 🖥️ Frontend (React / TypeScript / Vite)

- **Interface Utilisateur Moderne** : Tableau de bord dynamique, gestion fine des états (Redux/Context), vues interactives avec graphiques (Recharts) et data grids fluides.
- **Accessibilité & UX** : Mode "View As" (Simulation d'identité pour les SuperAdmins), panneaux de configuration personnalisables par utilisateur, rafraîchissement silencieux des sessions.

### ⚙️ Backend (Microservices Spring Boot)

#### 1. `user-service` (Comptes, Sécurité & Référentiels)

- **Authentification & Sécurité** : Connexion, obligation de changer le mot de passe initial, double authentification interne (re-authentication) pour actions sensibles, blocage de compte après échecs.
- **Profil Utilisateur** : Gestion des informations personnelles, visualisation des rôles et habilitations granulaires, badge spécifique (Chef de Service).
- **Administration** : Gestion des Agences, des Services et des Rôles système.

#### 2. `incident-service` (Cœur du Métier)

- **Tableau de Bord (Dashboard)** : KPIs globaux (Temps moyen de résolution, distribution des incidents), filtrage par périmètre de visibilité (OWN, SERVICE, AGENCY, ALL).
- **Cycle de Vie des Incidents** : Workflows complexes (Ouvert, En attente de validation, En cours, Bloqué, Transféré, Résolu, Clos, Rejeté) avec niveaux de criticité (Faible à Critique).
- **Actions de Transition** : Assignation, auto-assignation, transfert automatique/manuel, blocage avec motif.
- **Collaboration** : Fils de discussion et commentaires sur chaque incident.

#### 3. `document-service` (Pièces Jointes & Fichiers)

- **Stockage Sécurisé** : Intégration MinIO (S3-compatible) pour l'hébergement des documents et exports.
- **Gestion des Téléversements** : Contrôle strict des types MIME et de la taille (max 20Mo).
- **Intégrité des Données** : Verrouillage strict des pièces jointes d'un incident dès sa prise en charge pour garantir la non-altération (Audit).

#### 4. `notification-service` (Alertes & Escalades)

- **Acheminement** : Envoi d'alertes en temps réel (SSE/WebSockets) et notifications par e-mail.
- **Gouvernance** : Relance des envois échoués, notifications urgentes pour les incidents "Critiques".

#### 5. `reporting-service` (Rapports & Statistiques)

- **Extraction de Données** : Génération de rapports complets aux formats PDF, Excel ou JSON.
- **Planification** : Programmation d'envois automatisés (quotidiens, hebdomadaires, mensuels) aux destinataires choisis.

#### 6. `audit-service` (Traçabilité & Journalisation)

- **Journal d'Audit Immuable** : Enregistrement de chaque action (Qui, Quoi, Quand, Résultat, IP).
- **Analyse & Recherche** : Filtres croisés, export complet CSV, redirection instantanée vers la ressource impactée (Incident ou Utilisateur).

#### 7. Hub d'Administration "SuperAdmin" (Fonctionnalités gérées de façon transverse)

- **Santé des Services (Uptime)** : Monitoring du cluster, statistiques globales de délivrabilité, et capacité de redémarrage à distance des services.
- **Contrôle Qualité des Données** : Détection d'incidents orphelins, d'utilisateurs désactivés, d'anomalies de référence.
- **Moteur d'Escalade** : Gestion des dépassements SLA et alertes associées.
- **Maintenance** : Purge sélective des journaux d'audit et configuration à la volée des règles métier (limites d'export, intervalles d'escalade, etc.).

### 🐳 Infrastructure

- `nginx` : Reverse proxy gérant le SSL (HTTPS) et le routage intelligent.
- `postgresql` : Base de données relationnelle (données structurées).
- `mongodb` : Base de données NoSQL (traces d'audit, logs de notifications).
- `minio` : Stockage objet pour les fichiers volumineux.

---

## 💻 Technologies de base

- **Frontend** : Vite.js / React / TypeScript / CSS Vanilla.
- **Backend** : Java 21 / Spring Boot 4.0.3 / Spring Cloud / Maven.
- **Bases de données** : PostgreSQL 17 / MongoDB 7.
- **Proxy & Serveur Web** : Nginx 1.24.
- **Conteneurisation** : Docker / Docker Compose.

---

## 🚀 Lancement du Projet

Le projet est entièrement automatisé avec Docker.

### Prérequis

- Docker & Docker Compose installés.
- Un fichier `.env` configuré à la racine à partir de `.env.example`.

### Commandes de lancement

1.  **Préparation du domaine local (Optionnel mais recommandé)** :
    Ajoutez la ligne suivante à votre fichier `hosts` (`C:\Windows\System32\drivers\etc\hosts` sous Windows) :

    ```text
    127.0.0.1   finstar.fintrack.org
    ```

2.  **Certificat TLS local** :
    À exécuter une fois par poste avant le premier démarrage en mode production local :

    ```powershell
    .\scripts\generate-local-tls.ps1
    ```

3.  **Démarrage complet** :
    À la racine du projet, lancez :

    ```bash
    docker-compose up -d --build
    ```

    _Cette commande construit les images et lance tous les services, bases de données et le proxy Nginx._

4.  **Accès à l'application** :
    Ouvrez votre navigateur sur [**https://finstar.fintrack.org**](https://finstar.fintrack.org).
    _Note : Comme le certificat est auto-signé, cliquez sur **Paramètres avancés** puis sur **Continuer vers le site** lors du premier accès._

### Explication du flux de lancement

- **db-init** : Un service temporaire initialise les bases de données et crée les schémas nécessaires.
- **Backend Services** : Chaque service attend que sa base de donnée respective soit "healthy" avant de démarrer.
- **Nginx** : Une fois lancé, il redirige les requêtes `/` vers le frontend et les requêtes `/api/` vers les services backend appropriés.

5. **Structure des répertoires**
   fintrack/
   ├── docker-compose.yml
   ├── docker-compose.prod.yml
   ├── docker-compose.staging.yml
   ├── .env
   ├── .dockerignore
   │
   ├── frontend/
   │ ├── Dockerfile
   │ ├── .dockerignore
   │ ├── nginx.conf
   │ ├── package.json
   │ └── src/
   │
   ├── backend/
   │ ├── service/
   │ │ ├── Dockerfile
   │ │ ├── pom.xml
   │ │ └── src/
   │
   ├── nginx/
   │ ├── templates/
   │ │ └── default.conf.template
   │ ├── ssl/
   │ │ ├── fintrack.crt
   │ │ ├── fintrack.key
   │ └── nginx.conf
   │
   ├── scripts/
   │ ├── backup.sh
   │ ├── restore.sh
   │ ├── health-check.sh
   │ ├── init-db.sh
   │ ├── generate-local-tls.ps1
   │ ├── postgres-init/
   │ └── mongo-init/

---

5. **Structure du dépôt**
   fintrack/
   ├── .git/
   ├── .gitignore
   ├── .gitattributes
   │
   ├── backend/
   │ ├── service/
   │ │ ├── .gitignore
   │ │ ├── pom.xml
   │ │ └── src/
   │
   ├── frontend/
   │ ├── .gitignore
   │ ├── package.json
   │ └── src/
   │
   ├── nginx/
   │ ├── templates/
   │ └── ssl/
   │
   ├── scripts/
   │
   ├── docker-compose.yml
   ├── docker-compose.prod.yml
   ├── .env.example
   ├── .gitignore
   └── README.md

## 🌿 Branches & environnements

Trois branches principales partagent le **même code** ; elles ne diffèrent que par leur rôle et l'environnement _actif_.

| Branche                  | Rôle                                                                               | Particularités                                                 |
| ------------------------ | ---------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| `dev`                    | Développement / intégration (cible des merges de features). **CI** = tests + lint. | Contient `documentation/`. Profil par défaut : **dev**.        |
| `qa/playwright-coverage` | QA / tests E2E automatisés (Playwright).                                           | = `dev` + origines CORS `127.0.0.1:5173`.                      |
| `prod`                   | Déploiement production.                                                            | **N'inclut jamais `documentation/`.** Profil cible : **prod**. |

**Sélection d'environnement (sans séparer les fichiers par branche)** — le fichier `.env` racine est l'unique source de configuration. Le bloc production est actif par défaut (`COMPOSE_PROFILES=prod`) pour servir le build frontend avec Nginx et activer le profil Spring `prod`. Le bloc développement commenté contient les paramètres du serveur Vite à activer uniquement pour `npm run dev`. Les backends chargent `application.properties` puis `application-{dev,prod}.properties` selon le profil ; `application-test.properties` sert à la CI.

**Garde-fou `documentation/` hors de `prod`** — `.gitattributes` déclare `documentation/** merge=ours`. Activation unique par clone : `git config merge.ours.driver true`. Ensuite `git checkout prod && git merge dev && git push origin prod` conserve la doc exclue (sinon `git rm -r documentation` avant de committer le merge).

---

© 2026 FinStar - FinTrack Project.
