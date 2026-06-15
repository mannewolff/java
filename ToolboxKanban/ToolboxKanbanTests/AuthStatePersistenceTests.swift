//
//  AuthStatePersistenceTests.swift
//  ToolboxKanbanTests
//
//  Tests für #243: Token-Persistenz, Restore beim Start und automatisches Refresh.
//

import Foundation
import Testing

@testable import ToolboxKanban

@Suite(.serialized)
@MainActor
struct AuthStatePersistenceTests {

  /// Fester Referenzzeitpunkt für deterministische Ablauf-Berechnungen.
  private let fixedNow = Date(timeIntervalSinceReferenceDate: 1000)

  private func makeState(
    service: MockAuthService,
    store: MockTokenStore
  ) -> AuthState {
    AuthState(authService: service, tokenStore: store, now: { self.fixedNow })
  }

  // MARK: - login persistiert

  @Test func loginPersistsTokensInStore() async {
    let store = MockTokenStore()
    let state = makeState(service: MockAuthService(result: .success(.fixture)), store: store)
    await state.login()
    #expect(state.isAuthenticated == true)
    #expect(store.stored?.accessToken == "test-access-token")
    #expect(store.stored?.refreshToken == "test-refresh-token")
    #expect(store.stored?.accessTokenExpiry == fixedNow.addingTimeInterval(300))
  }

  @Test func logoutClearsStore() async {
    let store = MockTokenStore()
    let state = makeState(service: MockAuthService(result: .success(.fixture)), store: store)
    await state.login()
    state.logout()
    #expect(state.isAuthenticated == false)
    #expect(store.stored == nil)
    #expect(store.clearCallCount >= 1)
  }

  // MARK: - restore

  @Test func restoreWithEmptyStoreStaysLoggedOut() async {
    let service = MockAuthService()
    let state = makeState(service: service, store: MockTokenStore())
    await state.restore()
    #expect(state.isAuthenticated == false)
    #expect(service.refreshCallCount == 0)
    #expect(state.isRestoring == false)
  }

  @Test func restoreWithValidAccessTokenAuthenticatesWithoutRefresh() async {
    let store = MockTokenStore(
      stored: StoredTokens(
        accessToken: "stored-access",
        refreshToken: "stored-refresh",
        accessTokenExpiry: fixedNow.addingTimeInterval(300)))
    let service = MockAuthService()
    let state = makeState(service: service, store: store)
    await state.restore()
    #expect(state.isAuthenticated == true)
    #expect(state.accessToken == "stored-access")
    #expect(service.refreshCallCount == 0)
  }

  @Test func restoreWithExpiredAccessTokenRefreshes() async {
    let store = MockTokenStore(
      stored: StoredTokens(
        accessToken: "old-access",
        refreshToken: "stored-refresh",
        accessTokenExpiry: fixedNow.addingTimeInterval(-10)))
    let service = MockAuthService(refreshResult: .success(.refreshed))
    let state = makeState(service: service, store: store)
    await state.restore()
    #expect(service.refreshCallCount == 1)
    #expect(service.lastRefreshToken == "stored-refresh")
    #expect(state.isAuthenticated == true)
    #expect(state.accessToken == "refreshed-access-token")
    #expect(store.stored?.accessToken == "refreshed-access-token")
  }

  @Test func restoreWithExpiredTokenAndNoRefreshTokenClears() async {
    let store = MockTokenStore(
      stored: StoredTokens(
        accessToken: "old-access",
        refreshToken: nil,
        accessTokenExpiry: fixedNow.addingTimeInterval(-10)))
    let service = MockAuthService()
    let state = makeState(service: service, store: store)
    await state.restore()
    #expect(state.isAuthenticated == false)
    #expect(service.refreshCallCount == 0)
    #expect(store.stored == nil)
  }

  @Test func restoreWithFailingRefreshClearsSession() async {
    struct Expired: Error {}
    let store = MockTokenStore(
      stored: StoredTokens(
        accessToken: "old-access",
        refreshToken: "stored-refresh",
        accessTokenExpiry: fixedNow.addingTimeInterval(-10)))
    let service = MockAuthService(refreshResult: .failure(Expired()))
    let state = makeState(service: service, store: store)
    await state.restore()
    #expect(service.refreshCallCount == 1)
    #expect(state.isAuthenticated == false)
    #expect(store.stored == nil)
  }

  // MARK: - validAccessToken

  @Test func validAccessTokenReturnsNilWhenNotAuthenticated() async {
    let state = makeState(service: MockAuthService(), store: MockTokenStore())
    let token = await state.validAccessToken()
    #expect(token == nil)
  }

  @Test func validAccessTokenReturnsCachedTokenWhenStillValid() async {
    let service = MockAuthService(result: .success(.fixture))
    let state = makeState(service: service, store: MockTokenStore())
    await state.login()
    let token = await state.validAccessToken()
    #expect(token == "test-access-token")
    #expect(service.refreshCallCount == 0)
  }

  @Test func validAccessTokenRefreshesWhenExpired() async {
    // Login zum Zeitpunkt fixedNow (Ablauf +300), dann Abruf 400s später.
    let service = MockAuthService(result: .success(.fixture), refreshResult: .success(.refreshed))
    var current = fixedNow
    let state = AuthState(
      authService: service, tokenStore: MockTokenStore(), now: { current })
    await state.login()
    current = fixedNow.addingTimeInterval(400)
    let token = await state.validAccessToken()
    #expect(service.refreshCallCount == 1)
    #expect(token == "refreshed-access-token")
  }

  @Test func validAccessTokenClearsSessionWhenRefreshFails() async {
    struct Expired: Error {}
    let service = MockAuthService(result: .success(.fixture), refreshResult: .failure(Expired()))
    var current = fixedNow
    let store = MockTokenStore()
    let state = AuthState(authService: service, tokenStore: store, now: { current })
    await state.login()
    current = fixedNow.addingTimeInterval(400)
    let token = await state.validAccessToken()
    #expect(token == nil)
    #expect(state.isAuthenticated == false)
    #expect(store.stored == nil)
  }
}
