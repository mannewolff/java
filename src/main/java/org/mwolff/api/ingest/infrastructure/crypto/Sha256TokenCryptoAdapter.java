package org.mwolff.api.ingest.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

import org.mwolff.api.ingest.domain.TokenCryptoPort;
import org.springframework.stereotype.Component;

/**
 * Plaintext-Format: {@code tk_<64-hex>} (32 Byte CSPRNG, hex-encoded).
 *
 * <p>Hashing via SHA-256 — deterministisch fuer DB-Lookup. Die Brute-Force-Sicherheit kommt aus der
 * Token-Entropie (256 bit), nicht aus einer langsamen Hash-Funktion.
 */
@Component
public class Sha256TokenCryptoAdapter implements TokenCryptoPort {

  private static final String PREFIX = "tk_";
  private static final int RANDOM_BYTES = 32;

  private final SecureRandom random = new SecureRandom();

  @Override
  public String generatePlaintext() {
    final byte[] bytes = new byte[RANDOM_BYTES];
    random.nextBytes(bytes);
    return PREFIX + HexFormat.of().formatHex(bytes);
  }

  @Override
  public String hash(String plaintext) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashed = digest.digest(plaintext.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 ist in jeder JDK-Distribution Pflicht — sollte nie auftreten.
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
