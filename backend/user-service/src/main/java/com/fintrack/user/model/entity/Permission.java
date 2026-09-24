// Entite metier : represente les donnees persistees liees a permission.

package com.fintrack.user.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

// Entite JPA modelisant un droit d'acces unitaire du systeme.

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(length = 1000)
  private String description;
}
