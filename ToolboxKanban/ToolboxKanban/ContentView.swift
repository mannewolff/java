//
//  ContentView.swift
//  ToolboxKanban
//
//  Root-View: routet abhängig vom Authentifizierungszustand.
//

import SwiftUI

struct ContentView: View {
  @Environment(AuthState.self) private var authState

  var body: some View {
    if authState.isRestoring {
      ProgressView("Anmeldung wird wiederhergestellt…")
    } else if authState.isAuthenticated {
      TicketsView()
    } else {
      LoginView()
    }
  }
}

#Preview("Logged in") {
  ContentView()
    .environment(AuthState(authService: MockLoggedInService()))
}

#Preview("Logged out") {
  ContentView()
    .environment(AuthState(authService: MockLoggedOutService()))
}

private final class MockLoggedInService: AuthServiceProtocol {
  func authenticate() async throws -> TokenResponse {
    TokenResponse(
      accessToken: "preview-token", refreshToken: nil, idToken: nil,
      expiresIn: 300, tokenType: "Bearer", scope: "openid")
  }
  func refresh(refreshToken: String) async throws -> TokenResponse { try await authenticate() }
}

private final class MockLoggedOutService: AuthServiceProtocol {
  func authenticate() async throws -> TokenResponse { throw AuthError.cancelled }
  func refresh(refreshToken: String) async throws -> TokenResponse { throw AuthError.cancelled }
}
