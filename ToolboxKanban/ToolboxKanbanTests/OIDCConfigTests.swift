//
//  OIDCConfigTests.swift
//  ToolboxKanbanTests
//
//  Tests für #263: Dev/Prod-OIDC-Konfiguration, HTTPS im Prod-Pfad.
//

import Foundation
import Testing

@testable import ToolboxKanban

struct OIDCConfigTests {

  @Test func productionUsesHTTPS() {
    #expect(OIDCConfig.production.issuer.scheme == "https")
    #expect(OIDCConfig.production.tokenEndpoint.absoluteString.hasPrefix("https://"))
    #expect(OIDCConfig.production.authorizationEndpoint.absoluteString.hasPrefix("https://"))
  }

  @Test func productionTargetsProdRealm() {
    #expect(OIDCConfig.production.issuer.absoluteString == "https://toolboxauth.mwolff.org/realms/toolbox")
    #expect(OIDCConfig.production.clientID == "toolbox-ios")
    #expect(OIDCConfig.production.redirectURI == "org.mwolff.toolboxkanban://callback")
  }

  @Test func developmentUsesLocalhost() {
    #expect(OIDCConfig.development.issuer.scheme == "http")
    #expect(OIDCConfig.development.issuer.host == "localhost")
  }

  @Test func currentMatchesDevelopmentInDebugBuilds() {
    // Tests laufen in der Debug-Konfiguration → current == development.
    #expect(OIDCConfig.current == .development)
  }

  @Test func bothConfigsRequestOfflineAccess() {
    #expect(OIDCConfig.production.scopes.contains("offline_access"))
    #expect(OIDCConfig.development.scopes.contains("offline_access"))
  }
}
