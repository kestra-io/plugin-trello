package io.kestra.plugin.trello.cards;

import io.kestra.core.context.TestRunContextFactory;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.trello.AbstractTrelloTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TriggerTest extends AbstractTrelloTest {

    @Inject
    private TestRunContextFactory runContextFactory;

    @Test
    void testCardTriggerInterval() {
        Trigger trigger = Trigger.builder()
                .id("test-interval-trigger")
                .type(Trigger.class.getName())
                .apiKey(Property.ofValue("test-key"))
                .apiToken(Property.ofValue("test-token"))
                .listId(Property.ofValue("list123"))
                .build();

        // Default interval should be 5 minutes
        assertEquals(java.time.Duration.ofMinutes(5), trigger.getInterval());
    }

    @Test
    void testCardTriggerCustomInterval() {
        Trigger trigger = Trigger.builder()
                .id("test-custom-interval-trigger")
                .type(Trigger.class.getName())
                .apiKey(Property.ofValue("test-key"))
                .apiToken(Property.ofValue("test-token"))
                .listId(Property.ofValue("list123"))
                .interval(java.time.Duration.ofMinutes(10))
                .build();

        assertEquals(java.time.Duration.ofMinutes(10), trigger.getInterval());
    }

    @Test
    void testCardTriggerPropertyRendering() throws Exception {
        RunContext runContext = runContextFactory.of();

        Trigger trigger = Trigger.builder()
                .id("test-property-trigger")
                .type(Trigger.class.getName())
                .apiKey(Property.ofValue("my-api-key"))
                .apiToken(Property.ofValue("my-api-token"))
                .listId(Property.ofValue("my-list-id"))
                .build();

        String renderedApiKey = runContext.render(trigger.getApiKey()).as(String.class).orElse(null);
        String renderedApiToken = runContext.render(trigger.getApiToken()).as(String.class).orElse(null);
        String renderedListId = runContext.render(trigger.getListId()).as(String.class).orElse(null);

        assertEquals("my-api-key", renderedApiKey);
        assertEquals("my-api-token", renderedApiToken);
        assertEquals("my-list-id", renderedListId);
    }

    @Test
    void testCardTriggerWithListIdsPropertyRendering() throws Exception {
        RunContext runContext = runContextFactory.of();

        Trigger trigger = Trigger.builder()
                .id("test-list-ids-trigger")
                .type(Trigger.class.getName())
                .apiKey(Property.ofValue("test-key"))
                .apiToken(Property.ofValue("test-token"))
                .listIds(Property.ofValue(List.of("list1", "list2", "list3")))
                .build();

        List<String> renderedListIds = runContext.render(trigger.getListIds()).asList(String.class);

        assertEquals(3, renderedListIds.size());
        assertTrue(renderedListIds.contains("list1"));
        assertTrue(renderedListIds.contains("list2"));
        assertTrue(renderedListIds.contains("list3"));
    }
}
