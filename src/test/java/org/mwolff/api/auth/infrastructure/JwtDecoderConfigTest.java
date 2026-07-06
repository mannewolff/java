package org.mwolff.api.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Sichert die Key-Quellen-Entscheidung ab (#320): ein gesetztes {@code jwk-set-uri} muss Vorrang
 * vor der Discovery über {@code issuer-uri} haben, damit der api-Container im Docker-Profil die
 * Signaturschlüssel vom erreichbaren {@code keycloak:8080} holt statt vom nicht erreichbaren {@code
 * localhost:8081}. Die Nimbus-Verdrahtung selbst bleibt aus der Coverage ausgeschlossen; hier wird
 * nur die reine Verzweigungslogik geprüft.
 */
class JwtDecoderConfigTest {

  @Test
  void prefersJwkSetUriWhenSet() {
    assertThat(
            JwtDecoderConfig.preferJwkSetUri(
                "http://keycloak:8080/realms/toolbox-dev/protocol/openid-connect/certs"))
        .isTrue();
  }

  @Test
  void fallsBackToIssuerDiscoveryWhenJwkSetUriNull() {
    assertThat(JwtDecoderConfig.preferJwkSetUri(null)).isFalse();
  }

  @Test
  void fallsBackToIssuerDiscoveryWhenJwkSetUriEmpty() {
    assertThat(JwtDecoderConfig.preferJwkSetUri("")).isFalse();
  }

  @Test
  void fallsBackToIssuerDiscoveryWhenJwkSetUriBlank() {
    assertThat(JwtDecoderConfig.preferJwkSetUri("   ")).isFalse();
  }
}
