package io.kestra.plugin.trello.cards;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.kestra.core.context.TestRunContextFactory;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.trello.AbstractTrelloTest;

import jakarta.inject.Inject;

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

    @Test
    void testCardTriggerEvaluatePopulatesCount() throws Exception {
        Trigger trigger = Trigger.builder()
            .id("test-count-trigger")
            .type(Trigger.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .lists(Property.ofValue(List.of("list123")))
            .build();

        Map.Entry<ConditionContext, io.kestra.core.models.triggers.Trigger> mock = TestsUtils.mockTrigger(runContextFactory, trigger);

        Optional<Execution> evaluate = trigger.evaluate(mock.getKey(), mock.getValue());

        assertTrue(evaluate.isPresent());

        Map<String, Object> triggerVariables = evaluate.get().getTrigger().getVariables();
        List<?> cards = (List<?>) triggerVariables.get("cards");

        assertNotNull(cards);
        assertFalse(cards.isEmpty());
        assertEquals(cards.size(), triggerVariables.get("count"));
    }
}
