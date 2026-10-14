package com.healthcare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SAML 2.0 Service Provider configuration properties.
 * Bound from the {@code app.saml.*} namespace in application.properties.
 *
 * @param spEntityId      the SP entity ID registered with the IdP
 * @param keystorePath    classpath or file: path to the PKCS12 keystore
 * @param keystorePassword password for the keystore and private key
 * @param keyAlias        alias of the private key within the keystore
 * @param idpMetadataUri  URL (or classpath path) of the IdP metadata XML
 */
@ConfigurationProperties(prefix = "app.saml")
public record SamlProperties(
        String spEntityId,
        String keystorePath,
        String keystorePassword,
        String keyAlias,
        String idpMetadataUri
) {}
