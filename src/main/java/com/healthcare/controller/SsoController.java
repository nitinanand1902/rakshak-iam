package com.healthcare.controller;

import com.healthcare.dto.request.SsoCompleteRequest;
import com.healthcare.dto.response.AuthResponse;
import com.healthcare.dto.response.SsoProvidersResponse;
import com.healthcare.dto.response.SsoProvidersResponse.SsoProvider;
import com.healthcare.service.SsoAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * SSO REST controller.
 *
 * <p>Exposes three public/protected endpoints:
 * <ul>
 *   <li>{@code GET  /api/v1/auth/sso/providers}      — list configured providers (public)</li>
 *   <li>{@code POST /api/v1/auth/sso/complete}        — complete role-selection onboarding (provisioning JWT required)</li>
 *   <li>{@code GET  /api/v1/auth/sso/saml/metadata}   — SP metadata XML (public, for IdP registration)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth/sso")
@Tag(name = "SSO", description = "OAuth2/OIDC and SAML federated authentication")
@RequiredArgsConstructor
@Slf4j
public class SsoController {

    private final SsoAuthService ssoAuthService;

    @Value("${spring.security.oauth2.client.registration.google.client-id:CHANGE_ME_GOOGLE_CLIENT_ID}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.microsoft.client-id:CHANGE_ME_AZURE_CLIENT_ID}")
    private String microsoftClientId;

    @Value("${app.saml.idp-metadata-uri:CHANGE_ME_IDP_METADATA_URL}")
    private String samlIdpMetadataUri;

    // -------------------------------------------------------------------------

    @GetMapping("/providers")
    @Operation(
        summary = "List SSO providers",
        description = "Returns all configured SSO providers with their initiation URLs. " +
                      "The frontend uses this to render the social login buttons dynamically."
    )
    public ResponseEntity<SsoProvidersResponse> listProviders() {
        List<SsoProvider> providers = new ArrayList<>();

        // Google OIDC
        if (!googleClientId.startsWith("CHANGE_ME")) {
            providers.add(new SsoProvider(
                    "google",
                    "Sign in with Google",
                    "/login/oauth2/authorization/google",
                    "OIDC"
            ));
        } else {
            // Still expose the provider so the frontend can show the button (will fail with a clear IdP error)
            providers.add(new SsoProvider(
                    "google",
                    "Sign in with Google",
                    "/login/oauth2/authorization/google",
                    "OIDC"
            ));
        }

        // Microsoft OIDC
        if (!microsoftClientId.startsWith("CHANGE_ME")) {
            providers.add(new SsoProvider(
                    "microsoft",
                    "Sign in with Microsoft",
                    "/login/oauth2/authorization/microsoft",
                    "OIDC"
            ));
        } else {
            providers.add(new SsoProvider(
                    "microsoft",
                    "Sign in with Microsoft",
                    "/login/oauth2/authorization/microsoft",
                    "OIDC"
            ));
        }

        // SAML
        if (!samlIdpMetadataUri.startsWith("CHANGE_ME")) {
            providers.add(new SsoProvider(
                    "saml",
                    "Login with SSO",
                    "/saml2/authenticate/default-idp",
                    "SAML"
            ));
        } else {
            providers.add(new SsoProvider(
                    "saml",
                    "Login with SSO",
                    "/saml2/authenticate/default-idp",
                    "SAML"
            ));
        }

        return ResponseEntity.ok(new SsoProvidersResponse(providers));
    }

    @PostMapping("/complete")
    @Operation(
        summary = "Complete SSO onboarding",
        description = "Called after the user selects their role on the onboarding page. " +
                      "Requires the short-lived PROVISIONING JWT from the SSO callback. " +
                      "Returns a full access + refresh token pair."
    )
    public ResponseEntity<AuthResponse> completeProvisioning(
            @Valid @RequestBody SsoCompleteRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = ssoAuthService.completeProvisioning(
                request.provisioningToken(),
                request.role(),
                getClientIp(httpRequest)
        );
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Note: SAML metadata is served automatically by Spring Security at:
    //   GET /saml2/service-provider-metadata/{registrationId}
    // A convenience alias is exposed here for documentation purposes.
    // -------------------------------------------------------------------------

    @GetMapping("/saml/metadata-info")
    @Operation(
        summary = "SAML SP metadata location",
        description = "Returns the URL of the SAML Service Provider metadata XML. " +
                      "Register this URL in your IdP (Okta, Azure AD, ADFS) as the SP entity."
    )
    public ResponseEntity<java.util.Map<String, String>> samlMetadataInfo(HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return ResponseEntity.ok(java.util.Map.of(
                "metadataUrl", baseUrl + "/saml2/service-provider-metadata/default-idp",
                "acsUrl",      baseUrl + "/login/saml2/sso/default-idp",
                "entityId",    baseUrl + "/saml2/service-provider-metadata/default-idp"
        ));
    }

    // -------------------------------------------------------------------------
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
