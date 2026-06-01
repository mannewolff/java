package org.mwolff.api.appversion.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mwolff.api.appversion.domain.AppVersion;

class AppVersionResponseTest {

  @Test
  void fromMapsDomainToDto() {
    final AppVersionResponse dto = AppVersionResponse.from(AppVersion.of(0, 1));
    assertThat(dto.major()).isZero();
    assertThat(dto.minor()).isEqualTo(1);
  }

  @Test
  void formattedJoinsMajorAndMinorWithDot() {
    assertThat(new AppVersionResponse(0, 1).formatted()).isEqualTo("0.1");
    assertThat(new AppVersionResponse(12, 34).formatted()).isEqualTo("12.34");
  }
}
