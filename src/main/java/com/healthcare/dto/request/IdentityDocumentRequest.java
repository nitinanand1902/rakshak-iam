package com.healthcare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request to add an identity document for a patient. */
public record IdentityDocumentRequest(
        @NotNull(message = "Document type is required")
        String documentType,

        @NotBlank(message = "Document value is required")
        String documentValue,

        /**
         * ISO 3166-1 alpha-2 country code.
         * Determines which masking strategy and validation rules apply.
         */
        @NotBlank(message = "Country code is required")
        String countryCode
) {}
