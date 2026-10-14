package com.healthcare.security.sso;

import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps a Spring Security {@link Saml2AuthenticatedPrincipal} to our normalized
 * {@link SsoUserInfo} record.
 *
 * <p>SAML assertions may encode user attributes using various attribute names
 * depending on the IdP. We check common URN-format names (used by ADFS/Azure AD)
 * as well as friendly short names (used by Okta/Shibboleth).
 */
@Component
public class SamlUserMapper {

    // Common SAML attribute URNs
    private static final String ATTR_EMAIL_URN     = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";
    private static final String ATTR_EMAIL_OKTA    = "email";
    private static final String ATTR_NAME_URN      = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name";
    private static final String ATTR_GIVENNAME_URN = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname";
    private static final String ATTR_SURNAME_URN   = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname";
    private static final String ATTR_DISPLAY_NAME  = "displayName";
    private static final String ATTR_FIRST_NAME    = "firstName";
    private static final String ATTR_LAST_NAME     = "lastName";

    /**
     * Maps a SAML principal to our internal representation.
     *
     * @param principal       the authenticated SAML principal
     * @return normalized {@link SsoUserInfo}
     */
    public SsoUserInfo map(Saml2AuthenticatedPrincipal principal) {
        String sub  = principal.getName(); // NameID
        String email = resolveAttribute(principal, ATTR_EMAIL_URN, ATTR_EMAIL_OKTA, "mail");
        String name  = resolveDisplayName(principal);

        return new SsoUserInfo("SAML", sub, email, name);
    }

    // -------------------------------------------------------------------------

    private String resolveAttribute(Saml2AuthenticatedPrincipal principal, String... candidateNames) {
        for (String candidate : candidateNames) {
            List<Object> values = principal.getAttributes().get(candidate);
            if (values != null && !values.isEmpty()) {
                Object v = values.get(0);
                if (v instanceof String s && !s.isBlank()) return s;
            }
        }
        return null;
    }

    private String resolveDisplayName(Saml2AuthenticatedPrincipal principal) {
        // 1. Try URN full name
        String fullName = resolveAttribute(principal, ATTR_NAME_URN, ATTR_DISPLAY_NAME);
        if (fullName != null) return fullName;

        // 2. Try combining given + surname
        String first = resolveAttribute(principal, ATTR_GIVENNAME_URN, ATTR_FIRST_NAME);
        String last  = resolveAttribute(principal, ATTR_SURNAME_URN, ATTR_LAST_NAME);
        if (first != null && last != null) return first + " " + last;
        if (first != null) return first;

        // 3. Fall back to email or NameID
        String email = resolveAttribute(principal, ATTR_EMAIL_URN, ATTR_EMAIL_OKTA);
        if (email != null) return email.split("@")[0];

        return principal.getName();
    }
}
