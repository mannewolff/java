//
//  ASWebAuthSessionService.swift
//  ToolboxKanban
//
//  Produktions-Implementierung von AuthServiceProtocol:
//  öffnet ASWebAuthenticationSession → Keycloak → PKCE-Code-Exchange.
//

import AuthenticationServices
import Foundation

@MainActor
final class ASWebAuthSessionService: NSObject, AuthServiceProtocol {
  private let config: OIDCConfig
  /// Hält die laufende Session im Speicher, damit sie nicht vorzeitig dealloziiert wird.
  private var activeSession: ASWebAuthenticationSession?

  init(config: OIDCConfig = .development) {
    self.config = config
  }

  func authenticate() async throws -> TokenResponse {
    let pkce = PKCE.generate()
    let state = UUID().uuidString
    let request = AuthorizationRequest(config: config, pkce: pkce, state: state)
    let callbackURL = try await launchWebSession(authURL: request.url, scheme: config.redirectScheme)
    let code = try AuthCallback.extractCode(from: callbackURL, expectedState: state)
    return try await TokenExchange(config: config).exchange(code: code, codeVerifier: pkce.codeVerifier)
  }

  private func launchWebSession(authURL: URL, scheme: String) async throws -> URL {
    try await withCheckedThrowingContinuation { continuation in
      let session = ASWebAuthenticationSession(
        url: authURL,
        callbackURLScheme: scheme
      ) { [weak self] callbackURL, error in
        self?.activeSession = nil
        if let sessionError = error as? ASWebAuthenticationSessionError,
          sessionError.code == .canceledLogin
        {
          continuation.resume(throwing: AuthError.cancelled)
        } else if let error {
          continuation.resume(throwing: error)
        } else if let callbackURL {
          continuation.resume(returning: callbackURL)
        } else {
          continuation.resume(throwing: AuthError.missingCallback)
        }
      }
      session.presentationContextProvider = self
      // false = geteilte Safari-Session (SSO), true = privat (kein Cookie-Sharing).
      session.prefersEphemeralWebBrowserSession = false
      activeSession = session
      session.start()
    }
  }
}

extension ASWebAuthSessionService: ASWebAuthenticationPresentationContextProviding {
  func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
    UIApplication.shared.connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .first?.keyWindow ?? ASPresentationAnchor()
  }
}
