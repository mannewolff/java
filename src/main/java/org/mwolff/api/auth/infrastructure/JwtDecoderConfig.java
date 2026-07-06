package org.mwolff.api.auth.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;

/**
 * Stellt den {@link JwtDecoder} des Resource Servers bereit — mit zusätzlicher Audience-Validierung
 * (#311): neben der Standard-Prüfung (Signatur, Issuer, Ablauf) muss der {@code aud}-Claim {@code
 * toolbox-api} enthalten (siehe {@link AudienceValidator}). So werden Tokens abgelehnt, die für
 * einen anderen Zweck ausgestellt wurden und nur zufällig denselben Issuer teilen.
 *
 * <p>Key-Quelle (#320): Ist ein {@code jwk-set-uri} gesetzt, werden die Signaturschlüssel von dort
 * geladen und der {@code iss}-Claim nur offline gegen {@code issuer-uri} validiert — <b>ohne</b>
 * OIDC-Discovery über {@code issuer-uri}. Das ist im Docker-Profil zwingend: der Token trägt {@code
 * iss=http://localhost:8081/...} (vom Browser bezogen), aber der api-Container erreicht {@code
 * localhost:8081} nicht — die Keys liegen unter {@code http://keycloak:8080/.../certs}. Ohne
 * gesetztes {@code jwk-set-uri} (dev/prod, wo {@code issuer-uri} erreichbar ist) wird wie bisher
 * per {@link NimbusJwtDecoder#withIssuerLocation} über Discovery aufgelöst.
 *
 * <p>Der Decoder wird über einen {@link SupplierJwtDecoder} lazy gebaut — der Schlüssel-/Discovery-
 * Bezug gegen Keycloak passiert erst beim ersten Token, nicht beim Start. So bleibt der
 * Anwendungskontext auch ohne erreichbares Keycloak startfähig (Tests, Offline-Start).
 *
 * <p>Bewusst als eigene Config-Klasse getrennt (analog {@code PythonToolsConfig}): die
 * Nimbus-Verdrahtung ist ohne echtes Keycloak nicht unit-testbar und daher aus der Coverage
 * ausgeschlossen. Die eigentliche Sicherheitslogik liegt im separat getesteten {@link
 * AudienceValidator}; die Key-Quellen-Entscheidung im ebenfalls getesteten {@link
 * #preferJwkSetUri(String)}.
 */
@Configuration
public class JwtDecoderConfig {

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") final String issuerUri,
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") final String jwkSetUri,
      @Value("${toolbox.auth.expected-audience:toolbox-api}") final String expectedAudience) {
    return new SupplierJwtDecoder(
        () -> {
          final NimbusJwtDecoder decoder =
              preferJwkSetUri(jwkSetUri)
                  ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
                  : NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
          decoder.setJwtValidator(
              new DelegatingOAuth2TokenValidator<>(
                  JwtValidators.createDefaultWithIssuer(issuerUri),
                  new AudienceValidator(expectedAudience)));
          return decoder;
        });
  }

  /**
   * {@code true}, wenn ein {@code jwk-set-uri} gesetzt ist (nicht {@code null}/leer/blank) — dann
   * werden die Keys von dort bezogen statt per Discovery über {@code issuer-uri}.
   */
  static boolean preferJwkSetUri(final String jwkSetUri) {
    return jwkSetUri != null && !jwkSetUri.isBlank();
  }
}
