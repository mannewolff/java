//
//  MockURLProtocol.swift
//  ToolboxKanbanTests
//
//  Fängt URLSession-Requests ab, um Netzwerkantworten in Tests zu stubben.
//

import Foundation

final class MockURLProtocol: URLProtocol {
  /// Handler bekommt den Request und liefert (Status, Body, optional zuletzt gesehener Request-Body).
  nonisolated(unsafe) static var handler: ((URLRequest) throws -> (Int, Data))?
  /// Letzter abgefangener Request-Body (für Assertions auf die gesendeten Form-Daten).
  nonisolated(unsafe) static var lastRequestBody: String?

  override class func canInit(with request: URLRequest) -> Bool { true }
  override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

  override func startLoading() {
    if let stream = request.httpBodyStream {
      stream.open()
      var data = Data()
      let bufferSize = 1024
      let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
      while stream.hasBytesAvailable {
        let read = stream.read(buffer, maxLength: bufferSize)
        if read <= 0 { break }
        data.append(buffer, count: read)
      }
      buffer.deallocate()
      stream.close()
      MockURLProtocol.lastRequestBody = String(data: data, encoding: .utf8)
    } else if let body = request.httpBody {
      MockURLProtocol.lastRequestBody = String(data: body, encoding: .utf8)
    }

    guard let handler = MockURLProtocol.handler else {
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

  /// Erzeugt eine URLSession, die ausschließlich über diesen Mock läuft.
  static func makeSession() -> URLSession {
    let config = URLSessionConfiguration.ephemeral
    config.protocolClasses = [MockURLProtocol.self]
    return URLSession(configuration: config)
  }
}
