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
 * <p>Der Decoder wird über einen {@link SupplierJwtDecoder} lazy gebaut — die OIDC-Discovery gegen
 * Keycloak passiert erst beim ersten Token, nicht beim Start. So bleibt der Anwendungskontext auch
 * ohne erreichbares Keycloak startfähig (Tests, Offline-Start).
 *
 * <p>Bewusst als eigene Config-Klasse getrennt (analog {@code PythonToolsConfig}): die
 * Nimbus-Verdrahtung ist ohne echtes Keycloak nicht unit-testbar und daher aus der Coverage
 * ausgeschlossen. Die eigentliche Sicherheitslogik liegt im separat getesteten {@link
 * AudienceValidator}.
 */
@Configuration
public class JwtDecoderConfig {

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") final String issuerUri,
      @Value("${toolbox.auth.expected-audience:toolbox-api}") final String expectedAudience) {
    return new SupplierJwtDecoder(
        () -> {
          final NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
          decoder.setJwtValidator(
              new DelegatingOAuth2TokenValidator<>(
                  JwtValidators.createDefaultWithIssuer(issuerUri),
                  new AudienceValidator(expectedAudience)));
          return decoder;
        });
  }
}
