package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.model.TaskInfo;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a Meilisearch index",
    description = "Creates a new search index in Meilisearch with an optional primary key."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a new movies index with 'id' as the primary key.",
            full = true,
            code = """
                id: meilisearch_create_index
                namespace: company.team

                tasks:
                  - id: create_index
                    type: io.kestra.plugin.meilisearch.CreateIndex
                    host: http://localhost:7700
                    apiKey: "{{ secret('MEILISEARCH_API_KEY') }}"
                    index: movies
                    primaryKey: id
                """
        )
    }
)
public class CreateIndex extends AbstractMeilisearch implements RunnableTask<CreateIndex.Output> {
    @Schema(
        title = "Index UID",
        description = "The unique identifier of the index to create."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> index;

    @Schema(
        title = "Primary key",
        description = "The document attribute that serves as the primary key. If omitted, Meilisearch infers it from the first document."
    )
    @PluginProperty(group = "main")
    private Property<String> primaryKey;

    @Schema(
        title = "Wait for task completion",
        description = "Whether to wait synchronously for the asynchronous indexing task to complete."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<Boolean> waitForTask = Property.ofValue(true);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rIndex = runContext.render(this.index).as(String.class).orElseThrow(() -> new IllegalArgumentException("'index' is required"));
        var rPrimaryKey = runContext.render(this.primaryKey).as(String.class).orElse(null);
        var rWaitForTask = runContext.render(this.waitForTask).as(Boolean.class).orElse(true);

        Client client = this.buildClient(runContext);

        TaskInfo taskInfo;
        if (rPrimaryKey != null && !rPrimaryKey.isBlank()) {
            taskInfo = client.createIndex(rIndex, rPrimaryKey);
        } else {
            taskInfo = client.createIndex(rIndex);
        }

        String status = taskInfo.getStatus().toString();
        if (Boolean.TRUE.equals(rWaitForTask)) {
            var task = this.waitForTask(client, taskInfo, runContext);
            status = task.getStatus().toString();
        }

        runContext.logger().info("Created Meilisearch index '{}' (taskUid={}, status={})", rIndex, taskInfo.getTaskUid(), status);

        return Output.builder()
            .index(rIndex)
            .primaryKey(rPrimaryKey)
            .taskUid(taskInfo.getTaskUid())
            .status(status)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Index UID")
        private final String index;

        @Schema(title = "Primary key attribute name")
        private final String primaryKey;

        @Schema(title = "Task UID created in Meilisearch")
        private final Integer taskUid;

        @Schema(title = "Status of the task")
        private final String status;
    }
}
