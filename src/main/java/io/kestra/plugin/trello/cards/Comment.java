package io.kestra.plugin.trello.cards;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.trello.AbstractTrelloTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@SuperBuilder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode

@Schema(
    title = "Add a comment to a Trello card",
    description = "Add a comment/action to an existing Trello card"
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
                    id: "5abbe4b7ddc1b351ef961414"
                    text: "This is my comment on the card"
                """
        )
    }
)
public class Comment extends AbstractTrelloTask {

    @Schema(title = "Card ID", description = "The ID of the card to add a comment to")
    @NotNull
    protected Property<String> cardId;

    @Schema(title = "Comment Text", description = "The text of the comment")
    @NotNull
    protected Property<String> text;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        String rId = runContext.render(this.cardId).as(String.class).orElseThrow();
        String rText = runContext.render(this.text).as(String.class).orElseThrow();

        String url = buildApiUrl(runContext, "cards/" + rId + "/actions/comments");
        String authParams = buildAuthParams(runContext);

        String fullUrl = url + "?" + authParams + "&text=" + URLEncoder.encode(rText, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.builder()
            .method("POST")
            .uri(URI.create(fullUrl))
            .addHeader("Accept", "application/json")
            .build();

        try (HttpClient httpClient = HttpClient.builder()
            .runContext(runContext)
            .build()) {
            HttpResponse<String> response = httpClient.request(request, String.class);

            if (response.getStatus().getCode() != 200) {
                throw new RuntimeException(
                    "Failed to add comment: " + response.getStatus().getCode() + " - "
                        + response.getBody());
            }

            return null;
        }
    }
}
