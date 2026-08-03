package com.yue.jobcomparer.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AiClientResolver {

    private final Map<AiProvider, AiClient> byProvider;

    public AiClientResolver(List<AiClient> aiClients) {
        this.byProvider = aiClients.stream()
                .collect(Collectors.toMap(AiClient::getProvider, client -> client));
    }

    public AiClient resolve(AiProvider provider) {
        return Optional.ofNullable(byProvider.get(provider))
                .orElseThrow(() -> new IllegalStateException("No AiClient implementation registered for provider: " + provider));
    }
}
