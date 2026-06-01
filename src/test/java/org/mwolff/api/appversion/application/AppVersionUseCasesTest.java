package org.mwolff.api.appversion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mwolff.api.appversion.domain.AppVersion;
import org.mwolff.api.appversion.domain.AppVersionPort;

class AppVersionUseCasesTest {

  private final AppVersionPort port = mock(AppVersionPort.class);

  @Test
  void getReturnsCurrentVersion() {
    // Given
    given(port.getCurrent()).willReturn(AppVersion.of(0, 1));

    // When / Then
    assertThat(new GetAppVersionUseCase(port).execute()).isEqualTo(AppVersion.of(0, 1));
  }

  @Test
  void incrementMinorRaisesMinorAndPersists() {
    // Given
    given(port.getCurrent()).willReturn(AppVersion.of(0, 1));

    // When
    final AppVersion result = new IncrementMinorVersionUseCase(port).execute();

    // Then
    assertThat(result).isEqualTo(AppVersion.of(0, 2));
    verify(port).setVersion(AppVersion.of(0, 2));
  }

  @Test
  void incrementMajorRaisesMajorResetsMinorAndPersists() {
    // Given
    given(port.getCurrent()).willReturn(AppVersion.of(0, 99));

    // When
    final AppVersion result = new IncrementMajorVersionUseCase(port).execute();

    // Then
    assertThat(result).isEqualTo(AppVersion.of(1, 0));
    verify(port).setVersion(AppVersion.of(1, 0));
  }
}
