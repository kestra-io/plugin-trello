package io.kestra.plugin.trello.cards;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.trello.AbstractTrelloTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@SuperBuilder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@Schema(
    title = "Move a Trello card between lists",
    description = "Move a card from one list to another"
)
@Plugin(
    examples = {
        @Example(
            title = "Move a Trello card to another list",
            full = true,
            code = """
                id: trello_move_card
                namespace: company.team

                tasks:
                  - id: move_card
                    type: io.kestra.plugin.trello.cards.Move
                    apiKey: "{{ secret('TRELLO_API_KEY') }}"
                    apiToken: "{{ secret('TRELLO_API_TOKEN') }}"
                    cardId: "5abbe4b7ddc1b351ef961414"
                    idList: "5abbe4b7ddc1b351ef961415"
                """
        )
    }
)
public class Move extends AbstractTrelloTask {

    @Schema(title = "Card ID", description = "The ID of the card to move")
    @NotNull
    protected Property<String> cardId;

    @Schema(title = "Target List ID", description = "The ID of the list to move the card to")
    @NotNull
    protected Property<String> idList;

    @Schema(title = "Position", description = "The position of the card in the new list. top, bottom, or a positive float")
    protected Property<String> pos;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        String rId = runContext.render(this.cardId).as(String.class).orElseThrow();
        String url = buildApiUrl(runContext, "cards/" + rId);

        Map<String, Object> moveData = new HashMap<>();

        String rIdList = runContext.render(this.idList).as(String.class).orElseThrow();
        moveData.put("idList", rIdList);

        runContext.render(this.pos).as(String.class).ifPresent(val -> moveData.put("pos", val));

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .method("PUT")
            .uri(URI.create(url))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .body(HttpRequest.StringRequestBody.builder()
                .content(JacksonMapper.ofJson().writeValueAsString(moveData))
                .build());

        HttpRequest request = addAuthHeaders(runContext, requestBuilder).build();

        try (HttpClient httpClient = HttpClient.builder()
            .runContext(runContext)
            .build()) {
            HttpResponse<String> response = httpClient.request(request, String.class);

            if (response.getStatus().getCode() != 200) {
                throw new RuntimeException(
                    "Failed to move card: " + response.getStatus().getCode() + " - "
                        + response.getBody());
            }

            return null;
        }
    }
}
