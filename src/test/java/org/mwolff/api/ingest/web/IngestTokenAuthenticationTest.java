package org.mwolff.api.ingest.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class IngestTokenAuthenticationTest {

  @Test
  void exposesUserSubAndTokenId() {
    final IngestTokenAuthentication auth = new IngestTokenAuthentication("user-1", 42L);

    assertThat(auth.getUserSub()).isEqualTo("user-1");
    assertThat(auth.getTokenId()).isEqualTo(42L);
    assertThat(auth.getPrincipal()).isEqualTo("user-1");
    assertThat(auth.getCredentials()).isNull();
    assertThat(auth.isAuthenticated()).isTrue();
    assertThat(auth.getAuthorities()).contains(new SimpleGrantedAuthority("ROLE_INGEST"));
  }
}
