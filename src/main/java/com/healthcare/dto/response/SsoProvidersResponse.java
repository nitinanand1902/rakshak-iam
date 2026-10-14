package com.healthcare.dto.response;

import java.util.List;

/**
 * Response for GET /api/v1/auth/sso/providers.
 * Tells the frontend which SSO providers are configured and what URLs to redirect to.
 *
 * @param providers list of configured SSO providers
 */
public record SsoProvidersResponse(List<SsoProvider> providers) {

    /**
     * A single configured SSO provider.
     *
     * @param id          provider identifier (e.g. "google", "microsoft", "saml")
     * @param displayName human-readable name shown on the button
     * @param loginUrl    URL the frontend should navigate to for SSO initiation
     * @param type        protocol: "OIDC" or "SAML"
     */
    public record SsoProvider(
            String id,
            String displayName,
            String loginUrl,
            String type
    ) {}
}
