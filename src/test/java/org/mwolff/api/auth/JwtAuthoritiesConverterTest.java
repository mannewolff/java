package org.mwolff.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAuthoritiesConverterTest {

  private final JwtAuthoritiesConverter converter = new JwtAuthoritiesConverter();

  @Test
  void shouldMapRealmRolesToRolePrefixedAuthorities() {
    // given
    final Jwt jwt = jwtWithRealmRoles(List.of("USER", "ADMIN"));

    // when
    final List<String> authorities = extractAuthorityStrings(jwt);

    // then
    assertThat(authorities).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  void shouldReturnEmptyAuthoritiesWhenRealmAccessClaimMissing() {
    // given
    final Jwt jwt = jwtWithoutRealmAccess();

    // when
    final List<String> authorities = extractAuthorityStrings(jwt);

    // then
    assertThat(authorities).isEmpty();
  }

  @Test
  void shouldReturnEmptyAuthoritiesWhenRolesListMissing() {
    // given
    final Jwt jwt = jwtWithRealmAccessButNoRoles();

    // when
    final List<String> authorities = extractAuthorityStrings(jwt);

    // then
    assertThat(authorities).isEmpty();
  }

  @Test
  void shouldReturnEmptyAuthoritiesWhenRolesListEmpty() {
    // given
    final Jwt jwt = jwtWithRealmRoles(List.of());

    // when
    final List<String> authorities = extractAuthorityStrings(jwt);

    // then
    assertThat(authorities).isEmpty();
  }

  @Test
  void shouldIgnoreNonStringRoleEntries() {
    // given
    final Jwt jwt = jwtWithRealmAccess(Map.of("roles", List.of("USER", 42, "ADMIN")));

    // when
    final List<String> authorities = extractAuthorityStrings(jwt);

    // then
    assertThat(authorities).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  void shouldReturnEmptyAuthoritiesWhenRolesClaimIsNotACollection() {
    // given
    final Jwt jwt = jwtWithRealmAccess(Map.of("roles", "USER"));

    // when
    final List<String> authorities = extractAuthorityStrings(jwt);

    // then
    assertThat(authorities).isEmpty();
  }

  private List<String> extractAuthorityStrings(final Jwt jwt) {
    return converter.convert(jwt).stream().map(GrantedAuthority::getAuthority).sorted().toList();
  }

  private static Jwt jwtWithRealmRoles(final List<String> roles) {
    return jwtWithRealmAccess(Map.of("roles", roles));
  }

  private static Jwt jwtWithRealmAccess(final Map<String, Object> realmAccess) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("user-1")
        .claim("realm_access", realmAccess)
        .issuedAt(Instant.EPOCH)
        .expiresAt(Instant.EPOCH.plusSeconds(3600))
        .build();
  }

  private static Jwt jwtWithRealmAccessButNoRoles() {
    return jwtWithRealmAccess(Map.of("other", "value"));
  }

  private static Jwt jwtWithoutRealmAccess() {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("user-1")
        .claim("some-other-claim", "value")
        .issuedAt(Instant.EPOCH)
        .expiresAt(Instant.EPOCH.plusSeconds(3600))
        .build();
  }
}
