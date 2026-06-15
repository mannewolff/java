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

  init(result: Result<TokenResponse, Error> = .failure(AuthError.cancelled)) {
    self.result = result
  }

  func authenticate() async throws -> TokenResponse {
    switch result {
    case .success(let token): return token
    case .failure(let error): throw error
    }
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
}
