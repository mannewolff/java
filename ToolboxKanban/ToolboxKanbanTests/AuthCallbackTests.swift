//
//  AuthCallbackTests.swift
//  ToolboxKanbanTests
//

import Foundation
import Testing

@testable import ToolboxKanban

struct AuthCallbackTests {

  @Test func extractsCodeWhenStateMatches() throws {
    let url = URL(string: "org.mwolff.toolboxkanban://callback?code=abc123&state=s1")!
    let code = try AuthCallback.extractCode(from: url, expectedState: "s1")
    #expect(code == "abc123")
  }

  @Test func throwsStateMismatchWhenStateDiffers() {
    let url = URL(string: "org.mwolff.toolboxkanban://callback?code=abc&state=wrong")!
    #expect(throws: AuthCallbackError.stateMismatch) {
      try AuthCallback.extractCode(from: url, expectedState: "s1")
    }
  }

  @Test func throwsServerErrorWhenKeycloakReportsError() {
    let url = URL(string: "org.mwolff.toolboxkanban://callback?error=access_denied&state=s1")!
    #expect(throws: AuthCallbackError.serverError("access_denied")) {
      try AuthCallback.extractCode(from: url, expectedState: "s1")
    }
  }

  @Test func throwsMissingCodeWhenNeitherCodeNorError() {
    let url = URL(string: "org.mwolff.toolboxkanban://callback?state=s1")!
    #expect(throws: AuthCallbackError.missingCode) {
      try AuthCallback.extractCode(from: url, expectedState: "s1")
    }
  }
}
