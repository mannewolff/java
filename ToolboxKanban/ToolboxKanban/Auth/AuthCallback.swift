//
//  AuthCallback.swift
//  ToolboxKanban
//
//  Parst die Redirect-URL, die ASWebAuthenticationSession nach dem Login liefert,
//  und extrahiert den Authorization-Code — mit State-Prüfung (CSRF-Schutz).
//

import Foundation

enum AuthCallbackError: Error, Equatable {
  /// Keycloak meldete `error=...` zurück (z. B. `access_denied`).
  case serverError(String)
  /// `state` in der Callback-URL passt nicht zum erwarteten Wert.
  case stateMismatch
  /// Weder `code` noch `error` in der URL — unbrauchbarer Callback.
  case missingCode
}

enum AuthCallback {
  /// Extrahiert den Authorization-Code aus `url`, sofern `state` mit `expectedState`
  /// übereinstimmt. Wirft bei Server-Fehler, State-Mismatch oder fehlendem Code.
  static func extractCode(from url: URL, expectedState: String) throws -> String {
    let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
    let items = components?.queryItems ?? []
    func value(_ name: String) -> String? {
      items.first { $0.name == name }?.value
    }

    if let error = value("error") {
      throw AuthCallbackError.serverError(error)
    }
    guard value("state") == expectedState else {
      throw AuthCallbackError.stateMismatch
    }
    guard let code = value("code"), !code.isEmpty else {
      throw AuthCallbackError.missingCode
    }
    return code
  }
}
