package com.healthcare.security.sso;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Maps a Spring Security {@link OidcUser} (from any OIDC provider) into our
 * normalized {@link SsoUserInfo} record.
 *
 * <p>Provider detection is based on the registration ID passed by Spring's OAuth2
 * client machinery. The {@code sub} claim is used as the stable, durable identifier
 * for the user within that IdP, which we store as {@code providerSub} on the User entity.
 */
@Component
public class OidcUserMapper {

    /**
     * Maps an OIDC user to our internal representation.
     *
     * @param oidcUser       the authenticated OIDC principal
     * @param registrationId the Spring OAuth2 registration ID ("google", "microsoft", etc.)
     * @return normalized {@link SsoUserInfo}
     */
    public SsoUserInfo map(OidcUser oidcUser, String registrationId) {
        String provider = resolveProvider(registrationId);
        String sub = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String displayName = resolveDisplayName(oidcUser);

        return new SsoUserInfo(provider, sub, email, displayName);
    }

    // -------------------------------------------------------------------------

    private String resolveProvider(String registrationId) {
        if (registrationId == null) return "OIDC_GENERIC";
        return switch (registrationId.toLowerCase()) {
            case "google"    -> "OIDC_GOOGLE";
            case "microsoft" -> "OIDC_MICROSOFT";
            default          -> "OIDC_GENERIC";
        };
    }

    private String resolveDisplayName(OidcUser user) {
        // Try full name, then email prefix as fallback
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getEmail() != null) {
            return user.getEmail().split("@")[0];
        }
        return user.getSubject();
    }
}
