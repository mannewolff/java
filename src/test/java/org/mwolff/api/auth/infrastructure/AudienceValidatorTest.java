package org.mwolff.api.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

  private final AudienceValidator validator = new AudienceValidator("toolbox-api");

  private static Jwt jwtWithAudience(List<String> audience) {
    final Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").subject("user-1");
    if (audience != null) {
      builder.audience(audience);
    }
    return builder.build();
  }

  @Test
  void acceptsTokenWithExpectedAudience() {
    assertThat(validator.validate(jwtWithAudience(List.of("toolbox-api"))).hasErrors()).isFalse();
  }

  @Test
  void acceptsTokenWhenExpectedAudienceIsAmongMultiple() {
    assertThat(validator.validate(jwtWithAudience(List.of("other", "toolbox-api"))).hasErrors())
        .isFalse();
  }

  @Test
  void rejectsTokenWithDifferentAudience() {
    assertThat(validator.validate(jwtWithAudience(List.of("toolbox-cli-only"))).hasErrors())
        .isTrue();
  }

  @Test
  void rejectsTokenWithoutAudience() {
    assertThat(validator.validate(jwtWithAudience(null)).hasErrors()).isTrue();
  }
}
