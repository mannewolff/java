package org.mwolff.api.common.token;

/**
 * Strategy fuer Token-Erzeugung und -Hashing — Infrastruktur-Adapter liefert eine
 * SHA-256-Implementierung mit CSPRNG-Plaintext.
 *
 * <p>Wir verwenden bewusst keinen bcrypt-Hash: Bcrypt ist randomisiert (Salt) und damit fuer
 * Direct-Lookup-via-Hash ungeeignet. Bei API-Tokens kommt die Brute-Force-Sicherheit aus der
 * Entropie des Plaintexts (256 bit / 64 Hex-Zeichen), nicht aus dem Hashing-Verfahren.
 *
 * <p>Liegt bewusst im modul-neutralen {@code common.token}-Paket, weil mehrere Feature-Module
 * (Ingest, Kanban) denselben PAT-Hash-Mechanismus brauchen (Issue #362).
 */
public interface TokenCryptoPort {

  /** Erzeugt einen frischen Plaintext-Token im Format {@code tk_<64-hex>}. */
  String generatePlaintext();

  /** Liefert den deterministischen SHA-256-Hash eines Plaintext-Tokens. */
  String hash(String plaintext);
}
