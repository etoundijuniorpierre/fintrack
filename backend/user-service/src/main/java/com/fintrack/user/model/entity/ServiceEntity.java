// Entite metier : represente les donnees persistees liees a service.

package com.fintrack.user.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Entite metier representant service.

@Getter
@Setter
@ToString(exclude = { "headOfService", "users" })
@EqualsAndHashCode(callSuper = true, exclude = { "headOfService", "users" })
@Entity(name = "Service")
@Table(name = "services")
public class ServiceEntity extends BaseEntity {

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(length = 1000)
  private String description;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "head_user_id")
  private User headOfService;

  @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
  private Set<User> users;
}
