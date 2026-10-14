package com.healthcare.security;

import com.healthcare.config.SamlProperties;
import com.healthcare.security.sso.OidcSuccessHandler;
import com.healthcare.security.sso.SamlSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.List;

/**
 * Spring Security configuration.
 * <p>
 * Supports three authentication mechanisms:
 * <ol>
 *   <li><b>Username/password (local)</b> — existing JWT filter pipeline</li>
 *   <li><b>OAuth2 / OIDC</b> — Google, Microsoft, and generic OIDC providers</li>
 *   <li><b>SAML 2.0</b> — SP-initiated SSO via Spring Security SAML2</li>
 * </ol>
 * All three flows ultimately issue our own JWT tokens so the downstream RBAC
 * and audit pipeline remain completely unchanged.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final OidcSuccessHandler oidcSuccessHandler;
    private final SamlSuccessHandler samlSuccessHandler;
    private final SamlProperties samlProperties;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler,
                          OidcSuccessHandler oidcSuccessHandler,
                          SamlSuccessHandler samlSuccessHandler,
                          SamlProperties samlProperties) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.oidcSuccessHandler = oidcSuccessHandler;
        this.samlSuccessHandler = samlSuccessHandler;
        this.samlProperties = samlProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            // SSO (OIDC + SAML) relies on browser redirects so we cannot be fully stateless.
            // We allow session creation for the OAuth2/SAML handshake only.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                // OIDC / SAML handshake endpoints (Spring Security internal)
                .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                .requestMatchers("/saml2/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // RBAC rules (unchanged)
                .requestMatchers(HttpMethod.GET, "/api/v1/patients/**").hasAnyRole("PATIENT", "DOCTOR", "TECHNICIAN")
                .requestMatchers(HttpMethod.POST, "/api/v1/patients/**").hasRole("PATIENT")
                .requestMatchers("/api/v1/doctors/**").hasAnyRole("DOCTOR", "TECHNICIAN")
                .requestMatchers(HttpMethod.POST, "/api/v1/diagnosis/**").hasRole("DOCTOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/diagnosis/**").hasAnyRole("PATIENT", "DOCTOR", "TECHNICIAN")
                .requestMatchers("/api/v1/reports/**").hasAnyRole("DOCTOR", "TECHNICIAN")
                .requestMatchers("/api/v1/identity/**").hasAnyRole("PATIENT", "DOCTOR", "TECHNICIAN")
                .requestMatchers("/api/v1/audit/**").hasRole("DOCTOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/iam/evaluate/me").authenticated()
                .requestMatchers("/api/v1/iam/**").hasAnyRole("DOCTOR", "TECHNICIAN")

                .anyRequest().authenticated())
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Allow H2 console frames
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))

            // ----------------------------------------------------------------
            // OAuth2 / OIDC — Google, Microsoft, and generic OIDC providers
            // ----------------------------------------------------------------
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oidcSuccessHandler)
                .failureUrl("/api/v1/auth/sso/failed"))

            // ----------------------------------------------------------------
            // SAML 2.0 Service Provider
            // Conditionally enabled; if the IdP metadata URI is unconfigured
            // the SAML filter chain is skipped gracefully.
            // ----------------------------------------------------------------
            .saml2Login(saml2 -> saml2
                .successHandler(samlSuccessHandler));

        return http.build();
    }

    // -------------------------------------------------------------------------
    // SAML RelyingParty Registration
    // -------------------------------------------------------------------------

    /**
     * Builds the SP registration for the configured SAML IdP.
     * If the IdP metadata URI is the placeholder value, a minimal registration is
     * created with no assertion consumer URL so Spring Security doesn't try to
     * contact an unreachable endpoint at startup.
     */
    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        boolean samlConfigured = samlProperties.idpMetadataUri() != null
                && !samlProperties.idpMetadataUri().startsWith("CHANGE_ME");

        RelyingPartyRegistration registration;

        if (samlConfigured) {
            registration = buildConfiguredSamlRegistration();
        } else {
            // Dev/CI fallback — SAML IdP not yet configured. Creates a stub registration
            // so the app starts without errors. The /saml2/ endpoints will return errors
            // until a real IdP metadata URI is provided.
            registration = RelyingPartyRegistration
                    .withRegistrationId("default-idp")
                    .entityId("http://localhost:8080/saml2/service-provider-metadata/default-idp")
                    .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
                    .assertingPartyDetails(party -> party
                            .entityId("https://not-configured.example.com")
                            .singleSignOnServiceLocation("https://not-configured.example.com/sso/saml")
                            .wantAuthnRequestsSigned(false))
                    .build();
        }

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    private RelyingPartyRegistration buildConfiguredSamlRegistration() {
        try {
            // Load SP signing certificate from keystore
            DefaultResourceLoader loader = new DefaultResourceLoader();
            Resource keystoreResource = loader.getResource(samlProperties.keystorePath());

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream ks = keystoreResource.getInputStream()) {
                keyStore.load(ks, samlProperties.keystorePassword().toCharArray());
            }

            RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(
                    samlProperties.keyAlias(),
                    samlProperties.keystorePassword().toCharArray()
            );
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(samlProperties.keyAlias());
            Saml2X509Credential signingCredential = Saml2X509Credential.signing(privateKey, cert);
            Saml2X509Credential decryptionCredential = Saml2X509Credential.decryption(privateKey, cert);

            return RelyingPartyRegistrations
                    .fromMetadataLocation(samlProperties.idpMetadataUri())
                    .registrationId("default-idp")
                    .entityId(samlProperties.spEntityId())
                    .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
                    .signingX509Credentials(creds -> creds.add(signingCredential))
                    .decryptionX509Credentials(creds -> creds.add(decryptionCredential))
                    .build();

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load SAML keystore from: " + samlProperties.keystorePath()
                    + ". Check app.saml.* properties.", ex);
        }
    }

    // -------------------------------------------------------------------------
    // CORS
    // -------------------------------------------------------------------------

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Refresh-Token"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true); // required for OIDC/SAML cookie-based sessions
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
