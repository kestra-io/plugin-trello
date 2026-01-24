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
    title = "Create a new Trello card",
    description = "Create a new card in a specified Trello list"
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
                    idList: "5abbe4b7ddc1b351ef961414"
                    desc: "This is the card description"
                """
        )
    }
)
public class Create extends AbstractTrelloTask {

    @Schema(title = "Card Name", description = "The name for the card")
    @NotNull
    protected Property<String> name;

    @Schema(title = "List ID", description = "The ID of the list where the card will be created")
    @NotNull
    protected Property<String> idList;

    @Schema(title = "Description", description = "The description for the card")
    protected Property<String> desc;

    @Schema(title = "Position", description = "The position of the new card. top, bottom, or a positive float")
    protected Property<String> pos;

    @Schema(title = "Due Date", description = "A due date for the card")
    protected Property<String> due;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        String url = buildApiUrl(runContext, "cards");
        String authParams = buildAuthParams(runContext);

        Map<String, Object> cardData = new HashMap<>();

        String rName = runContext.render(this.name).as(String.class).orElseThrow();
        String rIdList = runContext.render(this.idList).as(String.class).orElseThrow();

        cardData.put("name", rName);
        cardData.put("idList", rIdList);

        runContext.render(this.desc).as(String.class).ifPresent(val -> cardData.put("desc", val));
        runContext.render(this.pos).as(String.class).ifPresent(val -> cardData.put("pos", val));
        runContext.render(this.due).as(String.class).ifPresent(val -> cardData.put("due", val));

        HttpRequest request = HttpRequest.builder()
            .method("POST")
            .uri(URI.create(url + "?" + authParams))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .body(HttpRequest.StringRequestBody.builder()
                .content(JacksonMapper.ofJson().writeValueAsString(cardData))
                .build())
            .build();

        try (HttpClient httpClient = HttpClient.builder()
            .runContext(runContext)
            .build()) {
            HttpResponse<String> response = httpClient.request(request, String.class);

            if (response.getStatus().getCode() != 200) {
                throw new RuntimeException(
                    "Failed to create card: " + response.getStatus().getCode() + " - "
                        + response.getBody());
            }

            return null;
        }
    }
}
