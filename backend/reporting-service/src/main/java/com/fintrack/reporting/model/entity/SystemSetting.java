// Entite metier : represente les donnees persistees liees a system setting.

package com.fintrack.reporting.model.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Parametre systeme modifiable.
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
  name = "system_settings",
  uniqueConstraints = @UniqueConstraint(columnNames = "setting_key")
)
public class SystemSetting extends BaseEntity {

  public static final String CATEGORY_THRESHOLD = "THRESHOLD";
  public static final String CATEGORY_EMAIL_NOTIFICATION = "EMAIL_NOTIFICATION";

  @Column(name = "setting_key", nullable = false, length = 80)
  private String settingKey;

  @Column(name = "setting_value", columnDefinition = "TEXT")
  private String settingValue;

  @Column(name = "category", nullable = false, length = 32)
  private String category;

  @Column(name = "description", length = 255)
  private String description;
}
