package org.mwolff.api.image.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mwolff.api.image.domain.StoredImage;

@ExtendWith(MockitoExtension.class)
class JpaStoredImageAdapterTest {

  @Mock private StoredImageJpaRepository repository;

  private static StoredImageEntity entity(
      final Long id, final String type, final byte[] data, final Instant createdAt) {
    final StoredImageEntity e = new StoredImageEntity();
    e.setId(id);
    e.setContentType(type);
    e.setSizeBytes(data.length);
    e.setData(data);
    e.setCreatedAt(createdAt);
    return e;
  }

  @Test
  void saveMapsDomainToEntityAndBack() {
    final JpaStoredImageAdapter adapter = new JpaStoredImageAdapter(repository);
    final Instant now = Instant.parse("2026-06-01T10:00:00Z");
    final StoredImageEntity persisted = entity(7L, "image/png", new byte[] {1, 2, 3}, now);
    final ArgumentCaptor<StoredImageEntity> captor =
        ArgumentCaptor.forClass(StoredImageEntity.class);
    when(repository.save(captor.capture())).thenReturn(persisted);

    final StoredImage result = adapter.save(StoredImage.of("image/png", new byte[] {1, 2, 3}));

    final StoredImageEntity sent = captor.getValue();
    assertThat(sent.getContentType()).isEqualTo("image/png");
    assertThat(sent.getSizeBytes()).isEqualTo(3);
    assertThat(sent.getData()).containsExactly(1, 2, 3);

    assertThat(result.id()).isEqualTo(7L);
    assertThat(result.contentType()).isEqualTo("image/png");
    assertThat(result.sizeBytes()).isEqualTo(3);
    assertThat(result.data()).containsExactly(1, 2, 3);
    assertThat(result.createdAt()).isEqualTo(now);
  }

  @Test
  void findByIdMapsPresentEntity() {
    final JpaStoredImageAdapter adapter = new JpaStoredImageAdapter(repository);
    when(repository.findById(7L))
        .thenReturn(Optional.of(entity(7L, "image/webp", new byte[] {9}, null)));

    final Optional<StoredImage> result = adapter.findById(7L);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(7L);
    assertThat(result.get().contentType()).isEqualTo("image/webp");
    assertThat(result.get().data()).containsExactly(9);
  }

  @Test
  void findByIdReturnsEmptyWhenMissing() {
    final JpaStoredImageAdapter adapter = new JpaStoredImageAdapter(repository);
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThat(adapter.findById(99L)).isEmpty();
  }
}
