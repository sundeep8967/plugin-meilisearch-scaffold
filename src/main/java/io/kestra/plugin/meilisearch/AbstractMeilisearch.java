package io.kestra.plugin.meilisearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractMeilisearch extends Task implements MeilisearchConnectionInterface {
    protected static final ObjectMapper MAPPER = JacksonMapper.ofJson();

    @Schema(
        title = "Meilisearch host URL",
        description = "The URL of the Meilisearch instance, e.g. http://localhost:7700 or a Meilisearch Cloud endpoint."
    )
    @NotNull
    @PluginProperty(group = "connection")
    protected Property<String> host;

    @Schema(
        title = "API key",
        description = "The master key or API key for authenticating requests with Meilisearch."
    )
    @PluginProperty(group = "connection", secret = true)
    @ToString.Exclude
    protected Property<String> apiKey;

    @Schema(
        title = "Client timeout",
        description = "Timeout in seconds for requests sent to Meilisearch."
    )
    @PluginProperty(group = "connection")
    protected Property<Integer> clientTimeout;

    protected Client buildClient(RunContext runContext) throws IllegalVariableEvaluationException {
        var rHost = runContext.render(this.host).as(String.class).orElseThrow(() -> new IllegalArgumentException("'host' is required"));
        var rApiKey = runContext.render(this.apiKey).as(String.class).orElse("");

        Config config = new Config(rHost, rApiKey);
        return new Client(config);
    }

    protected com.meilisearch.sdk.model.Task waitForTask(Client client, TaskInfo taskInfo, RunContext runContext) throws MeilisearchException {
        int taskUid = taskInfo.getTaskUid();
        client.waitForTask(taskUid);
        com.meilisearch.sdk.model.Task task = client.getTask(taskUid);
        if (task.getStatus() == TaskStatus.FAILED) {
            String error = task.getError() != null ? task.getError().getMessage() : "Unknown Meilisearch error";
            throw new MeilisearchException("Meilisearch task " + taskUid + " failed: " + error);
        }
        runContext.logger().debug("Meilisearch task {} completed with status {}", taskUid, task.getStatus());
        return task;
    }
}
