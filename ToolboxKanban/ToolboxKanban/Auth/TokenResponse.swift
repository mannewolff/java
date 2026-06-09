//
//  TokenResponse.swift
//  ToolboxKanban
//
//  Antwort des Keycloak-Token-Endpoints (Authorization-Code- und Refresh-Grant).
//

import Foundation

struct TokenResponse: Equatable, Decodable {
  let accessToken: String
  let refreshToken: String?
  let idToken: String?
  /// Gültigkeit des Access-Tokens in Sekunden.
  let expiresIn: Int
  let tokenType: String
  let scope: String?

  enum CodingKeys: String, CodingKey {
    case accessToken = "access_token"
    case refreshToken = "refresh_token"
    case idToken = "id_token"
    case expiresIn = "expires_in"
    case tokenType = "token_type"
    case scope
  }
}
