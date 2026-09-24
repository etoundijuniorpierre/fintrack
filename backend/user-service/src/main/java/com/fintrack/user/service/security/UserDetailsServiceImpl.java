// Contrat metier : expose les operations du domaine utilisateur details.

package com.fintrack.user.service.security;

import com.fintrack.user.model.entity.User;
import com.fintrack.user.repository.UserRepository;
import com.fintrack.user.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementation du service de chargement des utilisateurs pour Spring Security.

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  // Charge l'utilisateur Spring Security a partir de son identifiant.
  public UserDetails loadUserByUsername(String username)
    throws UsernameNotFoundException {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() ->
        new UsernameNotFoundException(
          "Utilisateur introuvable avec l'identifiant de connexion : " +
            username
        )
      );

    return UserDetailsImpl.build(user);
  }
}
