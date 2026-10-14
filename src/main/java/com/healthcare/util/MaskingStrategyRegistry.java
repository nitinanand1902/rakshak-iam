package com.healthcare.util;

import com.healthcare.entity.DocumentType;
import com.healthcare.util.masking.GenericDocumentMaskingStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry that resolves the correct {@link MaskingStrategy} for any {@link DocumentType}.
 * <p>
 * Spring auto-discovers all {@link MaskingStrategy} beans. Adding a new document type:
 * 1. Add enum constant to {@link DocumentType}
 * 2. Implement {@link MaskingStrategy} — Spring auto-registers it
 * 3. No changes to this class (OCP)
 */
@Service
@Slf4j
public class MaskingStrategyRegistry {

    private final Map<DocumentType, MaskingStrategy> strategyMap = new EnumMap<>(DocumentType.class);
    private final List<MaskingStrategy> strategies;
    private final GenericDocumentMaskingStrategy genericStrategy;

    public MaskingStrategyRegistry(List<MaskingStrategy> strategies,
                                   GenericDocumentMaskingStrategy genericStrategy) {
        this.strategies = strategies;
        this.genericStrategy = genericStrategy;
    }

    @PostConstruct
    public void init() {
        for (MaskingStrategy strategy : strategies) {
            strategyMap.put(strategy.supports(), strategy);
            log.info("Registered masking strategy: {} -> {}", strategy.supports(), strategy.getClass().getSimpleName());
        }
        // Map DRIVING_LICENSE to the generic strategy if not specifically registered
        strategyMap.putIfAbsent(DocumentType.DRIVING_LICENSE, genericStrategy);
    }

    /**
     * Resolves the masking strategy for the given document type.
     * Falls back to {@link GenericDocumentMaskingStrategy} if no specific strategy found.
     *
     * @param documentType the document type to resolve
     * @return the appropriate masking strategy (never null)
     */
    public MaskingStrategy resolve(DocumentType documentType) {
        return strategyMap.getOrDefault(documentType, genericStrategy);
    }
}
