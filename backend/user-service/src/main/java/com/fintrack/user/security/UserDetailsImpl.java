// Securite : applique l'authentification et les autorisations liees a user details impl.

package com.fintrack.user.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fintrack.user.model.entity.User;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
  private final boolean firstLogin;

  @Getter
  private final UUID agencyId;

  @Getter
  private final UUID serviceId;

  private final Collection<? extends GrantedAuthority> authorities;

  // Realise l'intention metier user details impl.
  public UserDetailsImpl(
    UUID id,
    String username,
    String password,
    boolean enabled,
    boolean firstLogin,
    UUID agencyId,
    UUID serviceId,
    Collection<? extends GrantedAuthority> authorities
  ) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.enabled = enabled;
    this.firstLogin = firstLogin;
    this.agencyId = agencyId;
    this.serviceId = serviceId;
    this.authorities = authorities;
  }

  // Construit la representation attendue pour le domaine utilisateur details.

  public static UserDetailsImpl build(User user) {
    Set<GrantedAuthority> authorities = new HashSet<>();

    if (user.getRoles() != null) {
      user.getRoles().forEach(role -> {
        authorities.add(
          new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase())
        );
        if (role.getPermissions() != null) {
          role
            .getPermissions()
            .forEach(permission ->
              authorities.add(
                new SimpleGrantedAuthority(permission.getName().toUpperCase())
              )
            );
        }
      });
    }

    if (user.getPermissions() != null) {
      user
        .getPermissions()
        .forEach(permission ->
          authorities.add(
            new SimpleGrantedAuthority(permission.getName().toUpperCase())
          )
        );
    }

    // Soustraction des permissions revoquees : (directes ∪ role) - revoquees.
    if (user.getRevokedPermissions() != null) {
      user
        .getRevokedPermissions()
        .forEach(permission ->
          authorities.remove(
            new SimpleGrantedAuthority(permission.getName().toUpperCase())
          )
        );
    }

    return new UserDetailsImpl(
      user.getId(),
      user.getUsername(),
      user.getPassword(),
      user.isActive(),
      user.isFirstLogin(),
      user.getAgency() != null ? user.getAgency().getId() : null,
      user.getService() != null ? user.getService().getId() : null,
      authorities
    );
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

  // Verifie si first login.

  public boolean isFirstLogin() {
    return firstLogin;
  }
}
