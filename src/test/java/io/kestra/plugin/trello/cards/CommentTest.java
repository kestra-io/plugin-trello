package io.kestra.plugin.trello.cards;

import io.kestra.core.context.TestRunContextFactory;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.trello.AbstractTrelloTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class CommentTest extends AbstractTrelloTest {

    @Inject
    private TestRunContextFactory runContextFactory;

    @Test
    void testAddComment() throws Exception {
        Create createTask = Create.builder()
            .id("test-create-for-comment")
            .type(Create.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .name(Property.ofValue("Card for Comments"))
            .idList(Property.ofValue("list123"))
            .build();

        RunContext createContext = TestsUtils.mockRunContext(runContextFactory, createTask, Map.of());
        createTask.run(createContext);

        Comment commentTask = Comment.builder()
            .id("test-add-comment")
            .type(Comment.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .cardId(Property.ofValue("test-card-id"))
            .text(Property.ofValue("This is a test comment"))
            .build();

        RunContext commentContext = runContextFactory.of();
        commentTask.run(commentContext);
    }

    @Test
    void testAddMultipleComments() throws Exception {
        Create createTask = Create.builder()
            .id("test-create-for-multi-comment")
            .type(Create.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .name(Property.ofValue("Card for Multiple Comments"))
            .idList(Property.ofValue("list456"))
            .build();

        RunContext createContext = TestsUtils.mockRunContext(runContextFactory, createTask, Map.of());
        createTask.run(createContext);

        Comment firstCommentTask = Comment.builder()
            .id("test-first-comment")
            .type(Comment.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .cardId(Property.ofValue("test-card-id"))
            .text(Property.ofValue("First comment"))
            .build();

        RunContext firstContext = runContextFactory.of();
        firstCommentTask.run(firstContext);

        Comment secondCommentTask = Comment.builder()
            .id("test-second-comment")
            .type(Comment.class.getName())
            .apiKey(Property.ofValue("test-key"))
            .apiToken(Property.ofValue("test-token"))
            .apiBaseUrl(Property.ofValue(getApiBaseUrl()))
            .cardId(Property.ofValue("test-card-id"))
            .text(Property.ofValue("Second comment"))
            .build();

        RunContext secondContext = runContextFactory.of();
        secondCommentTask.run(secondContext);
        secondCommentTask.run(secondContext);
    }
}
