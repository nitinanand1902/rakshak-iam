package com.healthcare.security.sso;

import com.healthcare.service.SsoAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Success handler invoked by Spring Security after a successful OIDC login.
 *
 * <p>Delegates to {@link SsoAuthService} to look up or provision the user,
 * then redirects the browser to the frontend callback URL with appropriate tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OidcSuccessHandler implements AuthenticationSuccessHandler {

    private final SsoAuthService ssoAuthService;
    private final OidcUserMapper oidcUserMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OidcUser oidcUser = (OidcUser) oauthToken.getPrincipal();
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();

            SsoUserInfo ssoInfo = oidcUserMapper.map(oidcUser, registrationId);
            String ipAddress = extractIp(request);

            String redirectUrl = ssoAuthService.handleSsoLogin(ssoInfo, ipAddress);
            log.info("OIDC login successful, redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception ex) {
            log.error("OIDC success handler error: {}", ex.getMessage(), ex);
            response.sendRedirect("http://localhost:5173/login?error=sso_failed");
        }
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
