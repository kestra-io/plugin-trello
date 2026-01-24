package io.kestra.plugin.trello.cards;

import io.kestra.core.context.TestRunContextFactory;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.trello.AbstractTrelloTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class CreateTest extends AbstractTrelloTest {

    @Inject
    private TestRunContextFactory runContextFactory;

    @Test
    void testCreateCard() throws Exception {
        Create task = Create.builder()
            .id("test-create-card")
            .type(Create.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .name(Property.ofValue("Test Card"))
            .idList(Property.ofValue("list123"))
            .desc(Property.ofValue("This is a test card"))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());
        task.run(runContext);
    }

    @Test
    void testCreateCardWithPosition() throws Exception {
        Create task = Create.builder()
            .id("test-create-card-position")
            .type(Create.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .name(Property.ofValue("Test Card with Position"))
            .idList(Property.ofValue("list456"))
            .desc(Property.ofValue("Card with specific position"))
            .pos(Property.ofValue("top"))
            .build();

        RunContext runContext = runContextFactory.of();
        task.run(runContext);
    }
}
