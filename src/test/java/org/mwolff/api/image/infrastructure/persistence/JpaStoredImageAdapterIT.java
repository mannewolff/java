package org.mwolff.api.image.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.image.domain.ImageMetadata;
import org.mwolff.api.image.domain.StoredImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Integrationstest des {@link JpaStoredImageAdapter} gegen MariaDB via Testcontainers (#198).
 * Verifiziert insbesondere die Metadaten-Projektion (ohne LONGBLOB) und die Paginierung.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaStoredImageAdapter.class)
class JpaStoredImageAdapterIT extends AbstractIntegrationTest {

  @Autowired private JpaStoredImageAdapter adapter;

  private static byte[] bytes(final int n) {
    final byte[] b = new byte[n];
    for (int i = 0; i < n; i++) {
      b[i] = (byte) (i % 7);
    }
    return b;
  }

  @Test
  void saveAndFindByIdRoundTrip() {
    final StoredImage saved = adapter.save(StoredImage.of("image/png", bytes(16)));

    assertThat(saved.id()).isNotNull();
    final StoredImage loaded = adapter.findById(saved.id()).orElseThrow();
    assertThat(loaded.contentType()).isEqualTo("image/png");
    assertThat(loaded.sizeBytes()).isEqualTo(16L);
    assertThat(loaded.data()).hasSize(16);
  }

  @Test
  void findMetadataReturnsNewestFirstWithoutBinaryData() {
    final StoredImage first = adapter.save(StoredImage.of("image/png", bytes(8)));
    final StoredImage second = adapter.save(StoredImage.of("image/webp", bytes(32)));

    final List<ImageMetadata> all = adapter.findMetadata(10, 0);

    assertThat(all).extracting(ImageMetadata::id).containsExactly(second.id(), first.id());
    final ImageMetadata newest = all.get(0);
    assertThat(newest.contentType()).isEqualTo("image/webp");
    assertThat(newest.sizeBytes()).isEqualTo(32L);
    assertThat(newest.createdAt()).isNotNull();
    // hash erst mit #199 befüllt.
    assertThat(newest.hash()).isNull();
  }

  @Test
  void findMetadataAppliesLimitAndOffset() {
    final StoredImage a = adapter.save(StoredImage.of("image/png", bytes(4)));
    final StoredImage b = adapter.save(StoredImage.of("image/png", bytes(4)));
    final StoredImage c = adapter.save(StoredImage.of("image/png", bytes(4)));

    // Absteigend nach id: [c, b, a]. limit=1, offset=1 → [b].
    final List<ImageMetadata> page = adapter.findMetadata(1, 1);

    assertThat(page).extracting(ImageMetadata::id).containsExactly(b.id());
    assertThat(adapter.count()).isEqualTo(3L);
    assertThat(a.id()).isNotNull();
    assertThat(c.id()).isNotNull();
  }
}
