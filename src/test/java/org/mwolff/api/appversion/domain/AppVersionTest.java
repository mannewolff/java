package org.mwolff.api.appversion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppVersionTest {

  @Test
  void ofCreatesVersionWithGivenMajorAndMinor() {
    // When
    final AppVersion version = AppVersion.of(0, 1);

    // Then
    assertThat(version.major()).isZero();
    assertThat(version.minor()).isEqualTo(1);
  }

  @Test
  void withIncrementedMinorRaisesMinorByOne() {
    // Given
    final AppVersion version = AppVersion.of(0, 1);

    // When
    final AppVersion next = version.withIncrementedMinor();

    // Then
    assertThat(next).isEqualTo(AppVersion.of(0, 2));
  }

  @Test
  void withIncrementedMajorRaisesMajorAndResetsMinor() {
    // Given
    final AppVersion version = AppVersion.of(0, 99);

    // When
    final AppVersion next = version.withIncrementedMajor();

    // Then
    assertThat(next).isEqualTo(AppVersion.of(1, 0));
  }

  @Test
  void formatJoinsMajorAndMinorWithDot() {
    assertThat(AppVersion.of(0, 1).format()).isEqualTo("0.1");
    assertThat(AppVersion.of(12, 34).format()).isEqualTo("12.34");
  }
}
