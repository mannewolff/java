package org.mwolff.api.image.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mwolff.api.image.domain.ImageMetadata;
import org.mwolff.api.image.domain.StoredImage;
import org.springframework.data.domain.Pageable;

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

  @Test
  void findMetadataPassesOffsetAndLimitAndMapsViews() {
    final JpaStoredImageAdapter adapter = new JpaStoredImageAdapter(repository);
    final Instant now = Instant.parse("2026-06-01T10:00:00Z");
    final StoredImageMetadataView view = mock(StoredImageMetadataView.class);
    when(view.getId()).thenReturn(42L);
    when(view.getContentType()).thenReturn("image/png");
    when(view.getSizeBytes()).thenReturn(123);
    when(view.getCreatedAt()).thenReturn(now);
    final ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    when(repository.findAllByOrderByIdDesc(captor.capture())).thenReturn(List.of(view));

    final List<ImageMetadata> result = adapter.findMetadata(10, 20);

    final Pageable pageable = captor.getValue();
    assertThat(pageable.getOffset()).isEqualTo(20L);
    assertThat(pageable.getPageSize()).isEqualTo(10);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(42L);
    assertThat(result.get(0).contentType()).isEqualTo("image/png");
    assertThat(result.get(0).sizeBytes()).isEqualTo(123L);
    assertThat(result.get(0).createdAt()).isEqualTo(now);
    // getHash() der Projektion ist hier nicht gestubbt → null.
    assertThat(result.get(0).hash()).isNull();
  }

  @Test
  void countDelegatesToRepository() {
    final JpaStoredImageAdapter adapter = new JpaStoredImageAdapter(repository);
    when(repository.count()).thenReturn(7L);

    assertThat(adapter.count()).isEqualTo(7L);
  }

  @Test
  void findIdByHashReturnsFirstMatch() {
    final JpaStoredImageAdapter adapter = new JpaStoredImageAdapter(repository);
    when(repository.findIdsByHash("h")).thenReturn(List.of(3L, 9L));

    assertThat(adapter.findIdByHash("h")).contains(3L);
  }

  @Test
  void findIdByHashEmptyWhenNoMatch() {
    final JpaStoredImageAdapter adapter = new JpaStoredImageAdapter(repository);
    when(repository.findIdsByHash("h")).thenReturn(List.of());

    assertThat(adapter.findIdByHash("h")).isEmpty();
  }
}
