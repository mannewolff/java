//
//  AuthServiceProtocol.swift
//  ToolboxKanban
//
//  Abstraktion über den OIDC-Login-Flow; ermöglicht austauschbare Implementierungen
//  (ASWebAuthenticationSession im App-Target, MockAuthService in Tests).
//

import Foundation

protocol AuthServiceProtocol {
  /// Führt den vollständigen Authorization-Code-Flow (PKCE) durch.
  /// Wirft `AuthError.cancelled`, wenn der User abbricht.
  func authenticate() async throws -> TokenResponse

  /// Erneuert die Tokens am Token-Endpoint mit einem Refresh-Token (#243).
  /// Wirft, wenn das Refresh-Token abgelaufen/ungültig ist.
  func refresh(refreshToken: String) async throws -> TokenResponse
}
