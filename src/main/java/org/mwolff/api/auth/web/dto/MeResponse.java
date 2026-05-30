package org.mwolff.api.auth.web.dto;

import java.util.List;

/**
 * Identity payload exposed at {@code GET /api/me}. Mapped from the Keycloak ID/Access-Token
 * standard claims plus the {@code realm_access.roles} list.
 *
 * @param subject the JWT {@code sub} claim — opaque, stable Keycloak user id
 * @param username the JWT {@code preferred_username} claim
 * @param email the JWT {@code email} claim
 * @param givenName the JWT {@code given_name} claim
 * @param familyName the JWT {@code family_name} claim
 * @param roles the realm roles assigned to the user, without the {@code ROLE_} prefix
 */
public record MeResponse(
    String subject,
    String username,
    String email,
    String givenName,
    String familyName,
    List<String> roles) {}
