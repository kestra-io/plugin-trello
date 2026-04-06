package io.kestra.plugin.trello.cards;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.triggers.*;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@Schema(
    title = "Poll Trello cards for changes",
    description = "Polls Trello list and board card endpoints and triggers an execution when `dateLastActivity` is newer than the previous interval window. Defaults to `PT5M`; if you set both `boardId` and `lists`, both sources are polled and the same card can appear more than once"
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
                        "text": "Card {{ trigger.cards[0].cardName }} was {{ trigger.cards[0].action }}: {{ trigger.cards[0].cardUrl }}"
                      }

                triggers:
                  - id: card_trigger
                    type: io.kestra.plugin.trello.cards.Trigger
                    apiKey: "{{ secret('TRELLO_API_KEY') }}"
                    apiToken: "{{ secret('TRELLO_API_TOKEN') }}"
                    lists:
                      - "5abbe4b7ddc1b351ef961414"
                    interval: PT5M
                """
        ),
        @Example(
            title = "Monitor multiple lists for card changes",
            full = true,
            code = """
                id: trello_monitor
                namespace: company.team

                tasks:
                  - id: notify_slack
                    type: io.kestra.plugin.notifications.slack.SlackIncomingWebhook
                    url: "{{ secret('SLACK_WEBHOOK_URL') }}"
                    payload: |
                      {
                        "text": "Card {{ trigger.cards[0].cardName }} was {{ trigger.cards[0].action }}: {{ trigger.cards[0].cardUrl }}"
                      }

                triggers:
                  - id: multi_list_trigger
                    type: io.kestra.plugin.trello.cards.Trigger
                    apiKey: "{{ secret('TRELLO_API_KEY') }}"
                    apiToken: "{{ secret('TRELLO_API_TOKEN') }}"
                    lists:
                      - "5abbe4b7ddc1b351ef961414"
                      - "5abbe4b7ddc1b351ef961415"
                    interval: PT10M
                """
        ),
        @Example(
            title = "Monitor a board for card changes",
            full = true,
            code = """
                id: trello_board_monitor
                namespace: company.team

                tasks:
                  - id: notify_slack
                    type: io.kestra.plugin.notifications.slack.SlackIncomingWebhook
                    url: "{{ secret('SLACK_WEBHOOK_URL') }}"
                    payload: |
                      {
                        "text": "Card {{ trigger.cards[0].cardName }} was {{ trigger.cards[0].action }}: {{ trigger.cards[0].cardUrl }}"
                      }

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
public class Trigger extends AbstractTrigger implements PollingTriggerInterface, TriggerOutput<Trigger.Output> {

    @Schema(title = "Trello API Key", description = "API key used to authenticate Trello requests. Render this from a secret")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> apiKey;

    @Schema(title = "Trello API Token", description = "API token used to authenticate Trello requests. Render this from a secret")
    @NotNull
    @PluginProperty(group = "main")
    protected Property<String> apiToken;

    @Schema(title = "API Version", description = "Trello REST API version appended to the base URL. Defaults to `1`")
    @Builder.Default
    @PluginProperty(group = "advanced")
    protected Property<String> apiVersion = Property.ofValue("1");

    @Schema(title = "Base API URL", description = "Base URL for Trello API requests. Defaults to `https://api.trello.com`; override only for compatible proxies or tests")
    @Builder.Default
    @PluginProperty(group = "connection")
    protected Property<String> apiBaseUrl = Property.ofValue("https://api.trello.com");

    @Schema(title = "List IDs", description = "Trello list IDs to poll for new or updated cards")
    @PluginProperty(group = "advanced")
    protected Property<List<String>> lists;

    @Schema(title = "Board ID", description = "Trello board ID to poll across all lists on the board")
    @PluginProperty(group = "advanced")
    protected Property<String> boardId;

    @Schema(title = "Polling Interval", description = "Time between Trello checks. Defaults to `PT5M`")
    @PluginProperty(group = "execution")
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

        List<String> rListIds = runContext.render(this.lists).asList(String.class);
        List<String> listsToMonitor = new ArrayList<>(rListIds);

        runContext.logger().info("Monitoring {} lists for card changes", listsToMonitor.size());

        // Calculate last check time based on trigger context
        Instant lastCheckTime = context.getNextExecutionDate() != null
            ? context.getNextExecutionDate().toInstant().minus(this.interval)
            : Instant.now().minus(this.interval);

        List<CardData> newOrUpdatedCards = new ArrayList<>();

        try (
            HttpClient httpClient = HttpClient.builder()
                .runContext(runContext)
                .build()
        ) {

            // If boardId is specified, get all cards from the board
            if (this.boardId != null) {
                String rBoardId = runContext.render(this.boardId).as(String.class).orElse(null);
                if (rBoardId != null) {
                    newOrUpdatedCards.addAll(
                        getCardsFromBoard(
                            runContext, httpClient, rBaseUrl, rVersion,
                            rApiKey, rApiToken, rBoardId, lastCheckTime
                        )
                    );
                }
            }

            // Get cards from specified lists
            for (String listId : listsToMonitor) {
                newOrUpdatedCards.addAll(
                    getCardsFromList(
                        runContext, httpClient, rBaseUrl, rVersion,
                        rApiKey, rApiToken, listId, lastCheckTime
                    )
                );
            }
        }

