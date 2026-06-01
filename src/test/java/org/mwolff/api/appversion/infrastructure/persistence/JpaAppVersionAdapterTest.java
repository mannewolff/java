package org.mwolff.api.appversion.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.appversion.domain.AppVersion;

class JpaAppVersionAdapterTest {

  private final AppVersionJpaRepository repository = mock(AppVersionJpaRepository.class);
  private final JpaAppVersionAdapter adapter = new JpaAppVersionAdapter(repository);

  private static AppVersionEntity entity(final int major, final int minor) {
    final AppVersionEntity e = new AppVersionEntity();
    e.setId(1L);
    e.setMajor(major);
    e.setMinor(minor);
    return e;
  }

  @Test
  void getCurrentMapsEntityToRecord() {
    // Given
    given(repository.findById(1L)).willReturn(Optional.of(entity(0, 1)));

    // When / Then
    assertThat(adapter.getCurrent()).isEqualTo(AppVersion.of(0, 1));
  }

  @Test
  void getCurrentThrowsWhenRowMissing() {
    // Given
    given(repository.findById(1L)).willReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(adapter::getCurrent).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void setVersionUpdatesFieldsAndSaves() {
    // Given
    final AppVersionEntity existing = entity(0, 1);
    given(repository.findById(1L)).willReturn(Optional.of(existing));

    // When
    adapter.setVersion(AppVersion.of(2, 5));

    // Then
    assertThat(existing.getMajor()).isEqualTo(2);
    assertThat(existing.getMinor()).isEqualTo(5);
    verify(repository).save(existing);
  }

  @Test
  void setVersionThrowsWhenRowMissing() {
    // Given
    given(repository.findById(1L)).willReturn(Optional.empty());

    // When / Then
    assertThatThrownBy(() -> adapter.setVersion(AppVersion.of(1, 0)))
        .isInstanceOf(IllegalStateException.class);
  }
}
