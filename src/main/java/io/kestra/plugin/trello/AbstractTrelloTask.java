package io.kestra.plugin.trello;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractTrelloTask extends Task implements RunnableTask<io.kestra.core.models.tasks.Output> {

    @Schema(title = "Trello API Key", description = "Your Trello API key")
    @NotNull
    protected Property<String> apiKey;

    @Schema(title = "Trello API Token", description = "Your Trello API token")
    @NotNull
    protected Property<String> apiToken;

    @Schema(title = "API Version", description = "Trello API version to use", defaultValue = "1")
    @Builder.Default
    protected Property<String> apiVersion = Property.ofValue("1");

    @Schema(title = "Base API URL", description = "The base URL for the Trello API")
    @Builder.Default
    protected Property<String> apiBaseUrl = Property.ofValue("https://api.trello.com");

    protected String buildApiUrl(RunContext runContext, String endpoint) throws Exception {
        String rVersion = runContext.render(this.apiVersion).as(String.class).orElse("1");
        String rBaseUrl = runContext.render(this.apiBaseUrl).as(String.class).orElse("https://api.trello.com");
        return String.format("%s/%s/%s", rBaseUrl, rVersion, endpoint);
    }

    protected HttpRequest.HttpRequestBuilder addAuthHeaders(RunContext runContext, HttpRequest.HttpRequestBuilder builder) throws Exception {
        String rApiKey = runContext.render(this.apiKey).as(String.class).orElseThrow();
        String rApiToken = runContext.render(this.apiToken).as(String.class).orElseThrow();
        String authHeader = String.format("OAuth oauth_consumer_key=\"%s\", oauth_token=\"%s\"", rApiKey, rApiToken);
        return builder.addHeader("Authorization", authHeader);
    }
}