        if (newOrUpdatedCards.isEmpty()) {
            runContext.logger().info("No new or updated cards found");
            return Optional.empty();
        }

        runContext.logger().info("Found {} new or updated cards", newOrUpdatedCards.size());

        Output output = Output.builder()
            .cards(newOrUpdatedCards)
            .build();

        Execution execution = TriggerService.generateExecution(this, conditionContext, context, output);

        return Optional.of(execution);
    }

    private List<CardData> getCardsFromBoard(RunContext runContext, HttpClient httpClient, String baseUrl,
        String version, String apiKey, String apiToken,
        String boardId, Instant lastCheckTime) throws Exception {
        String url = buildApiUrl(baseUrl, version, "boards/" + boardId + "/cards");
        return fetchAndFilterCards(runContext, httpClient, url, apiKey, apiToken, lastCheckTime);
    }

    private List<CardData> getCardsFromList(RunContext runContext, HttpClient httpClient, String baseUrl,
        String version, String apiKey, String apiToken,
        String listId, Instant lastCheckTime) throws Exception {
        String url = buildApiUrl(baseUrl, version, "lists/" + listId + "/cards");
        return fetchAndFilterCards(runContext, httpClient, url, apiKey, apiToken, lastCheckTime);
    }

    private List<CardData> fetchAndFilterCards(RunContext runContext, HttpClient httpClient, String url,
        String apiKey, String apiToken, Instant lastCheckTime) throws Exception {
        List<CardData> results = new ArrayList<>();

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .method("GET")
            .uri(URI.create(url))
            .addHeader("Accept", "application/json");

        HttpRequest request = addAuthHeaders(apiKey, apiToken, requestBuilder).build();

        HttpResponse<String> response = httpClient.request(request, String.class);

        if (response.getStatus().getCode() != 200) {
            throw new RuntimeException(
                "Failed to fetch cards: " + response.getStatus().getCode() + " - " + response.getBody()
            );
        }

        JsonNode cardsArray = JacksonMapper.ofJson().readTree(response.getBody());

        if (cardsArray.isArray()) {
            for (JsonNode cardNode : cardsArray) {
                CardData cardData = parseCardData(cardNode, lastCheckTime);
                if (cardData != null) {
                    results.add(cardData);
                }
            }
        }

        return results;
    }

    private CardData parseCardData(JsonNode cardNode, Instant lastCheckTime) {
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
        String listId = cardNode.has("idList") ? cardNode.get("idList").asText() : null;
        String boardId = cardNode.has("idBoard") ? cardNode.get("idBoard").asText() : null;
        // Determine if it's a new card or an update based on creation date
        String action = "updated";
        if (cardNode.has("dateLastActivity")) {
            // If the card was created very recently (within a minute of last activity),
            if (lastActivity.minusSeconds(60).isBefore(lastCheckTime)) {
                action = "created";
            }
        }

        return CardData.builder()
            .cardId(cardId)
            .cardName(cardName)
            .cardUrl(cardUrl)
            .cardDescription(cardDesc)
            .lastActivity(lastActivity)
            .listId(listId)
            .boardId(boardId)
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
        @Schema(title = "Detected Card Count", description = "Declared count field. This trigger currently returns card details in `cards`")
        private final Integer count;

        @Schema(title = "Detected Cards", description = "Cards matched in this polling window")
        private final List<CardData> cards;
    }

    @Builder
    @Getter
    public static class CardData {
        @Schema(title = "Detected Card ID", description = "Trello card ID")
        @PluginProperty(group = "advanced")
        private final String cardId;

        @Schema(title = "Detected Card Name", description = "Trello card name")
        @PluginProperty(group = "advanced")
        private final String cardName;

        @Schema(title = "Detected Card URL", description = "Short Trello URL for the card")
        @PluginProperty(group = "connection")
        private final String cardUrl;

        @Schema(title = "Detected Card Description", description = "Card description returned by Trello")
        @PluginProperty(group = "advanced")
        private final String cardDescription;

        @Schema(title = "Last Activity Time", description = "Latest Trello activity timestamp used for filtering")
        @PluginProperty(group = "advanced")
        private final Instant lastActivity;

        @Schema(title = "Detected Action Type", description = "Derived action label: `created` or `updated`")
        @PluginProperty(group = "advanced")
        private final String action;

        @Schema(title = "Detected List ID", description = "List ID returned by Trello")
        @PluginProperty(group = "advanced")
        private final String listId;

        @Schema(title = "Detected Board ID", description = "Board ID returned by Trello")
        @PluginProperty(group = "advanced")
        private final String boardId;
    }
}
