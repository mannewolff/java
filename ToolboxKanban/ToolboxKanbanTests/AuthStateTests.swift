//
//  AuthStateTests.swift
//  ToolboxKanbanTests
//

import Testing
@testable import ToolboxKanban

@Suite(.serialized)
@MainActor
struct AuthStateTests {

  @Test func isNotAuthenticatedInitially() {
    let state = AuthState(authService: MockAuthService())
    #expect(state.isAuthenticated == false)
    #expect(state.accessToken == nil)
  }

  @Test func loginSetsAuthenticatedOnSuccess() async {
    let state = AuthState(authService: MockAuthService(result: .success(.fixture)))
    await state.login()
    #expect(state.isAuthenticated == true)
    #expect(state.accessToken == "test-access-token")
  }

  @Test func loginRemainsFalseWhenCancelled() async {
    let state = AuthState(authService: MockAuthService(result: .failure(AuthError.cancelled)))
    await state.login()
    // Cancellation ist kein Fehler — User hat abgebrochen, kein Alert nötig.
    #expect(state.isAuthenticated == false)
    #expect(state.loginError == nil)
  }

  @Test func loginCapturesOtherErrors() async {
    struct NetworkError: Error {}
    let state = AuthState(authService: MockAuthService(result: .failure(NetworkError())))
    await state.login()
    #expect(state.isAuthenticated == false)
    #expect(state.loginError != nil)
  }

  @Test func logoutClearsAuthenticatedState() async {
    let state = AuthState(authService: MockAuthService(result: .success(.fixture)))
    await state.login()
    state.logout()
    #expect(state.isAuthenticated == false)
    #expect(state.accessToken == nil)
  }

  @Test func secondLoginAfterLogoutWorks() async {
    let state = AuthState(authService: MockAuthService(result: .success(.fixture)))
    await state.login()
    state.logout()
    await state.login()
    #expect(state.isAuthenticated == true)
  }
}
