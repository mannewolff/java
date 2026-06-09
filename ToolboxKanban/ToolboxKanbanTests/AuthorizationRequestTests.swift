//
//  AuthorizationRequestTests.swift
//  ToolboxKanbanTests
//

import Foundation
import Testing

@testable import ToolboxKanban

struct AuthorizationRequestTests {

  private func makeQuery() -> [String: String] {
    let request = AuthorizationRequest(
      config: .development,
      pkce: PKCE(codeVerifier: "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
      state: "xyz-state-123"
    )
    let components = URLComponents(url: request.url, resolvingAgainstBaseURL: false)!
    var dict: [String: String] = [:]
    for item in components.queryItems ?? [] { dict[item.name] = item.value }
    return dict
  }

  @Test func usesAuthorizationEndpointPath() {
    let request = AuthorizationRequest(
      config: .development, pkce: .generate(), state: "s")
    #expect(request.url.path == "/realms/toolbox-dev/protocol/openid-connect/auth")
  }

  @Test func requestsAuthorizationCodeFlow() {
    #expect(makeQuery()["response_type"] == "code")
  }

  @Test func sendsClientID() {
    #expect(makeQuery()["client_id"] == "toolbox-ios")
  }

  @Test func sendsRedirectURI() {
    #expect(makeQuery()["redirect_uri"] == "org.mwolff.toolboxkanban://callback")
  }

  @Test func sendsSpaceJoinedScopesIncludingOfflineAccess() {
    let scope = makeQuery()["scope"]
    #expect(scope == "openid profile email offline_access")
  }

  @Test func sendsPKCEChallengeWithS256Method() {
    let query = makeQuery()
    #expect(query["code_challenge"] == "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
    #expect(query["code_challenge_method"] == "S256")
  }

  @Test func sendsStateUnchanged() {
    #expect(makeQuery()["state"] == "xyz-state-123")
  }

  @Test func redirectSchemeIsExtractedFromRedirectURI() {
    #expect(OIDCConfig.development.redirectScheme == "org.mwolff.toolboxkanban")
  }
}
