package io.kestra.plugin.trello.cards;

import io.kestra.core.context.TestRunContextFactory;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.trello.AbstractTrelloTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class MoveTest extends AbstractTrelloTest {

    @Inject
    private TestRunContextFactory runContextFactory;

    @Test
    void testMoveCard() throws Exception {
        Create createTask = Create.builder()
            .id("test-create-for-move")
            .type(Create.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .name(Property.ofValue("Card to Move"))
            .idList(Property.ofValue("list123"))
            .build();

        RunContext createContext = TestsUtils.mockRunContext(runContextFactory, createTask, Map.of());
        createTask.run(createContext);

        Move moveTask = Move.builder()
            .id("test-move-card")
            .type(Move.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .cardId(Property.ofValue("test-card-id"))
            .idList(Property.ofValue("list456"))
            .build();

        RunContext moveContext = runContextFactory.of();
        moveTask.run(moveContext);
    }

    @Test
    void testMoveCardWithPosition() throws Exception {
        Create createTask = Create.builder()
            .id("test-create-for-move-pos")
            .type(Create.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .name(Property.ofValue("Card to Move with Pos"))
            .idList(Property.ofValue("list789"))
            .build();

        RunContext createContext = TestsUtils.mockRunContext(runContextFactory, createTask, Map.of());
        createTask.run(createContext);

        Move moveTask = Move.builder().id("test-move-card-pos")
            .type(Move.class.getName()).apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .cardId(Property.ofValue("test-card-id"))
            .idList(Property.ofValue("list999"))
            .pos(Property.ofValue("top"))
            .build();

        RunContext moveContext = runContextFactory.of();
        moveTask.run(moveContext);
    }
}
