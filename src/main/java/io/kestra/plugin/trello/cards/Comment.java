package io.kestra.plugin.trello.cards;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.trello.AbstractTrelloTask;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode

@Schema(
    title = "Add comments to Trello cards",
    description = "Posts one comment to an existing Trello card and returns the created comment ID. The `text` property is rendered, URL-encoded, and sent as a query parameter, so avoid putting secrets in comments"
)
@Plugin(
    examples = {
        @Example(
            title = "Add a comment to a Trello card",
            full = true,
            code = """
                id: trello_add_comment
                namespace: company.team

                tasks:
                  - id: add_comment
                    type: io.kestra.plugin.trello.cards.Comment
                    apiKey: "{{ secret('TRELLO_API_KEY') }}"
                    apiToken: "{{ secret('TRELLO_API_TOKEN') }}"
                    cardId: "5abbe4b7ddc1b351ef961414"
                    text: "This is my comment on the card"
                """
        )
    }
)
public class Comment extends AbstractTrelloTask {

    @Schema(title = "Card ID", description = "Card ID to comment on")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> cardId;

    @Schema(title = "Comment Text", description = "Comment text sent to Trello")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> text;

    @Override
    public io.kestra.core.models.tasks.Output run(RunContext runContext) throws Exception {
        String rId = runContext.render(this.cardId).as(String.class).orElseThrow();
        String rText = runContext.render(this.text).as(String.class).orElseThrow();

        String url = buildApiUrl(runContext, "cards/" + rId + "/actions/comments") + "?text="
            + URLEncoder.encode(rText, StandardCharsets.UTF_8);

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .method("POST")
            .uri(URI.create(url))
            .addHeader("Accept", "application/json");

        HttpRequest request = addAuthHeaders(runContext, requestBuilder).build();

        try (
            HttpClient httpClient = HttpClient.builder()
                .runContext(runContext)
                .build()
        ) {
            HttpResponse<String> response = httpClient.request(request, String.class);

            if (response.getStatus().getCode() != 200) {
                throw new RuntimeException(
                    "Failed to add comment: " + response.getStatus().getCode() + " - "
                        + response.getBody()
                );
            }

            JsonNode jsonNode = JacksonMapper.ofJson().readTree(response.getBody());

            return Output.builder()
                .commentId(jsonNode.has("id") ? jsonNode.get("id").asText() : null)
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created Comment ID", description = "Comment ID returned by Trello")
        private final String commentId;
    }
}
