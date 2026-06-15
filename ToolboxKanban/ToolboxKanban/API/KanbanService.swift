//
//  KanbanService.swift
//  ToolboxKanban
//
//  Lädt Kanban-Items vom Backend (#244). GET /api/kanban/items liefert die Items
//  nach Spalte gruppiert (Map<KanbanColumn, List<KanbanItemResponse>>); der Service
//  führt sie zu einer flachen, nach Erstelldatum absteigend sortierten Liste zusammen.
//

import Foundation

protocol KanbanServiceProtocol {
  /// Lädt alle (nicht archivierten) Items des angemeldeten Users.
  func fetchItems() async throws -> [KanbanItem]
}

final class KanbanService: KanbanServiceProtocol {
  private let baseURL: URL
  private let session: URLSession
  /// Liefert ein gültiges Access-Token (i. d. R. AuthState.validAccessToken).
  private let tokenProvider: () async -> String?

  init(
    baseURL: URL = APIConfig.development.baseURL,
    session: URLSession = .shared,
    tokenProvider: @escaping () async -> String?
  ) {
    self.baseURL = baseURL
    self.session = session
    self.tokenProvider = tokenProvider
  }

  func fetchItems() async throws -> [KanbanItem] {
    guard let token = await tokenProvider() else { throw APIError.unauthorized }

    var request = URLRequest(url: baseURL.appendingPathComponent("api/kanban/items"))
    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
    request.setValue("application/json", forHTTPHeaderField: "Accept")

    let data: Data
    let response: URLResponse
    do {
      (data, response) = try await session.data(for: request)
    } catch {
      throw APIError.transport
    }

    guard let http = response as? HTTPURLResponse else { throw APIError.transport }
    if http.statusCode == 401 { throw APIError.unauthorized }
    guard (200..<300).contains(http.statusCode) else {
      throw APIError.httpStatus(http.statusCode)
    }

    do {
      let grouped = try Self.decoder.decode([String: [KanbanItem]].self, from: data)
      return grouped.values.flatMap { $0 }.sorted { $0.createdAt > $1.createdAt }
    } catch {
      throw APIError.decoding
    }
  }

  /// Decoder mit ISO-8601-Datumsstrategie (mit und ohne Sekundenbruchteile),
  /// passend zur Jackson-Serialisierung von java.time.Instant.
  static let decoder: JSONDecoder = {
    let decoder = JSONDecoder()
    let withFraction = ISO8601DateFormatter()
    withFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    let withoutFraction = ISO8601DateFormatter()
    withoutFraction.formatOptions = [.withInternetDateTime]
    decoder.dateDecodingStrategy = .custom { decoder in
      let container = try decoder.singleValueContainer()
      let string = try container.decode(String.self)
      if let date = withFraction.date(from: string) ?? withoutFraction.date(from: string) {
        return date
      }
      throw DecodingError.dataCorruptedError(
        in: container, debugDescription: "Ungültiges ISO-8601-Datum: \(string)")
    }
    return decoder
  }()
}
