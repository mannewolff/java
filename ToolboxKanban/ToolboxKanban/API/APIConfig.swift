//
//  APIConfig.swift
//  ToolboxKanban
//
//  Basis-Konfiguration für die Toolbox-REST-API.
//

import Foundation

struct APIConfig: Equatable {
  /// Basis-URL des Spring-Boot-Backends (ohne abschließenden Pfad).
  let baseURL: URL

  /// Lokale Entwicklung: Spring Boot auf :8080. Der iOS-Simulator erreicht den
  /// Host-Mac über `localhost`.
  static let development = APIConfig(baseURL: URL(string: "http://localhost:8080")!)
}
