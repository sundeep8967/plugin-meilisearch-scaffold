package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
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

import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete documents from a Meilisearch index",
    description = "Deletes documents from an index by a list of primary key IDs or by a filter expression. Exactly one of 'ids' or 'filter' must be set."
)
@Plugin(
    examples = {
        @Example(
            title = "Delete documents by primary key ID list.",
            full = true,
            code = """
                id: meilisearch_delete_by_ids
                namespace: company.team

                tasks:
                  - id: delete_documents
                    type: io.kestra.plugin.meilisearch.DeleteDocuments
                    host: http://localhost:7700
                    apiKey: "{{ secret('MEILISEARCH_API_KEY') }}"
                    index: movies
                    ids: ["1", "2"]
                """
        ),
        @Example(
            title = "Delete documents matching a filter expression.",
            full = true,
            code = """
                id: meilisearch_delete_by_filter
                namespace: company.team

                tasks:
                  - id: delete_documents
                    type: io.kestra.plugin.meilisearch.DeleteDocuments
                    host: http://localhost:7700
                    apiKey: "{{ secret('MEILISEARCH_API_KEY') }}"
                    index: movies
                    filter: "genres = 'Horror'"
                """
        )
    }
)
public class DeleteDocuments extends AbstractMeilisearch implements RunnableTask<DeleteDocuments.Output> {
    @Schema(
        title = "Index UID",
        description = "The unique identifier of the target Meilisearch index."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> index;

    @Schema(
        title = "Document IDs to delete",
        description = "A list of document primary key IDs to delete. Mutually exclusive with 'filter'."
    )
    @PluginProperty(group = "main")
    private Property<List<String>> ids;

    @Schema(
        title = "Filter expression",
        description = "A filter expression to delete all matching documents. Mutually exclusive with 'ids'."
    )
    @PluginProperty(group = "main")
    private Property<String> filter;

    @Schema(
        title = "Wait for task completion",
        description = "Whether to wait synchronously for the deletion task to complete."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<Boolean> waitForTask = Property.ofValue(true);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rIndex = runContext.render(this.index).as(String.class).orElseThrow(() -> new IllegalArgumentException("'index' is required"));
        var rIds = runContext.render(this.ids).asList(String.class);
        var rFilter = runContext.render(this.filter).as(String.class).orElse(null);
        var rWaitForTask = runContext.render(this.waitForTask).as(Boolean.class).orElse(true);

        boolean hasIds = !rIds.isEmpty();
        boolean hasFilter = rFilter != null && !rFilter.isBlank();

        if (hasIds == hasFilter) {
            throw new IllegalArgumentException("Exactly one of 'ids' or 'filter' must be specified.");
        }

        Client client = this.buildClient(runContext);
        Index index = client.index(rIndex);

        TaskInfo taskInfo;
        if (hasIds) {
            taskInfo = index.deleteDocuments(rIds);
        } else {
            taskInfo = index.deleteDocumentsByFilter(rFilter);
        }

        String status = taskInfo.getStatus().toString();
        if (Boolean.TRUE.equals(rWaitForTask)) {
            var task = this.waitForTask(client, taskInfo, runContext);
            status = task.getStatus().toString();
        }

        runContext.logger().info("Deleted document(s) from Meilisearch index '{}' (taskUid={}, status={})", rIndex, taskInfo.getTaskUid(), status);

        return Output.builder()
            .index(rIndex)
            .taskUid(taskInfo.getTaskUid())
            .status(status)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Index UID")
        private final String index;

        @Schema(title = "Task UID created in Meilisearch")
        private final Integer taskUid;

        @Schema(title = "Status of the task")
        private final String status;
    }
}
