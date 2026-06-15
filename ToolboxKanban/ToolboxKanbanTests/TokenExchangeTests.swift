//
//  TokenExchangeTests.swift
//  ToolboxKanbanTests
//

import Foundation
import Testing

@testable import ToolboxKanban

// @Suite(.serialized) verhindert parallele Ausführung: MockURLProtocol-State
// (handler, lastRequestBody) ist shared mutable static — bei parallelem Lauf
// überschreiben sich die Tests gegenseitig.
@Suite(.serialized)
struct TokenExchangeTests {

  private let tokenJSON = """
    {
      "access_token": "ACCESS-Tok",
      "refresh_token": "REFRESH-Tok",
      "id_token": "ID-Tok",
      "expires_in": 300,
      "token_type": "Bearer",
      "scope": "openid profile email offline_access"
    }
    """

  @Test func decodesTokenResponseWithAllFields() throws {
    let data = Data(tokenJSON.utf8)
    let token = try JSONDecoder().decode(TokenResponse.self, from: data)
    #expect(token.accessToken == "ACCESS-Tok")
    #expect(token.refreshToken == "REFRESH-Tok")
    #expect(token.expiresIn == 300)
    #expect(token.tokenType == "Bearer")
  }

  @Test func decodesTokenResponseWithoutRefreshToken() throws {
    let json = #"{"access_token":"a","expires_in":60,"token_type":"Bearer"}"#
    let token = try JSONDecoder().decode(TokenResponse.self, from: Data(json.utf8))
    #expect(token.refreshToken == nil)
    #expect(token.accessToken == "a")
  }

  @Test func encodeFormPercentEncodesAndSortsKeys() {
    let encoded = TokenExchange.encodeForm(["b": "2", "a": "x y", "c": "p+q"])
    #expect(encoded == "a=x%20y&b=2&c=p%2Bq")
  }

  @Test func exchangePostsCodeAndVerifierAndReturnsTokens() async throws {
    MockURLProtocol.lastRequestBody = nil
    MockURLProtocol.handler = { _ in (200, Data(self.tokenJSON.utf8)) }
    let exchange = TokenExchange(config: .development, session: MockURLProtocol.makeSession())

    let token = try await exchange.exchange(code: "the-code", codeVerifier: "the-verifier")

    #expect(token.accessToken == "ACCESS-Tok")
    let body = MockURLProtocol.lastRequestBody ?? ""
    #expect(body.contains("grant_type=authorization_code"))
    #expect(body.contains("code=the-code"))
    #expect(body.contains("code_verifier=the-verifier"))
    #expect(body.contains("client_id=toolbox-ios"))
  }

  @Test func exchangeThrowsHTTPStatusOnError() async {
    MockURLProtocol.lastRequestBody = nil
    MockURLProtocol.handler = { _ in (400, Data(#"{"error":"invalid_grant"}"#.utf8)) }
    let exchange = TokenExchange(config: .development, session: MockURLProtocol.makeSession())

    await #expect(throws: TokenExchangeError.httpStatus(400)) {
      try await exchange.exchange(code: "x", codeVerifier: "y")
    }
  }

  @Test func refreshPostsRefreshGrant() async throws {
    MockURLProtocol.lastRequestBody = nil
    MockURLProtocol.handler = { _ in (200, Data(self.tokenJSON.utf8)) }
    let exchange = TokenExchange(config: .development, session: MockURLProtocol.makeSession())

    _ = try await exchange.refresh(refreshToken: "my-refresh")

    let body = MockURLProtocol.lastRequestBody ?? ""
    #expect(body.contains("grant_type=refresh_token"))
    #expect(body.contains("refresh_token=my-refresh"))
  }
}
