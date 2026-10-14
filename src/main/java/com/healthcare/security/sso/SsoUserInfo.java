package com.healthcare.security.sso;

/**
 * Normalized user information extracted from any SSO provider (OIDC or SAML).
 * This is the common representation passed to {@link com.healthcare.service.SsoAuthService}
 * regardless of the upstream identity protocol.
 *
 * @param provider    IdP identifier: "OIDC_GOOGLE", "OIDC_MICROSOFT", "OIDC_GENERIC", "SAML"
 * @param providerSub unique subject identifier issued by the IdP (e.g. Google UID, SAML nameID)
 * @param email       user's email address
 * @param displayName user's display name (full name or preferred username)
 */
public record SsoUserInfo(
        String provider,
        String providerSub,
        String email,
        String displayName
) {}
