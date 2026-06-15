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

  /// Legt ein neues Item an (#246) und liefert das erzeugte Item zurück.
  func createItem(title: String, body: String, column: KanbanColumn) async throws -> KanbanItem
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
    let request = try await authorizedRequest(path: "api/kanban/items", method: "GET")
    let data = try await send(request)
    do {
      let grouped = try Self.decoder.decode([String: [KanbanItem]].self, from: data)
      return grouped.values.flatMap { $0 }.sorted { $0.createdAt > $1.createdAt }
    } catch {
      throw APIError.decoding
    }
  }

  func createItem(title: String, body: String, column: KanbanColumn) async throws -> KanbanItem {
    var request = try await authorizedRequest(path: "api/kanban/items", method: "POST")
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    do {
      request.httpBody = try JSONEncoder().encode(
        CreateItemRequest(title: title, body: body, column: column))
    } catch {
      throw APIError.decoding
    }
    let data = try await send(request)
    do {
      return try Self.decoder.decode(KanbanItem.self, from: data)
    } catch {
      throw APIError.decoding
    }
  }

  /// Request-Body für POST /api/kanban/items.
  private struct CreateItemRequest: Encodable {
    let title: String
    let body: String
    let column: KanbanColumn
  }

  /// Baut einen mit Bearer-Token versehenen Request; wirft `.unauthorized` ohne Token.
  private func authorizedRequest(path: String, method: String) async throws -> URLRequest {
    guard let token = await tokenProvider() else { throw APIError.unauthorized }
    var request = URLRequest(url: baseURL.appendingPathComponent(path))
    request.httpMethod = method
    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
    request.setValue("application/json", forHTTPHeaderField: "Accept")
    return request
  }

  /// Führt den Request aus und mappt Transport-/HTTP-Fehler auf `APIError`.
  private func send(_ request: URLRequest) async throws -> Data {
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
    return data
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
