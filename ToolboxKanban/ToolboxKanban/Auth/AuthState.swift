//
//  AuthState.swift
//  ToolboxKanban
//
//  Zentraler ObservableObject-Ersatz (@Observable, iOS 17+) für den Authentifizierungszustand.
//  Wird per .environment(authState) in den View-Tree eingehängt.
//

import Foundation
import Observation

@MainActor
@Observable
final class AuthState {
  /// `true` nach einem erfolgreichen Login-Flow.
  private(set) var isAuthenticated = false
  /// Das Access-Token aus der letzten erfolgreichen Authentifizierung.
  private(set) var accessToken: String? = nil
  /// Letzter Fehler aus `login()` — `nil` bei Cancellation (kein Alert nötig).
  private(set) var loginError: (any Error)? = nil
  /// `true` während `login()` läuft (Spinner in der LoginView).
  private(set) var isLoading = false

  private let authService: any AuthServiceProtocol

  init(authService: any AuthServiceProtocol) {
    self.authService = authService
  }

  /// Startet den OIDC-Login-Flow. Cancellation wird stillschweigend ignoriert.
  func login() async {
    isLoading = true
    defer { isLoading = false }
    do {
      let tokens = try await authService.authenticate()
      accessToken = tokens.accessToken
      isAuthenticated = true
      loginError = nil
    } catch AuthError.cancelled {
      // Kein Fehler — User hat das Fenster geschlossen, kein Alert anzeigen.
    } catch {
      loginError = error
    }
  }

  /// Löscht den lokalen Authentifizierungszustand (kein Server-Logout in #242).
  func logout() {
    isAuthenticated = false
    accessToken = nil
    loginError = nil
  }
}
