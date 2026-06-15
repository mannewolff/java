//
//  APIError.swift
//  ToolboxKanban
//
//  Fehlertypen der REST-Schicht.
//

import Foundation

enum APIError: Error, Equatable {
  /// Kein gültiges Access-Token vorhanden oder Backend liefert 401 → Re-Login nötig.
  case unauthorized
  /// Non-2xx-HTTP-Status (außer 401).
  case httpStatus(Int)
  /// Netzwerk-/Transportfehler (kein HTTP-Response erhalten).
  case transport
  /// Antwort konnte nicht dekodiert werden.
  case decoding

  /// Nutzerfreundliche Meldung für die UI.
  var userMessage: String {
    switch self {
    case .unauthorized:
      return "Sitzung abgelaufen. Bitte erneut anmelden."
    case .httpStatus(let code):
      return "Serverfehler (\(code)). Bitte später erneut versuchen."
    case .transport:
      return "Keine Verbindung zum Server. Bitte Netzwerk prüfen."
    case .decoding:
      return "Unerwartete Antwort vom Server."
    }
  }
}
