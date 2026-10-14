package com.healthcare.security.sso;

import com.healthcare.service.SsoAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Success handler invoked by Spring Security after a successful SAML 2.0 login.
 *
 * <p>Delegates to {@link SsoAuthService} to look up or provision the user,
 * then redirects the browser to the frontend callback URL with appropriate tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SamlSuccessHandler implements AuthenticationSuccessHandler {

    private final SsoAuthService ssoAuthService;
    private final SamlUserMapper samlUserMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            Saml2Authentication samlAuth = (Saml2Authentication) authentication;
            Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) samlAuth.getPrincipal();

            SsoUserInfo ssoInfo = samlUserMapper.map(principal);
            String ipAddress = extractIp(request);

            String redirectUrl = ssoAuthService.handleSsoLogin(ssoInfo, ipAddress);
            log.info("SAML login successful, redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception ex) {
            log.error("SAML success handler error: {}", ex.getMessage(), ex);
            response.sendRedirect("http://localhost:5173/login?error=saml_failed");
        }
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
