// Entite metier : represente les donnees persistees liees a agency.

package com.fintrack.user.model.entity;

import jakarta.persistence.*;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Entite JPA representant une agence physique.

@Getter
@Setter
@ToString(exclude = { "headOfAgency", "users" })
@EqualsAndHashCode(callSuper = true, exclude = { "headOfAgency", "users" })
@Entity
@Table(name = "agencies")
public class Agency extends BaseEntity {

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(nullable = false, unique = true, length = 100)
  private String code;

  @Column(length = 100)
  private String city;

  @Column(length = 1000)
  private String address;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "head_user_id", unique = true)
  private User headOfAgency;

  @OneToMany(mappedBy = "agency", fetch = FetchType.LAZY)
  private Set<User> users;
}
