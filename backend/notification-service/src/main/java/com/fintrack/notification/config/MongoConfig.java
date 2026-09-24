// Configuration Spring : declare les regles techniques liees a mongo.

package com.fintrack.notification.config;

import org.bson.UuidRepresentation;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

// Configuration globale pour l'integration de MongoDB.
@Configuration
@EnableMongoAuditing
public class MongoConfig {

  // Realise l'intention metier uuid representation personnalisation.

  @Bean
  public MongoClientSettingsBuilderCustomizer uuidRepresentationCustomizer() {
    return builder -> builder.uuidRepresentation(UuidRepresentation.STANDARD);
  }
}
