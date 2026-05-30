package org.mwolff.api.auth.web;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.mwolff.api.auth.web.dto.MeResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class MeController {

  @GetMapping("/api/me")
  MeResponse me(final JwtAuthenticationToken authentication) {
    final Jwt jwt = authentication.getToken();
    return new MeResponse(
        jwt.getSubject(),
        jwt.getClaimAsString("preferred_username"),
        jwt.getClaimAsString("email"),
        jwt.getClaimAsString("given_name"),
        jwt.getClaimAsString("family_name"),
        extractRealmRoles(jwt));
  }

  private static List<String> extractRealmRoles(final Jwt jwt) {
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
        .toList();
  }
}
