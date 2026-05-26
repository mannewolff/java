package org.mwolff.api.auth;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps the Keycloak-specific {@code realm_access.roles} claim into Spring Security authorities with
 * the conventional {@code ROLE_} prefix so that {@code hasRole(...)} expressions work
 * out-of-the-box.
 */
final class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Override
  public Collection<GrantedAuthority> convert(final Jwt jwt) {
    final Object realmAccess = jwt.getClaim("realm_access");
    if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
      return List.of();
    }
    final Object roles = realmAccessMap.get("roles");
    if (!(roles instanceof Collection<?> roleCollection)) {
      return List.of();
    }
    return roleCollection.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .toList();
  }
}
