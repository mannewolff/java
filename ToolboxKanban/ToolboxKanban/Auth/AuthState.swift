//
//  AuthState.swift
//  ToolboxKanban
//
//  Zentraler ObservableObject-Ersatz (@Observable, iOS 17+) für den Authentifizierungszustand.
//  Wird per .environment(authState) in den View-Tree eingehängt.
//
//  #243: Tokens werden im TokenStore (Keychain) persistiert. Beim App-Start stellt
//  restore() die Session wieder her, validAccessToken() erneuert das Access-Token
//  automatisch vor Ablauf.
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
  /// `true` solange restore() beim App-Start läuft (verhindert kurzes Aufblitzen des Logins).
  private(set) var isRestoring = false

  private let authService: any AuthServiceProtocol
  private let tokenStore: any TokenStore
  private let now: () -> Date

  /// Aktuelles Refresh-Token (nur im Speicher gehalten, Persistenz über tokenStore).
  private var refreshToken: String?
  /// Ablaufzeitpunkt des aktuellen Access-Tokens.
  private var accessTokenExpiry: Date?

  init(
    authService: any AuthServiceProtocol,
    tokenStore: any TokenStore = KeychainTokenStore(),
    now: @escaping () -> Date = Date.init
  ) {
    self.authService = authService
    self.tokenStore = tokenStore
    self.now = now
  }

  /// Startet den OIDC-Login-Flow. Cancellation wird stillschweigend ignoriert.
  func login() async {
    isLoading = true
    defer { isLoading = false }
    do {
      let tokens = try await authService.authenticate()
      apply(tokens)
      loginError = nil
    } catch AuthError.cancelled {
      // Kein Fehler — User hat das Fenster geschlossen, kein Alert anzeigen.
    } catch {
      loginError = error
    }
  }

  /// Stellt beim App-Start eine gespeicherte Session wieder her ("angemeldet bleiben").
  /// Gültiges Access-Token → sofort angemeldet; abgelaufen aber Refresh-Token vorhanden →
  /// stiller Refresh; nicht erneuerbar → Store wird geleert, User landet im Login.
  func restore() async {
    isRestoring = true
    defer { isRestoring = false }
    guard let stored = tokenStore.load() else { return }
    if stored.isAccessTokenValid(now: now()) {
      accessToken = stored.accessToken
      refreshToken = stored.refreshToken
      accessTokenExpiry = stored.accessTokenExpiry
      isAuthenticated = true
      return
    }
    guard let token = stored.refreshToken else {
      tokenStore.clear()
      return
    }
    do {
      let tokens = try await authService.refresh(refreshToken: token)
      apply(tokens)
    } catch {
      // Refresh-Token abgelaufen/ungültig — saubere Abmeldung.
      clearSession()
    }
  }

  /// Liefert ein gültiges Access-Token für API-Aufrufe (#244ff) und erneuert es
  /// bei Bedarf automatisch vor Ablauf. `nil`, wenn keine Session (mehr) besteht.
  func validAccessToken() async -> String? {
    guard isAuthenticated else { return nil }
    if let expiry = accessTokenExpiry, now().addingTimeInterval(30) < expiry {
      return accessToken
    }
    guard let token = refreshToken else { return accessToken }
    do {
      let tokens = try await authService.refresh(refreshToken: token)
      apply(tokens)
      return accessToken
    } catch {
      clearSession()
      return nil
    }
  }

  /// Löscht den lokalen Authentifizierungszustand und die persistierten Tokens.
  func logout() {
    clearSession()
    loginError = nil
  }

  /// Übernimmt ein frisches TokenResponse in den Zustand und persistiert es.
  private func apply(_ tokens: TokenResponse) {
    accessToken = tokens.accessToken
    refreshToken = tokens.refreshToken
    let expiry = now().addingTimeInterval(TimeInterval(tokens.expiresIn))
    accessTokenExpiry = expiry
    isAuthenticated = true
    try? tokenStore.save(
      StoredTokens(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        accessTokenExpiry: expiry))
  }

  private func clearSession() {
    isAuthenticated = false
    accessToken = nil
    refreshToken = nil
    accessTokenExpiry = nil
    tokenStore.clear()
  }
}
