package io.kestra.plugin.trello;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.docs.JsonSchemaGenerator;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.plugin.trello.cards.Comment;
import io.kestra.plugin.trello.cards.Create;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

@KestraTest
class OutputsSchemaTest {

    @Inject
    private JsonSchemaGenerator jsonSchemaGenerator;

    @Test
    @SuppressWarnings("unchecked")
    void createOutputsSchemaExposesCardIdAndCardUrl() {
        Map<String, Object> outputs = jsonSchemaGenerator.outputs(null, Create.class);
        Map<String, Object> properties = (Map<String, Object>) outputs.get("properties");

        assertThat(properties, hasKey("cardId"));
        assertThat(properties, hasKey("cardUrl"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void commentOutputsSchemaExposesCommentId() {
        Map<String, Object> outputs = jsonSchemaGenerator.outputs(null, Comment.class);
        Map<String, Object> properties = (Map<String, Object>) outputs.get("properties");

        assertThat(properties, hasKey("commentId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createConnectionPropertiesAreGrouped() {
        Map<String, Object> schema = jsonSchemaGenerator.schemas(Create.class, false);
        Map<String, Object> definitions = (Map<String, Object>) schema.get("definitions");
        Map<String, Object> createDefinition = (Map<String, Object>) definitions.get(Create.class.getName());
        Map<String, Object> properties = (Map<String, Object>) createDefinition.get("properties");

        for (String connectionProperty : new String[] { "apiKey", "apiToken", "apiVersion" }) {
            Map<String, Object> property = (Map<String, Object>) properties.get(connectionProperty);
            assertThat(property, hasKey("$group"));
            assertThat(property.get("$group"), is("connection"));
        }
    }
}
