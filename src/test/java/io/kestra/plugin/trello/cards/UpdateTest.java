package io.kestra.plugin.trello.cards;

import io.kestra.core.context.TestRunContextFactory;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.trello.AbstractTrelloTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class UpdateTest extends AbstractTrelloTest {

    @Inject
    private TestRunContextFactory runContextFactory;

    @Test
    void testUpdateCard() throws Exception {
        Create createTask = Create.builder()
            .id("test-create-for-update")
            .type(Create.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .name(Property.ofValue("Original Card"))
            .idList(Property.ofValue("list123"))
            .build();

        RunContext createContext = TestsUtils.mockRunContext(runContextFactory, createTask, Map.of());
        createTask.run(createContext);

        Update updateTask = Update.builder()
            .id("test-update-card")
            .type(Update.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .cardId(Property.ofValue("test-card-id"))
            .name(Property.ofValue("Updated Card"))
            .desc(Property.ofValue("Updated description"))
            .build();

        RunContext updateContext = runContextFactory.of();
        updateTask.run(updateContext);
    }

    @Test
    void testUpdateCardClosed() throws Exception {
        Create createTask = Create.builder()
            .id("test-create-for-close")
            .type(Create.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .name(Property.ofValue("Card to Close"))
            .idList(Property.ofValue("list789"))
            .build();

        RunContext createContext = TestsUtils.mockRunContext(runContextFactory, createTask, Map.of());
        createTask.run(createContext);

        Update updateTask = Update.builder().id("test-close-card")
            .type(Update.class.getName()).apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .cardId(Property.ofValue("test-card-id"))
            .closed(Property.ofValue(true))
            .build();

        RunContext updateContext = runContextFactory.of();
        assertDoesNotThrow(() -> updateTask.run(updateContext));
    }
}
