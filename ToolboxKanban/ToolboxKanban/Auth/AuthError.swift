//
//  AuthError.swift
//  ToolboxKanban
//
//  Fehler auf der Orchestrierungs-Ebene (ASWebAuthSessionService).
//  Tiefere Schichten haben eigene Fehlertypen: AuthCallbackError, TokenExchangeError.
//

import Foundation

enum AuthError: Error, Equatable {
  /// User hat das Login-Browserfenster geschlossen.
  case cancelled
  /// Session beendet, aber keine Callback-URL erhalten.
  case missingCallback
}
