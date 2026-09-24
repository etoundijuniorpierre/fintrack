// Entite metier : represente les donnees persistees liees a notification.

package com.fintrack.notification.model.entity;

import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.constant.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

// Entite representant une notification stockee dans la base de donnees.

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Document(collection = "notifications")
@CompoundIndexes({
  @CompoundIndex(
    name = "idx_notifications_recipient_created_at",
    def = "{'recipient': 1, 'created_at': -1}"
  ),
  @CompoundIndex(
    name = "idx_notifications_status_next_retry",
    def = "{'status': 1, 'next_retry': 1}"
  ),
  @CompoundIndex(
    name = "idx_notifications_incident_created_at",
    def = "{'incident_id': 1, 'created_at': -1}"
  ),
})
public class Notification {

  @Id
  private String id; // ObjectId MongoDB → String

  @NotNull
  private NotificationType type;

  @NotBlank
  @Size(max = 255)
  private String recipient;

  @Field("incident_id")
  private UUID incidentId; // cross-service ref, nullable

  @Indexed(
    name = "ux_notifications_idempotency_key",
    unique = true,
    partialFilter = "{ 'idempotency_key': { $exists: true, $type: 'string' } }"
  )
  @Field("idempotency_key")
  private String idempotencyKey;

  @Size(max = 255)
  private String subject;

  private String content;

  // Variante anglaise rendue a l'emission ; le lecteur choisit sa langue.
  @Size(max = 255)
  private String subjectEn;

  private String contentEn;

  @NotNull
  private NotificationStatus status = NotificationStatus.PENDING;

  @Field("sent_at")
  private LocalDateTime sentAt; // non null si status == SENT

  @Field("retry_count")
  private Integer retryCount = 0; // invariant : >= 0

  @Field("retry_at")
  private LocalDateTime retryAt; // non null si retryCount > 0

  @Field("next_retry")
  private LocalDateTime nextRetry; // > retryAt si les deux non null

  @CreatedDate
  @Field("created_at")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Field("updated_at")
  private LocalDateTime updatedAt;

  @Field("modified_by")
  private UUID modifiedBy;

  @Field("template_params")
  private Map<String, Object> templateParams;
}
