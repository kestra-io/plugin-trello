package io.kestra.plugin.trello;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller("/1")
@Requires(property = "mock.trello.enabled", value = "true", defaultValue = "true")
public class MockTrelloApiServer {

    private static final Map<String, Map<String, Object>> cards = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        Map<String, Object> presetCard = new HashMap<>();
        presetCard.put("id", "test-card-id");
        presetCard.put("name", "Test Card");
        presetCard.put("idList", "list123");
        presetCard.put("url", "https://trello.com/c/test-card-id");
        cards.put("test-card-id", presetCard);
    }

    @Post("/cards")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<String> createCard(@Body String body, @Header("Authorization") String auth) {
        try {
            Map cardData = objectMapper.readValue(body, Map.class);
            String cardId = "card_" + System.currentTimeMillis();

            Map<String, Object> card = new HashMap<>(cardData);
            card.put("id", cardId);
            card.put("url", "https://trello.com/c/" + cardId);

            cards.put(cardId, card);

            return HttpResponse.ok(objectMapper.writeValueAsString(card));
        } catch (Exception e) {
            return HttpResponse.serverError("Error creating card: " + e.getMessage());
        }
    }

    @Put("/cards/{cardId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<String> updateCard(@PathVariable String cardId, @Body String body,
                                           @Header("Authorization") String auth) {
        try {
            Map<String, Object> existingCard = cards.get(cardId);
            if (existingCard == null) {
                return HttpResponse.notFound("Card not found");
            }

            Map<String, Object> updateData = objectMapper.readValue(body, Map.class);
            existingCard.putAll(updateData);

            return HttpResponse.ok(objectMapper.writeValueAsString(existingCard));
        } catch (Exception e) {
            return HttpResponse.serverError("Error updating card: " + e.getMessage());
        }
    }

    @Post("/cards/{cardId}/actions/comments")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<String> addComment(@PathVariable String cardId, @Header("Authorization") String auth,
                                           @QueryValue String text) {
        try {
            Map<String, Object> existingCard = cards.get(cardId);
            if (existingCard == null) {
                return HttpResponse.notFound("Card not found");
            }

            String actionId = "action_" + System.currentTimeMillis();
            Map<String, Object> action = new HashMap<>();
            action.put("id", actionId);
            action.put("type", "commentCard");

            Map<String, Object> data = new HashMap<>();
            data.put("text", text);
            data.put("card", Map.of("id", cardId, "name", existingCard.get("name")));
            action.put("data", data);

            return HttpResponse.ok(objectMapper.writeValueAsString(action));
        } catch (Exception e) {
            return HttpResponse.serverError("Error adding comment: " + e.getMessage());
        }
    }

    @Get("/cards/{cardId}")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<String> getCard(@PathVariable String cardId, @Header("Authorization") String auth) {
        try {
            Map<String, Object> card = cards.get(cardId);
            if (card == null) {
                return HttpResponse.notFound("Card not found");
            }

            return HttpResponse.ok(objectMapper.writeValueAsString(card));
        } catch (Exception e) {
            return HttpResponse.serverError("Error getting card: " + e.getMessage());
        }
    }
}
