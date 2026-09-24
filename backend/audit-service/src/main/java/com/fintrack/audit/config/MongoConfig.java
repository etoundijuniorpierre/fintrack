// Configuration Spring : declare les regles techniques liees a mongo.

package com.fintrack.audit.config;

import org.bson.UuidRepresentation;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

// Declare la configuration technique du domaine mongo.

@Configuration
@EnableMongoAuditing
public class MongoConfig {

  @Bean
  // Realise l'intention metier uuid representation personnalisation.
  MongoClientSettingsBuilderCustomizer uuidRepresentationCustomizer() {
    return builder -> builder.uuidRepresentation(UuidRepresentation.STANDARD);
  }
}
