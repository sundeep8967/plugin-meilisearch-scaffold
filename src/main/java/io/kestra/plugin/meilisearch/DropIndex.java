package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.model.TaskInfo;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
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
    title = "Delete a Meilisearch index",
    description = "Permanently deletes an index and all associated documents in Meilisearch. This operation cannot be undone."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a temporary search index.",
            full = true,
            code = """
                id: meilisearch_drop_index
                namespace: company.team

                tasks:
                  - id: drop_index
                    type: io.kestra.plugin.meilisearch.DropIndex
                    host: http://localhost:7700
                    apiKey: "{{ secret('MEILISEARCH_API_KEY') }}"
                    index: temp_movies
                """
        )
    }
)
public class DropIndex extends AbstractMeilisearch implements RunnableTask<VoidOutput> {
    @Schema(
        title = "Index UID",
        description = "The unique identifier of the index to delete."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> index;

    @Schema(
        title = "Wait for task completion",
        description = "Whether to wait synchronously for the index deletion task to complete."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<Boolean> waitForTask = Property.ofValue(true);

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var rIndex = runContext.render(this.index).as(String.class).orElseThrow(() -> new IllegalArgumentException("'index' is required"));
        var rWaitForTask = runContext.render(this.waitForTask).as(Boolean.class).orElse(true);

        Client client = this.buildClient(runContext);
        TaskInfo taskInfo = client.deleteIndex(rIndex);

        if (Boolean.TRUE.equals(rWaitForTask)) {
            var task = this.waitForTask(client, taskInfo, runContext);
            runContext.logger().info("Deleted Meilisearch index '{}' (taskUid={}, status={})", rIndex, taskInfo.getTaskUid(), task.getStatus());
        } else {
            runContext.logger().info("Triggered deletion of Meilisearch index '{}' (taskUid={})", rIndex, taskInfo.getTaskUid());
        }

        return null;
    }
}
