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
 * Verifiziert insbesondere die Metadaten-Projektion (ohne LONGBLOB), die Paginierung und die
 * Owner-Isolation (#230).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaStoredImageAdapter.class)
class JpaStoredImageAdapterIT extends AbstractIntegrationTest {

  private static final String USER_A = "user-a";
  private static final String USER_B = "user-b";

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
    final StoredImage saved = adapter.save(StoredImage.of(USER_A, "image/png", bytes(16)));

    assertThat(saved.id()).isNotNull();
    final StoredImage loaded = adapter.findByIdAndUserSub(saved.id(), USER_A).orElseThrow();
    assertThat(loaded.userSub()).isEqualTo(USER_A);
    assertThat(loaded.contentType()).isEqualTo("image/png");
    assertThat(loaded.sizeBytes()).isEqualTo(16L);
    assertThat(loaded.data()).hasSize(16);
  }

  @Test
  void findByIdAndUserSubDoesNotLeakForeignImage() {
    final StoredImage saved = adapter.save(StoredImage.of(USER_A, "image/png", bytes(8)));

    assertThat(adapter.findByIdAndUserSub(saved.id(), USER_B)).isEmpty();
  }

  @Test
  void findMetadataReturnsNewestFirstWithoutBinaryDataScopedToUser() {
    final StoredImage first = adapter.save(StoredImage.of(USER_A, "image/png", bytes(8)));
    final StoredImage second = adapter.save(StoredImage.of(USER_A, "image/webp", bytes(32)));
    adapter.save(StoredImage.of(USER_B, "image/png", bytes(4))); // fremdes Bild bleibt unsichtbar

    final List<ImageMetadata> all = adapter.findMetadataByUserSub(USER_A, 10, 0);

    assertThat(all).extracting(ImageMetadata::id).containsExactly(second.id(), first.id());
    final ImageMetadata newest = all.get(0);
    assertThat(newest.contentType()).isEqualTo("image/webp");
    assertThat(newest.sizeBytes()).isEqualTo(32L);
    assertThat(newest.createdAt()).isNotNull();
    // hash erst mit #199 befüllt.
    assertThat(newest.hash()).isNull();
  }

  @Test
  void findMetadataAppliesLimitAndOffsetAndCountIsPerUser() {
    final StoredImage a = adapter.save(StoredImage.of(USER_A, "image/png", bytes(4)));
    final StoredImage b = adapter.save(StoredImage.of(USER_A, "image/png", bytes(4)));
    final StoredImage c = adapter.save(StoredImage.of(USER_A, "image/png", bytes(4)));
    adapter.save(StoredImage.of(USER_B, "image/png", bytes(4))); // zählt nicht für USER_A

    // Absteigend nach id: [c, b, a]. limit=1, offset=1 → [b].
    final List<ImageMetadata> page = adapter.findMetadataByUserSub(USER_A, 1, 1);

    assertThat(page).extracting(ImageMetadata::id).containsExactly(b.id());
    assertThat(adapter.countByUserSub(USER_A)).isEqualTo(3L);
    assertThat(adapter.countByUserSub(USER_B)).isEqualTo(1L);
    assertThat(a.id()).isNotNull();
    assertThat(c.id()).isNotNull();
  }

  @Test
  void deleteRemovesImage() {
    final StoredImage saved = adapter.save(StoredImage.of(USER_A, "image/png", bytes(8)));
    assertThat(adapter.findByIdAndUserSub(saved.id(), USER_A)).isPresent();

    adapter.delete(saved.id());

    assertThat(adapter.findByIdAndUserSub(saved.id(), USER_A)).isEmpty();
  }

  @Test
  void hashIsPersistedAndFoundByHashPerUser() {
    final StoredImage saved =
        adapter.save(StoredImage.of(USER_A, "image/png", bytes(8), "cafebabe"));

    assertThat(saved.hash()).isEqualTo("cafebabe");
    assertThat(adapter.findByIdAndUserSub(saved.id(), USER_A).orElseThrow().hash())
        .isEqualTo("cafebabe");
    assertThat(adapter.findMetadataByUserSub(USER_A, 10, 0).get(0).hash()).isEqualTo("cafebabe");
    assertThat(adapter.findIdByHashAndUserSub("cafebabe", USER_A)).contains(saved.id());
    // Gleicher Hash, aber anderer User → kein Treffer (kein Existenz-Leak, #230).
    assertThat(adapter.findIdByHashAndUserSub("cafebabe", USER_B)).isEmpty();
    assertThat(adapter.findIdByHashAndUserSub("unknown", USER_A)).isEmpty();
  }
}
