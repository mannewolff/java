//
//  OIDCConfig.swift
//  ToolboxKanban
//
//  Statische OpenID-Connect-Konfiguration für den Keycloak-Client `toolbox-ios`.
//  Endpunkte werden aus dem Issuer abgeleitet (Keycloak-Standardpfade).
//

import Foundation

struct OIDCConfig: Equatable {
  /// Realm-Issuer, z. B. `http://localhost:8081/realms/toolbox-dev`.
  let issuer: URL
  /// Public-Client-ID in Keycloak.
  let clientID: String
  /// Custom-URL-Scheme-Redirect der App.
  let redirectURI: String
  /// Angeforderte Scopes (inkl. `offline_access` für „angemeldet bleiben", #243).
  let scopes: [String]

  var authorizationEndpoint: URL {
    issuer.appendingPathComponent("protocol/openid-connect/auth")
  }
  var tokenEndpoint: URL {
    issuer.appendingPathComponent("protocol/openid-connect/token")
  }
  var logoutEndpoint: URL {
    issuer.appendingPathComponent("protocol/openid-connect/logout")
  }

  /// Das Scheme-Präfix der Redirect-URI — von ASWebAuthenticationSession benötigt.
  var redirectScheme: String {
    String(redirectURI.prefix { $0 != ":" })
  }

  /// Lokale Entwicklungs-Konfiguration (Keycloak-Container auf :8081, Realm toolbox-dev).
  static let development = OIDCConfig(
    issuer: URL(string: "http://localhost:8081/realms/toolbox-dev")!,
    clientID: "toolbox-ios",
    redirectURI: "org.mwolff.toolboxkanban://callback",
    scopes: ["openid", "profile", "email", "offline_access"]
  )

  /// Produktions-Konfiguration: Keycloak hinter HTTPS (Realm `toolbox`, #263).
  /// Issuer-Host gemäß infra/keycloak/README (Reverse-Proxy auf :8081).
  static let production = OIDCConfig(
    issuer: URL(string: "https://toolboxauth.mwolff.org/realms/toolbox")!,
    clientID: "toolbox-ios",
    redirectURI: "org.mwolff.toolboxkanban://callback",
    scopes: ["openid", "profile", "email", "offline_access"]
  )

  /// Build-abhängige Auswahl: Debug-Builds nutzen Dev (localhost-HTTP), Release-Builds
  /// erzwingen die Prod-Config über HTTPS. So landet niemals eine HTTP-Issuer-URL im Release.
  static var current: OIDCConfig {
    #if DEBUG
      return .development
    #else
      return .production
    #endif
  }
}
