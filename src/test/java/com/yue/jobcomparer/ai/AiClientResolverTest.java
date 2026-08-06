package com.yue.jobcomparer.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AiClientResolverTest {

    @Test
    void resolve_shouldReturnMatchingClient() {
        // Arrange
        AiClient anthropic = clientFor(AiProvider.ANTHROPIC);
        AiClient deepseek = clientFor(AiProvider.DEEPSEEK);
        AiClientResolver resolver = new AiClientResolver(List.of(anthropic, deepseek));

        // Act
        AiClient result = resolver.resolve(AiProvider.DEEPSEEK);

        // Assert
        assertEquals(deepseek, result);
    }

    @Test
    void resolve_withUnregisteredProvider_shouldThrowIllegalStateException() {
        // Arrange
        AiClient anthropic = clientFor(AiProvider.ANTHROPIC);
        AiClientResolver resolver = new AiClientResolver(List.of(anthropic));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> resolver.resolve(AiProvider.DEEPSEEK));
    }

    @Test
    void constructor_withDuplicateProviders_shouldThrowIllegalStateException() {
        // Arrange
        AiClient first = clientFor(AiProvider.ANTHROPIC);
        AiClient second = clientFor(AiProvider.ANTHROPIC);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> new AiClientResolver(List.of(first, second)));
    }

    private AiClient clientFor(AiProvider provider) {
        AiClient client = mock(AiClient.class);
        when(client.getProvider()).thenReturn(provider);
        return client;
    }
}
