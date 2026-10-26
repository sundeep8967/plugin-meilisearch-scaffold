package io.kestra.plugin.meilisearch;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public interface MeilisearchConnectionInterface {
    @Schema(
        title = "Meilisearch host URL",
        description = "The URL of the Meilisearch instance, e.g. http://localhost:7700 or a Meilisearch Cloud endpoint."
    )
    @NotNull
    @PluginProperty(group = "connection")
    Property<String> getHost();

    @Schema(
        title = "API key",
        description = "The master key or API key for authenticating requests with Meilisearch."
    )
    @PluginProperty(group = "connection", secret = true)
    Property<String> getApiKey();

    @Schema(
        title = "Client timeout",
        description = "Timeout in seconds for requests sent to Meilisearch."
    )
    @PluginProperty(group = "connection")
    Property<Integer> getClientTimeout();
}
