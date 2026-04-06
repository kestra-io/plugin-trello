package io.kestra.plugin.trello.cards;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

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
    title = "Create cards in a Trello list",
    description = "Creates one card in the target Trello list and returns the new card ID. All properties are rendered before the request; `pos` accepts `top`, `bottom`, or a positive float"
)
@Plugin(
    examples = {
        @Example(
            title = "Create a new Trello card",
            full = true,
            code = """
                id: trello_create_card
                namespace: company.team

                tasks:
                  - id: create_card
                    type: io.kestra.plugin.trello.cards.Create
                    apiKey: "{{ secret('TRELLO_API_KEY') }}"
                    apiToken: "{{ secret('TRELLO_API_TOKEN') }}"
                    name: "My New Card"
                    listId: "5abbe4b7ddc1b351ef961414"
                    desc: "This is the card description"
                """
        )
    }
)
public class Create extends AbstractTrelloTask {

    @Schema(title = "Card Name", description = "Name for the new card")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> name;

    @Schema(title = "List ID", description = "Target Trello list ID")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> listId;

    @Schema(title = "Card Description", description = "Description text for the new card")
    @PluginProperty(group = "advanced")
    protected Property<String> desc;

    @Schema(title = "Card Position", description = "Position in the list: `top`, `bottom`, or a positive float")
    @PluginProperty(group = "advanced")
    protected Property<String> pos;

    @Schema(title = "Card Due Date", description = "Due date value passed to Trello as provided")
    @PluginProperty(group = "advanced")
    protected Property<String> due;

    @Override
    public io.kestra.core.models.tasks.Output run(RunContext runContext) throws Exception {
        String url = buildApiUrl(runContext, "cards");

        Map<String, Object> cardData = new HashMap<>();

        String rName = runContext.render(this.name).as(String.class).orElseThrow();
        String rIdList = runContext.render(this.listId).as(String.class).orElseThrow();

        cardData.put("name", rName);
        cardData.put("idList", rIdList);

        runContext.render(this.desc).as(String.class).ifPresent(val -> cardData.put("desc", val));
        runContext.render(this.pos).as(String.class).ifPresent(val -> cardData.put("pos", val));
        runContext.render(this.due).as(String.class).ifPresent(val -> cardData.put("due", val));

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .method("POST")
            .uri(URI.create(url))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .body(
                HttpRequest.StringRequestBody.builder()
                    .content(JacksonMapper.ofJson().writeValueAsString(cardData))
                    .build()
            );

        HttpRequest request = addAuthHeaders(runContext, requestBuilder).build();

        try (
            HttpClient httpClient = HttpClient.builder()
                .runContext(runContext)
                .build()
        ) {
            HttpResponse<String> response = httpClient.request(request, String.class);

            if (response.getStatus().getCode() != 200) {
                throw new RuntimeException(
                    "Failed to create card: " + response.getStatus().getCode() + " - "
                        + response.getBody()
                );
            }

            JsonNode jsonNode = JacksonMapper.ofJson().readTree(response.getBody());

            return Output.builder()
                .cardId(jsonNode.has("id") ? jsonNode.get("id").asText() : null)
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Created Card ID", description = "Card ID returned by Trello")
        private final String cardId;
    }
}
