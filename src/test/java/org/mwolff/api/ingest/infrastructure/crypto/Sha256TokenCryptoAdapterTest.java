package org.mwolff.api.ingest.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256TokenCryptoAdapterTest {

  private final Sha256TokenCryptoAdapter adapter = new Sha256TokenCryptoAdapter();

  @Test
  void generatedPlaintextHasTkPrefixAnd64HexChars() {
    final String plaintext = adapter.generatePlaintext();

    assertThat(plaintext).startsWith("tk_");
    assertThat(plaintext).hasSize(3 + 64);
    assertThat(plaintext.substring(3)).matches("[0-9a-f]{64}");
  }

  @Test
  void generatedPlaintextIsUnique() {
    final String a = adapter.generatePlaintext();
    final String b = adapter.generatePlaintext();

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void hashIsDeterministic() {
    final String hash1 = adapter.hash("tk_abc");
    final String hash2 = adapter.hash("tk_abc");

    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  void hashHas64HexChars() {
    final String hash = adapter.hash("tk_anything");

    assertThat(hash).matches("[0-9a-f]{64}");
  }

  @Test
  void differentInputsProduceDifferentHashes() {
    assertThat(adapter.hash("tk_a")).isNotEqualTo(adapter.hash("tk_b"));
  }
}
