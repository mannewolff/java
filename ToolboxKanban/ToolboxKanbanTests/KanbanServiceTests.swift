//
//  KanbanServiceTests.swift
//  ToolboxKanbanTests
//
//  Tests für #244: GET /api/kanban/items — gruppierte Antwort dekodieren/flatten,
//  Sortierung, Auth- und Fehler-Mapping.
//

import Foundation
import Testing

@testable import ToolboxKanban

@Suite(.serialized)
struct KanbanServiceTests {

  private let baseURL = URL(string: "http://localhost:8080")!

  private func makeService(token: String? = "test-token") -> KanbanService {
    KanbanService(
      baseURL: baseURL,
      session: KanbanMockURLProtocol.makeSession(),
      tokenProvider: { token })
  }

  private func grouped(_ json: String) {
    KanbanMockURLProtocol.handler = { _ in (200, Data(json.utf8)) }
  }

  @Test func decodesGroupedResponseAndFlattens() async throws {
    grouped(
      """
      {
        "BACKLOG": [
          {"id":1,"title":"Erstes","body":"","column":"BACKLOG","position":0,
           "createdAt":"2026-06-10T08:00:00Z","updatedAt":"2026-06-10T08:00:00Z",
           "movedToDoneAt":null,"archived":false,"number":1}
        ],
        "IN_PROGRESS": [],
        "IN_REVIEW": [],
        "DONE": [
          {"id":2,"title":"Zweites","body":"x","column":"DONE","position":0,
           "createdAt":"2026-06-12T08:00:00Z","updatedAt":"2026-06-12T08:00:00Z",
           "movedToDoneAt":"2026-06-13T08:00:00Z","archived":false,"number":2}
        ]
      }
      """)
    let items = try await makeService().fetchItems()
    #expect(items.count == 2)
    // Neueste zuerst → #2 (12.06.) vor #1 (10.06.).
    #expect(items.first?.id == 2)
    #expect(items.first?.column == .done)
    #expect(items.first?.movedToDoneAt != nil)
    #expect(items.last?.id == 1)
    #expect(items.last?.movedToDoneAt == nil)
  }

  @Test func decodesFractionalSecondsTimestamps() async throws {
    grouped(
      """
      {"BACKLOG":[{"id":1,"title":"T","body":"","column":"BACKLOG","position":0,
       "createdAt":"2026-06-10T08:00:00.123Z","updatedAt":"2026-06-10T08:00:00.123Z",
       "movedToDoneAt":null,"archived":false,"number":1}],
       "IN_PROGRESS":[],"IN_REVIEW":[],"DONE":[]}
      """)
    let items = try await makeService().fetchItems()
    #expect(items.count == 1)
  }

  @Test func missingTokenThrowsUnauthorized() async {
    KanbanMockURLProtocol.handler = { _ in (200, Data("{}".utf8)) }
    await #expect(throws: APIError.unauthorized) {
      try await makeService(token: nil).fetchItems()
    }
  }

  @Test func http401ThrowsUnauthorized() async {
    KanbanMockURLProtocol.handler = { _ in (401, Data()) }
    await #expect(throws: APIError.unauthorized) {
      try await makeService().fetchItems()
    }
  }

  @Test func http500ThrowsHTTPStatus() async {
    KanbanMockURLProtocol.handler = { _ in (500, Data()) }
    await #expect(throws: APIError.httpStatus(500)) {
      try await makeService().fetchItems()
    }
  }

  @Test func malformedJSONThrowsDecoding() async {
    KanbanMockURLProtocol.handler = { _ in (200, Data("not json".utf8)) }
    await #expect(throws: APIError.decoding) {
      try await makeService().fetchItems()
    }
  }

  @Test func requestsCorrectItemsEndpoint() async throws {
    nonisolated(unsafe) var seenURL: URL?
    KanbanMockURLProtocol.handler = { request in
      seenURL = request.url
      return (200, Data("{\"BACKLOG\":[],\"IN_PROGRESS\":[],\"IN_REVIEW\":[],\"DONE\":[]}".utf8))
    }
    _ = try await makeService().fetchItems()
    #expect(seenURL?.path == "/api/kanban/items")
  }
}

/// Eigener URL-Protocol-Mock mit isoliertem statischem Handler, damit diese Suite
/// nicht mit anderen MockURLProtocol-Nutzern (z. B. TokenExchangeTests) um den
/// gemeinsamen Zustand konkurriert, wenn Swift-Testing-Suites parallel laufen.
final class KanbanMockURLProtocol: URLProtocol {
  nonisolated(unsafe) static var handler: ((URLRequest) throws -> (Int, Data))?

  override class func canInit(with request: URLRequest) -> Bool { true }
  override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

  override func startLoading() {
    guard let handler = KanbanMockURLProtocol.handler else {
      client?.urlProtocol(self, didFailWithError: URLError(.badServerResponse))
      return
    }
    do {
      let (status, data) = try handler(request)
      let response = HTTPURLResponse(
        url: request.url!, statusCode: status, httpVersion: nil, headerFields: nil)!
      client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
      client?.urlProtocol(self, didLoad: data)
      client?.urlProtocolDidFinishLoading(self)
    } catch {
      client?.urlProtocol(self, didFailWithError: error)
    }
  }

  override func stopLoading() {}

  static func makeSession() -> URLSession {
    let config = URLSessionConfiguration.ephemeral
    config.protocolClasses = [KanbanMockURLProtocol.self]
    return URLSession(configuration: config)
  }
}
