package io.kestra.plugin.trello.stubs;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Controller("/1")
public class TrelloMockController {

    @Get(uri = "/lists/{listId}/cards", produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> getListCards(String listId) {
        String recentDate = Instant.now().minus(2, ChronoUnit.MINUTES).toString();

        String mockResponse = """
                [
                  {
                    "id": "card123",
                    "name": "Test Card",
                    "desc": "Test Description",
                    "listId": "%s",
                    "idBoard": "board123",
                    "shortUrl": "https://trello.com/c/card123",
                    "dateLastActivity": "%s"
                  }
                ]
                """.formatted(listId, recentDate);

        return HttpResponse.ok(mockResponse).contentType(MediaType.APPLICATION_JSON_TYPE);
    }

    @Get(uri = "/boards/{boardId}/cards", produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> getBoardCards(String boardId) {
        String recentDate = Instant.now().minus(2, ChronoUnit.MINUTES).toString();

        String mockResponse = """
                [
                  {
                    "id": "card456",
                    "name": "Board Test Card",
                    "desc": "Board Test Description",
                    "listId": "list789",
                    "idBoard": "%s",
                    "shortUrl": "https://trello.com/c/card456",
                    "dateLastActivity": "%s"
                  }
                ]
                """.formatted(boardId, recentDate);

        return HttpResponse.ok(mockResponse).contentType(MediaType.APPLICATION_JSON_TYPE);
    }

    @Post(uri = "/cards", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> createCard(@Body String body) {
        String mockResponse = """
                {
                  "id": "test-card-id",
                  "name": "Test Card",
                  "desc": "This is a test card",
                  "listId": "list123",
                  "idBoard": "board123",
                  "shortUrl": "https://trello.com/c/test123",
                  "dateLastActivity": "%s"
                }
                """.formatted(Instant.now().toString());

        return HttpResponse.ok(mockResponse).contentType(MediaType.APPLICATION_JSON_TYPE);
    }

    @Put(uri = "/cards/{cardId}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> updateCard(String cardId, @Body String body) {
        String mockResponse = """
                {
                  "id": "%s",
                  "name": "Updated Card",
                  "desc": "Updated description",
                  "listId": "list456",
                  "idBoard": "board123",
                  "shortUrl": "https://trello.com/c/%s",
                  "dateLastActivity": "%s"
                }
                """.formatted(cardId, cardId, Instant.now().toString());

        return HttpResponse.ok(mockResponse).contentType(MediaType.APPLICATION_JSON_TYPE);
    }

    @Post(uri = "/cards/{cardId}/actions/comments", produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> addComment(String cardId, @QueryValue String text) {
        String mockResponse = """
                {
                  "id": "comment123",
                  "type": "commentCard",
                  "data": {
                    "text": "%s",
                    "card": {
                      "id": "%s"
                    }
                  },
                  "date": "%s"
                }
                """.formatted(text != null ? text : "Test comment", cardId, Instant.now().toString());

        return HttpResponse.ok(mockResponse).contentType(MediaType.APPLICATION_JSON_TYPE);
    }
}
