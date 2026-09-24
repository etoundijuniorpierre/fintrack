// Securite : applique l'authentification et les autorisations liees a user details impl.

package com.fintrack.reporting.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// Implementation personnalisee de l'interface Spring Security UserDetails.

public class UserDetailsImpl implements UserDetails {

  private static final long serialVersionUID = 1L;

  @Getter
  private final UUID id;

  private final String username;

  @JsonIgnore
  private final String password;

  private final boolean enabled;
  private final Collection<? extends GrantedAuthority> authorities;

  @Getter
  private final UUID serviceId;

  @Getter
  private final UUID agencyId;

  // Realise l'intention metier user details impl.
  public UserDetailsImpl(
    UUID id,
    String username,
    String password,
    boolean enabled,
    Collection<? extends GrantedAuthority> authorities
  ) {
    this(id, username, password, enabled, authorities, null, null);
  }

  // Realise l'intention metier user details impl.
  public UserDetailsImpl(
    UUID id,
    String username,
    String password,
    boolean enabled,
    Collection<? extends GrantedAuthority> authorities,
    UUID serviceId,
    UUID agencyId
  ) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.enabled = enabled;
    this.authorities = authorities;
    this.serviceId = serviceId;
    this.agencyId = agencyId;
  }

  // Fournit authorities a la couche appelante.

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  // Fournit password a la couche appelante.

  @Override
  public String getPassword() {
    return password;
  }

  // Fournit username a la couche appelante.

  @Override
  public String getUsername() {
    return username;
  }

  // Verifie si account non expired.

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  // Verifie si account non locked.

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  // Verifie si credentials non expired.

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  // Verifie si enabled.

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
