//
//  AuthorizationRequest.swift
//  ToolboxKanban
//
//  Baut die Keycloak-Authorization-URL für den Authorization-Code-Flow mit PKCE.
//

import Foundation

struct AuthorizationRequest {
  let config: OIDCConfig
  let pkce: PKCE
  /// CSRF-Schutz; muss in der Callback-URL unverändert zurückkommen.
  let state: String

  /// Vollständige Authorize-URL inkl. PKCE-Challenge.
  var url: URL {
    var components = URLComponents(
      url: config.authorizationEndpoint, resolvingAgainstBaseURL: false)!
    components.queryItems = [
      URLQueryItem(name: "response_type", value: "code"),
      URLQueryItem(name: "client_id", value: config.clientID),
      URLQueryItem(name: "redirect_uri", value: config.redirectURI),
      URLQueryItem(name: "scope", value: config.scopes.joined(separator: " ")),
      URLQueryItem(name: "state", value: state),
      URLQueryItem(name: "code_challenge", value: pkce.codeChallenge),
      URLQueryItem(name: "code_challenge_method", value: pkce.codeChallengeMethod),
    ]
    return components.url!
  }
}
