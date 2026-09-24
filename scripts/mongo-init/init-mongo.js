// Initialise les bases MongoDB applicatives et leurs indexes techniques.

function ensureCollection(database, collectionName) {
  if (!database.getCollectionNames().includes(collectionName)) {
    database.createCollection(collectionName);
  }
}

const auditDb = db.getSiblingDB("auditservicedb");
ensureCollection(auditDb, "audit_logs");
auditDb.audit_logs.createIndex({ timestamp: 1 });

const notificationDb = db.getSiblingDB("notificationservicedb");
ensureCollection(notificationDb, "notifications");
notificationDb.notifications.createIndex({ created_at: -1 });
notificationDb.notifications.createIndex({ recipient: 1 });
notificationDb.notifications.createIndex({ incident_id: 1 });
notificationDb.notifications.createIndex({ status: 1 });
notificationDb.notifications.createIndex({ status: 1, next_retry: 1 });

print("MongoDB application databases initialized.");
