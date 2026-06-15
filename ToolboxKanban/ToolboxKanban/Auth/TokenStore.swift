//
//  TokenStore.swift
//  ToolboxKanban
//
//  Persistenz der OIDC-Tokens (#243). Der Store-Vertrag ist abstrahiert, damit
//  AuthState in Tests gegen einen In-Memory-Store laufen kann; im App-Target
//  speichert KeychainTokenStore in die iOS-Keychain.
//

import Foundation

/// Persistierte Tokens samt Ablaufzeitpunkt des Access-Tokens.
struct StoredTokens: Codable, Equatable {
  let accessToken: String
  let refreshToken: String?
  /// Absoluter Ablaufzeitpunkt des Access-Tokens (aus `expires_in` berechnet).
  let accessTokenExpiry: Date

  /// `true`, wenn das Access-Token zum Zeitpunkt `now` (inkl. Sicherheitspuffer) noch gültig ist.
  func isAccessTokenValid(now: Date, buffer: TimeInterval = 30) -> Bool {
    now.addingTimeInterval(buffer) < accessTokenExpiry
  }
}

/// Abstraktion über die Token-Persistenz (Keychain im App-Target, Mock in Tests).
protocol TokenStore {
  func save(_ tokens: StoredTokens) throws
  func load() -> StoredTokens?
  func clear()
}

/// Keychain-basierter TokenStore. Speichert die Tokens als ein JSON-kodiertes
/// Generic-Password-Item, nur lesbar wenn das Gerät entsperrt war (nach Erst-Unlock).
final class KeychainTokenStore: TokenStore {
  private let service: String
  private let account: String

  init(service: String = "org.mwolff.toolboxkanban.tokens", account: String = "oidc") {
    self.service = service
    self.account = account
  }

  func save(_ tokens: StoredTokens) throws {
    let data = try JSONEncoder().encode(tokens)
    // Vorhandenes Item entfernen, dann frisch anlegen (idempotentes Upsert).
    SecItemDelete(baseQuery() as CFDictionary)
    var attributes = baseQuery()
    attributes[kSecValueData as String] = data
    attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
    let status = SecItemAdd(attributes as CFDictionary, nil)
    guard status == errSecSuccess else {
      throw KeychainError.unexpectedStatus(status)
    }
  }

  func load() -> StoredTokens? {
    var query = baseQuery()
    query[kSecReturnData as String] = true
    query[kSecMatchLimit as String] = kSecMatchLimitOne
    var item: CFTypeRef?
    let status = SecItemCopyMatching(query as CFDictionary, &item)
    guard status == errSecSuccess, let data = item as? Data else {
      return nil
    }
    return try? JSONDecoder().decode(StoredTokens.self, from: data)
  }

  func clear() {
    SecItemDelete(baseQuery() as CFDictionary)
  }

  private func baseQuery() -> [String: Any] {
    [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: service,
      kSecAttrAccount as String: account,
    ]
  }
}

enum KeychainError: Error, Equatable {
  case unexpectedStatus(OSStatus)
}
