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
                .lists(Property.ofValue(List.of("list123")))
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
                .lists(Property.ofValue(List.of("list123")))
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
                .lists(Property.ofValue(List.of("my-list-id")))
                .build();

        String renderedApiKey = runContext.render(trigger.getApiKey()).as(String.class).orElse(null);
        String renderedApiToken = runContext.render(trigger.getApiToken()).as(String.class).orElse(null);
        List<String> renderedLists = runContext.render(trigger.getLists()).asList(String.class);

        assertEquals("my-api-key", renderedApiKey);
        assertEquals("my-api-token", renderedApiToken);
        assertEquals(1, renderedLists.size());
        assertEquals("my-list-id", renderedLists.getFirst());
    }

    @Test
    void testCardTriggerWithListsPropertyRendering() throws Exception {
        RunContext runContext = runContextFactory.of();

        Trigger trigger = Trigger.builder()
                .id("test-lists-trigger")
                .type(Trigger.class.getName())
                .apiKey(Property.ofValue("test-key"))
                .apiToken(Property.ofValue("test-token"))
                .lists(Property.ofValue(List.of("list1", "list2", "list3")))
                .build();

        List<String> renderedLists = runContext.render(trigger.getLists()).asList(String.class);

        assertEquals(3, renderedLists.size());
        assertTrue(renderedLists.contains("list1"));
        assertTrue(renderedLists.contains("list2"));
        assertTrue(renderedLists.contains("list3"));
    }
}
