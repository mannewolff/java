//
//  Data+Base64URL.swift
//  ToolboxKanban
//
//  Base64URL-Kodierung ohne Padding (RFC 4648 §5) — für PKCE und JWT-Handling.
//

import Foundation

extension Data {
  /// Kodiert die Bytes als base64url **ohne** `=`-Padding:
  /// `+` → `-`, `/` → `_`, abschließende `=` entfernt.
  func base64URLEncodedString() -> String {
    base64EncodedString()
      .replacingOccurrences(of: "+", with: "-")
      .replacingOccurrences(of: "/", with: "_")
      .replacingOccurrences(of: "=", with: "")
  }
}
