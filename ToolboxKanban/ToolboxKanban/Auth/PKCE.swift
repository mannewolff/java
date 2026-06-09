//
//  PKCE.swift
//  ToolboxKanban
//
//  Proof Key for Code Exchange (RFC 7636) für den OAuth-Authorization-Code-Flow.
//  Wird für den Login gegen den public Keycloak-Client `toolbox-ios` benötigt.
//

import CryptoKit
import Foundation

/// Ein PKCE-Paar aus `codeVerifier` (geheim, bleibt in der App) und
/// `codeChallenge` (wird an den Authorization-Endpoint gesendet).
struct PKCE: Equatable {
  /// Zufälliger High-Entropy-String, 43–128 Zeichen aus dem unreserved-Zeichensatz.
  let codeVerifier: String
  /// `BASE64URL(SHA256(ASCII(codeVerifier)))` ohne Padding.
  let codeChallenge: String
  /// Challenge-Methode für Keycloak — immer S256.
  let codeChallengeMethod = "S256"

  /// Erzeugt die Challenge deterministisch aus einem gegebenen Verifier.
  init(codeVerifier: String) {
    self.codeVerifier = codeVerifier
    let digest = SHA256.hash(data: Data(codeVerifier.utf8))
    self.codeChallenge = Data(digest).base64URLEncodedString()
  }

  /// Erzeugt ein frisches PKCE-Paar mit kryptografisch sicherem Zufalls-Verifier
  /// (32 Zufallsbytes → 43 Zeichen base64url).
  static func generate() -> PKCE {
    var bytes = [UInt8](repeating: 0, count: 32)
    let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
    precondition(status == errSecSuccess, "SecRandomCopyBytes fehlgeschlagen: \(status)")
    return PKCE(codeVerifier: Data(bytes).base64URLEncodedString())
  }
}
