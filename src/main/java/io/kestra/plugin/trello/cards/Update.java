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
    title = "Update a Trello card",
    description = "Update an existing Trello card's properties"
)
@Plugin(
    examples = {
        @Example(
            title = "Update a Trello card",
            full = true,
            code = """
                id: trello_update_card
                namespace: company.team

                tasks:
                  - id: update_card
                    type: io.kestra.plugin.trello.cards.Update
                    apiKey: "{{ secret('TRELLO_API_KEY') }}"
                    apiToken: "{{ secret('TRELLO_API_TOKEN') }}"
                    id: "5abbe4b7ddc1b351ef961414"
                    name: "Updated Card Name"
                    desc: "Updated description"
                """
        )
    }
)
public class Update extends AbstractTrelloTask {

    @Schema(title = "Card ID", description = "The ID of the card to update")
    @NotNull
    protected Property<String> cardId;

    @Schema(title = "Card Name", description = "The new name for the card")
    protected Property<String> name;

    @Schema(title = "Description", description = "The new description for the card")
    protected Property<String> desc;

    @Schema(title = "Closed", description = "Whether the card should be archived/closed")
    protected Property<Boolean> closed;

    @Schema(title = "Due Date", description = "A due date for the card")
    protected Property<String> due;

    @Schema(title = "Position", description = "The position of the card. top, bottom, or a positive float")
    protected Property<String> pos;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        String rId = runContext.render(this.cardId).as(String.class).orElseThrow();
        String url = buildApiUrl(runContext, "cards/" + rId);
        String authParams = buildAuthParams(runContext);

        Map<String, Object> updateData = new HashMap<>();

        runContext.render(this.name).as(String.class).ifPresent(val -> updateData.put("name", val));
        runContext.render(this.desc).as(String.class).ifPresent(val -> updateData.put("desc", val));
        runContext.render(this.closed).as(Boolean.class).ifPresent(val -> updateData.put("closed", val));
        runContext.render(this.pos).as(String.class).ifPresent(val -> updateData.put("pos", val));
        runContext.render(this.due).as(String.class).ifPresent(val -> updateData.put("due", val));

        HttpRequest request = HttpRequest.builder()
            .method("PUT")
            .uri(URI.create(url + "?" + authParams))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .body(HttpRequest.StringRequestBody.builder()
                .content(JacksonMapper.ofJson().writeValueAsString(updateData))
                .build())
            .build();

        try (HttpClient httpClient = HttpClient.builder()
            .runContext(runContext)
            .build()) {
            HttpResponse<String> response = httpClient.request(request, String.class);

            if (response.getStatus().getCode() != 200) {
                throw new RuntimeException(
                    "Failed to update card: " + response.getStatus().getCode() + " - "
                        + response.getBody());
            }

            return null;
        }
    }
}
