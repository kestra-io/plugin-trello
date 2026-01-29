package io.kestra.plugin.trello.cards;

import com.fasterxml.jackson.databind.JsonNode;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.triggers.AbstractTrigger;
import io.kestra.core.models.triggers.PollingTriggerInterface;
import io.kestra.core.models.triggers.TriggerContext;
import io.kestra.core.models.triggers.TriggerOutput;
import io.kestra.core.models.triggers.TriggerService;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuperBuilder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode

@Schema(
    title = "Trigger on Trello card creation or update",
    description = "Monitor Trello lists or boards for new or updated cards and trigger executions when detected. "
        + "This trigger polls the Trello API at regular intervals."
)

@Plugin(
    examples = {
        @Example(
            title = "Monitor a list for new or updated cards",
            full = true,
            code = """
                id: trello_card_monitor
                namespace: company.team

                tasks:
                  - id: notify_slack
                    type: io.kestra.plugin.notifications.slack.SlackIncomingWebhook
                    url: "{{ secret('SLACK_WEBHOOK_URL') }}"
                    payload: |
                      {
                        "text": "Card {{ trigger.cardName }} was {{ trigger.action }}: {{ trigger.cardUrl }}"
                      }

                triggers:
                  - id: card_trigger
                    type: io.kestra.plugin.trello.cards.Trigger
                    apiKey: "{{ secret('TRELLO_API_KEY') }}"
                    apiToken: "{{ secret('TRELLO_API_TOKEN') }}"
                    listId: "5abbe4b7ddc1b351ef961414"
                    interval: PT5M
                """
        ),
        @Example(
            title = "Monitor multiple lists for card changes",
            code = """
                triggers:
                  - id: multi_list_trigger
                    type: io.kestra.plugin.trello.cards.Trigger
                    apiKey: "{{ secret('TRELLO_API_KEY') }}"
                    apiToken: "{{ secret('TRELLO_API_TOKEN') }}"
                    listIds:
                      - "5abbe4b7ddc1b351ef961414"
                      - "5abbe4b7ddc1b351ef961415"
                    interval: PT10M
                """
        ),
        @Example(
            title = "Monitor a board for card changes",
            code = """
                triggers:
                  - id: board_trigger
                    type: io.kestra.plugin.trello.cards.Trigger
                    apiKey: "{{ secret('TRELLO_API_KEY') }}"
                    apiToken: "{{ secret('TRELLO_API_TOKEN') }}"
                    boardId: "5abbe4b7ddc1b351ef961416"
                    interval: PT15M
                """
        )
    }
)
public class Trigger extends AbstractTrigger
        implements PollingTriggerInterface, TriggerOutput<Trigger.Output> {

    @Schema(title = "Trello API Key", description = "Your Trello API key")
    @NotNull
    protected Property<String> apiKey;

    @Schema(title = "Trello API Token", description = "Your Trello API token")
    @NotNull
    protected Property<String> apiToken;

    @Schema(title = "API Version", description = "Trello API version to use")
    @Builder.Default
    protected Property<String> apiVersion = Property.ofValue("1");

    @Schema(title = "Base API URL", description = "The base URL for the Trello API")
    @Builder.Default
    protected Property<String> apiBaseUrl = Property.ofValue("https://api.trello.com");

    @Schema(title = "List ID", description = "Single Trello list ID to monitor for card changes")
    protected Property<String> listId;

    @Schema(title = "List IDs", description = "Multiple Trello list IDs to monitor for card changes")
    protected Property<List<String>> listIds;

    @Schema(title = "Board ID", description = "Trello board ID to monitor for card changes across all lists")
    protected Property<String> boardId;

    @Schema(title = "Polling interval", description = "How often to check for new or updated cards")
    @PluginProperty
    @Builder.Default
    private Duration interval = Duration.ofMinutes(5);

    @Override
    public Duration getInterval() {
        return this.interval;
    }

    @Override
    public Optional<Execution> evaluate(ConditionContext conditionContext, TriggerContext context) throws Exception {
        RunContext runContext = conditionContext.getRunContext();

        String rApiKey = runContext.render(this.apiKey).as(String.class).orElseThrow();
        String rApiToken = runContext.render(this.apiToken).as(String.class).orElseThrow();
        String rVersion = runContext.render(this.apiVersion).as(String.class).orElse("1");
        String rBaseUrl = runContext.render(this.apiBaseUrl).as(String.class).orElse("https://api.trello.com");

        // Determine which endpoint to use
        List<String> listsToMonitor = new ArrayList<>();

        if (this.listId != null) {
            String rListId = runContext.render(this.listId).as(String.class).orElse(null);
            if (rListId != null) {
                listsToMonitor.add(rListId);
            }
        }

        if (this.listIds != null) {
            List<String> rListIds = runContext.render(this.listIds).asList(String.class);
            listsToMonitor.addAll(rListIds);
        }

        runContext.logger().info("Monitoring {} lists for card changes", listsToMonitor.size());

        // Calculate last check time based on trigger context
        Instant lastCheckTime = context.getNextExecutionDate() != null
                ? context.getNextExecutionDate().toInstant().minus(this.interval)
                : Instant.now().minus(this.interval);

        List<CardData> newOrUpdatedCards = new ArrayList<>();

        try (HttpClient httpClient = HttpClient.builder()
                .runContext(runContext)
                .build()) {

            // If boardId is specified, get all cards from the board
            if (this.boardId != null) {
                String rBoardId = runContext.render(this.boardId).as(String.class).orElse(null);
                if (rBoardId != null) {
                    newOrUpdatedCards.addAll(getCardsFromBoard(runContext, httpClient, rBaseUrl, rVersion,
                            rApiKey, rApiToken, rBoardId, lastCheckTime));
                }
            }

            // Get cards from specified lists
            for (String listId : listsToMonitor) {
                newOrUpdatedCards.addAll(getCardsFromList(runContext, httpClient, rBaseUrl, rVersion,
                        rApiKey, rApiToken, listId, lastCheckTime));
            }
        }

        if (newOrUpdatedCards.isEmpty()) {
            runContext.logger().info("No new or updated cards found");
            return Optional.empty();
        }

        runContext.logger().info("Found {} new or updated cards", newOrUpdatedCards.size());

        // Get the most recent card for the output
        CardData latest = newOrUpdatedCards.stream()
                .max((c1, c2) -> c1.getLastActivity().compareTo(c2.getLastActivity()))
                .orElse(newOrUpdatedCards.get(0));

        Output output = Output.builder()
                .cardId(latest.getCardId())
                .cardName(latest.getCardName())
                .cardUrl(latest.getCardUrl())
                .cardDescription(latest.getCardDescription())
                .listId(latest.getListId())
                .boardId(latest.getBoardId())
                .lastActivity(latest.getLastActivity())
                .action(latest.getAction())
                .newCardsCount(newOrUpdatedCards.size())
                .allNewCards(newOrUpdatedCards)
                .build();

        Execution execution = TriggerService.generateExecution(this, conditionContext, context, output);

        return Optional.of(execution);
    }

    private List<CardData> getCardsFromBoard(RunContext runContext, HttpClient httpClient, String baseUrl,
            String version, String apiKey, String apiToken,
            String boardId, Instant lastCheckTime) throws Exception {
        String url = buildApiUrl(baseUrl, version, "boards/" + boardId + "/cards");
        return fetchAndFilterCards(runContext, httpClient, url, apiKey, apiToken, lastCheckTime, boardId, null);
    }

    private List<CardData> getCardsFromList(RunContext runContext, HttpClient httpClient, String baseUrl,
            String version, String apiKey, String apiToken,
            String listId, Instant lastCheckTime) throws Exception {
        String url = buildApiUrl(baseUrl, version, "lists/" + listId + "/cards");
        return fetchAndFilterCards(runContext, httpClient, url, apiKey, apiToken, lastCheckTime, null, listId);
    }

    private List<CardData> fetchAndFilterCards(RunContext runContext, HttpClient httpClient, String url,
            String apiKey, String apiToken, Instant lastCheckTime,
            String boardId, String listId) throws Exception {
        List<CardData> results = new ArrayList<>();

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
                .method("GET")
                .uri(URI.create(url))
                .addHeader("Accept", "application/json");

        HttpRequest request = addAuthHeaders(apiKey, apiToken, requestBuilder).build();

        HttpResponse<String> response = httpClient.request(request, String.class);

        if (response.getStatus().getCode() != 200) {
            throw new RuntimeException(
                    "Failed to fetch cards: " + response.getStatus().getCode() + " - " + response.getBody());
        }

        JsonNode cardsArray = JacksonMapper.ofJson().readTree(response.getBody());

        if (cardsArray.isArray()) {
            for (JsonNode cardNode : cardsArray) {
                CardData cardData = parseCardData(cardNode, lastCheckTime, boardId, listId);
                if (cardData != null) {
                    results.add(cardData);
                }
            }
        }

        return results;
    }

    private CardData parseCardData(JsonNode cardNode, Instant lastCheckTime, String boardIdOverride,
            String listIdOverride) {
        if (!cardNode.has("dateLastActivity")) {
            return null;
        }

        String dateLastActivityStr = cardNode.get("dateLastActivity").asText();
        Instant lastActivity = Instant.parse(dateLastActivityStr);

        // Only include cards that have activity after the last check
        if (!lastActivity.isAfter(lastCheckTime)) {
            return null;
        }

        String cardId = cardNode.has("id") ? cardNode.get("id").asText() : null;
        String cardName = cardNode.has("name") ? cardNode.get("name").asText() : null;
        String cardUrl = cardNode.has("shortUrl") ? cardNode.get("shortUrl").asText() : null;
        String cardDesc = cardNode.has("desc") ? cardNode.get("desc").asText() : null;
        String listId = listIdOverride != null ? listIdOverride
                : (cardNode.has("idList") ? cardNode.get("idList").asText() : null);
        String boardId = boardIdOverride != null ? boardIdOverride
                : (cardNode.has("idBoard") ? cardNode.get("idBoard").asText() : null);

        // Determine if it's a new card or an update based on creation date
        String action = "updated";
        if (cardNode.has("dateLastActivity")) {
            // If the card was created very recently (within a minute of last activity),
            // consider it new
            Instant creationTime = lastActivity;
            if (lastActivity.minusSeconds(60).isBefore(lastCheckTime)) {
                action = "created";
            }
        }

        return CardData.builder()
                .cardId(cardId)
                .cardName(cardName)
                .cardUrl(cardUrl)
                .cardDescription(cardDesc)
                .listId(listId)
                .boardId(boardId)
                .lastActivity(lastActivity)
                .action(action)
                .build();
    }

    private String buildApiUrl(String baseUrl, String version, String endpoint) {
        return String.format("%s/%s/%s", baseUrl, version, endpoint);
    }

    private HttpRequest.HttpRequestBuilder addAuthHeaders(String apiKey, String apiToken,
            HttpRequest.HttpRequestBuilder builder) {
        String authHeader = String.format("OAuth oauth_consumer_key=\"%s\", oauth_token=\"%s\"", apiKey, apiToken);
        return builder.addHeader("Authorization", authHeader);
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Card ID")
        private final String cardId;

        @Schema(title = "Card Name")
        private final String cardName;

        @Schema(title = "Card URL")
        private final String cardUrl;

        @Schema(title = "Card Description")
        private final String cardDescription;

        @Schema(title = "List ID")
        private final String listId;

        @Schema(title = "Board ID")
        private final String boardId;

        @Schema(title = "Last Activity Time")
        private final Instant lastActivity;

        @Schema(title = "Action", description = "Whether the card was 'created' or 'updated'")
        private final String action;

        @Schema(title = "Total number of new or updated cards found")
        private final Integer newCardsCount;

        @Schema(title = "All new or updated cards found")
        private final List<CardData> allNewCards;
    }

    @Builder
    @Getter
    public static class CardData {
        @Schema(title = "Card ID")
        private final String cardId;

        @Schema(title = "Card Name")
        private final String cardName;

        @Schema(title = "Card URL")
        private final String cardUrl;

        @Schema(title = "Card Description")
        private final String cardDescription;

        @Schema(title = "List ID")
        private final String listId;

        @Schema(title = "Board ID")
        private final String boardId;

        @Schema(title = "Last Activity Time")
        private final Instant lastActivity;

        @Schema(title = "Action Type")
        private final String action;
    }
}
