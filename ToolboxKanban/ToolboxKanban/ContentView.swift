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
    if authState.isAuthenticated {
      TicketsPlaceholderView()
    } else {
      LoginView()
    }
  }
}

/// Platzhalter für den Tickets-Screen (wird in #244 durch echten Screen ersetzt).
struct TicketsPlaceholderView: View {
  @Environment(AuthState.self) private var authState

  var body: some View {
    NavigationStack {
      VStack(spacing: 24) {
        Image(systemName: "checkmark.seal.fill")
          .font(.system(size: 64))
          .foregroundStyle(.green)
        Text("Angemeldet!")
          .font(.title.bold())
        Text("Kanban-Board folgt in #244.")
          .foregroundStyle(.secondary)
      }
      .navigationTitle("Toolbox Kanban")
      .toolbar {
        ToolbarItem(placement: .topBarTrailing) {
          Button("Abmelden") {
            authState.logout()
          }
        }
      }
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
}

private final class MockLoggedOutService: AuthServiceProtocol {
  func authenticate() async throws -> TokenResponse { throw AuthError.cancelled }
}
