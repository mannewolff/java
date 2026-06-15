//
//  MockAuthService.swift
//  ToolboxKanbanTests
//
//  Stub-Implementierung von AuthServiceProtocol für Unit-Tests.
//

import Foundation
@testable import ToolboxKanban

final class MockAuthService: AuthServiceProtocol {
  private let result: Result<TokenResponse, Error>
  private let refreshResult: Result<TokenResponse, Error>
  /// Zählt die `refresh(refreshToken:)`-Aufrufe für Verhaltensprüfungen.
  private(set) var refreshCallCount = 0
  /// Zuletzt an `refresh` übergebenes Token.
  private(set) var lastRefreshToken: String?

  init(
    result: Result<TokenResponse, Error> = .failure(AuthError.cancelled),
    refreshResult: Result<TokenResponse, Error> = .failure(AuthError.cancelled)
  ) {
    self.result = result
    self.refreshResult = refreshResult
  }

  func authenticate() async throws -> TokenResponse {
    switch result {
    case .success(let token): return token
    case .failure(let error): throw error
    }
  }

  func refresh(refreshToken: String) async throws -> TokenResponse {
    refreshCallCount += 1
    lastRefreshToken = refreshToken
    switch refreshResult {
    case .success(let token): return token
    case .failure(let error): throw error
    }
  }
}

/// In-Memory-TokenStore für deterministische Persistenz-Tests (keine Keychain).
final class MockTokenStore: TokenStore {
  private(set) var stored: StoredTokens?
  private(set) var clearCallCount = 0

  init(stored: StoredTokens? = nil) {
    self.stored = stored
  }

  func save(_ tokens: StoredTokens) throws { stored = tokens }
  func load() -> StoredTokens? { stored }
  func clear() {
    stored = nil
    clearCallCount += 1
  }
}

/// Vorgefertigtes `TokenResponse`-Fixture für Tests.
extension TokenResponse {
  static let fixture = TokenResponse(
    accessToken: "test-access-token",
    refreshToken: "test-refresh-token",
    idToken: "test-id-token",
    expiresIn: 300,
    tokenType: "Bearer",
    scope: "openid offline_access"
  )

  /// Variante mit abweichendem Access-Token, um Refresh-Ergebnisse zu unterscheiden.
  static let refreshed = TokenResponse(
    accessToken: "refreshed-access-token",
    refreshToken: "refreshed-refresh-token",
    idToken: "test-id-token",
    expiresIn: 300,
    tokenType: "Bearer",
    scope: "openid offline_access"
  )
}
