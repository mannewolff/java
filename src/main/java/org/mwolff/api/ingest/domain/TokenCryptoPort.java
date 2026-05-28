package org.mwolff.api.ingest.domain;

/**
 * Strategy fuer Token-Erzeugung und -Hashing — Infrastruktur-Adapter liefert eine
 * SHA-256-Implementierung mit CSPRNG-Plaintext.
 *
 * <p>Wir verwenden bewusst keinen bcrypt-Hash: Bcrypt ist randomisiert (Salt) und damit fuer
 * Direct-Lookup-via-Hash ungeeignet. Bei API-Tokens kommt die Brute-Force-Sicherheit aus der
 * Entropie des Plaintexts (256 bit / 64 Hex-Zeichen), nicht aus dem Hashing-Verfahren.
 */
public interface TokenCryptoPort {

  /** Erzeugt einen frischen Plaintext-Token im Format {@code tk_<64-hex>}. */
  String generatePlaintext();

  /** Liefert den deterministischen SHA-256-Hash eines Plaintext-Tokens. */
  String hash(String plaintext);
}
