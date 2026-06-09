//
//  PKCETests.swift
//  ToolboxKanbanTests
//

import Foundation
import Testing

@testable import ToolboxKanban

struct PKCETests {

  /// Offizielles Testvektor-Paar aus RFC 7636 Appendix B.
  @Test func rfc7636TestVectorProducesExpectedChallenge() {
    let pkce = PKCE(codeVerifier: "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
    #expect(pkce.codeChallenge == "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
  }

  @Test func challengeMethodIsS256() {
    #expect(PKCE.generate().codeChallengeMethod == "S256")
  }

  @Test func generatedVerifierHasLegalLength() {
    let length = PKCE.generate().codeVerifier.count
    #expect(length >= 43 && length <= 128)
  }

  @Test func generatedVerifierUsesOnlyUnreservedCharacters() {
    let allowed = Set("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~")
    let verifier = PKCE.generate().codeVerifier
    #expect(verifier.allSatisfy { allowed.contains($0) })
  }

  @Test func challengeHasNoBase64Padding() {
    #expect(!PKCE.generate().codeChallenge.contains("="))
  }

  @Test func twoGeneratedPairsDiffer() {
    #expect(PKCE.generate().codeVerifier != PKCE.generate().codeVerifier)
  }
}
