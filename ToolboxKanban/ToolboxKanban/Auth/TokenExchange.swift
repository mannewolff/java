//
//  TokenExchange.swift
//  ToolboxKanban
//
//  Tauscht den Authorization-Code (bzw. später ein Refresh-Token) am
//  Keycloak-Token-Endpoint gegen ein TokenResponse. Public Client → kein Secret.
//

import Foundation

enum TokenExchangeError: Error, Equatable {
  /// Non-2xx-HTTP-Status vom Token-Endpoint.
  case httpStatus(Int)
  /// Antwort konnte nicht als TokenResponse dekodiert werden.
  case decoding
}

struct TokenExchange {
  let config: OIDCConfig
  let session: URLSession

  init(config: OIDCConfig, session: URLSession = .shared) {
    self.config = config
    self.session = session
  }

  /// Authorization-Code-Grant: tauscht `code` + `codeVerifier` gegen Tokens.
  func exchange(code: String, codeVerifier: String) async throws -> TokenResponse {
    try await post(form: [
      "grant_type": "authorization_code",
      "code": code,
      "redirect_uri": config.redirectURI,
      "client_id": config.clientID,
      "code_verifier": codeVerifier,
    ])
  }

  /// Refresh-Grant: erneuert die Tokens mit einem Refresh-Token (#243).
  func refresh(refreshToken: String) async throws -> TokenResponse {
    try await post(form: [
      "grant_type": "refresh_token",
      "refresh_token": refreshToken,
      "client_id": config.clientID,
    ])
  }

  private func post(form: [String: String]) async throws -> TokenResponse {
    var request = URLRequest(url: config.tokenEndpoint)
    request.httpMethod = "POST"
    request.setValue(
      "application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
    request.httpBody = Self.encodeForm(form).data(using: .utf8)

    let (data, response) = try await session.data(for: request)
    if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
      throw TokenExchangeError.httpStatus(http.statusCode)
    }
    do {
      return try JSONDecoder().decode(TokenResponse.self, from: data)
    } catch {
      throw TokenExchangeError.decoding
    }
  }

  /// x-www-form-urlencoded-Kodierung mit stabiler (sortierter) Reihenfolge.
  static func encodeForm(_ form: [String: String]) -> String {
    var allowed = CharacterSet.alphanumerics
    allowed.insert(charactersIn: "-._~")
    return form.keys.sorted().map { key in
      let value = form[key] ?? ""
      let encKey = key.addingPercentEncoding(withAllowedCharacters: allowed) ?? key
      let encValue = value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
      return "\(encKey)=\(encValue)"
    }.joined(separator: "&")
  }
}
