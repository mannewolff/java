package org.mwolff.api.auth.infrastructure;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Prüft, dass ein JWT die erwartete Audience ({@code toolbox-api}) trägt (#311). Alle legitimen
 * Toolbox-Clients (Web, iOS, CLI) füllen den {@code aud}-Claim per Audience-Mapper mit {@code
 * toolbox-api}. Ein Token, das ohne diesen Mapper — also nicht für die Toolbox-API — ausgestellt
 * wurde, wird so vom Resource Server abgelehnt (401), statt allein aufgrund gültiger
 * Signatur/Issuer akzeptiert zu werden.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

  private final String requiredAudience;

  public AudienceValidator(String requiredAudience) {
    this.requiredAudience = requiredAudience;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    if (jwt.getAudience() != null && jwt.getAudience().contains(requiredAudience)) {
      return OAuth2TokenValidatorResult.success();
    }
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error(
            "invalid_token", "Required audience '" + requiredAudience + "' is missing", null));
  }
}
