package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.TaskInfo;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Add or replace documents in a Meilisearch index",
    description = "Adds new documents or replaces existing documents matching the primary key in a Meilisearch index. Reads from an inline list of maps or from a Kestra internal storage URI."
)
@Plugin(
    examples = {
        @Example(
            title = "Add documents to a movies index with automatic batching.",
            full = true,
            code = """
                id: meilisearch_add_documents
                namespace: company.team

                tasks:
                  - id: add_documents
                    type: io.kestra.plugin.meilisearch.AddDocuments
                    host: http://localhost:7700
                    apiKey: "{{ secret('MEILISEARCH_API_KEY') }}"
                    index: movies
                    documents:
                      - id: 1
                        title: "Carol"
                        genres: ["Romance", "Drama"]
                      - id: 2
                        title: "Wonder Woman"
                        genres: ["Action", "Adventure"]
                """
        )
    }
)
public class AddDocuments extends AbstractMeilisearch implements RunnableTask<AddDocuments.Output> {
    @Schema(
        title = "Index UID",
        description = "The unique identifier of the target Meilisearch index."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> index;

    @Schema(
        title = "Documents to add or replace",
        description = "Document data source, either an inline list of maps or a Kestra internal storage URI to an ION file.",
        anyOf = {String.class, List.class, Map.class}
    )
    @NotNull
    @PluginProperty(group = "main")
    private Object documents;

    @Schema(
        title = "Primary key",
        description = "Optional primary key attribute name to assign to the index if it does not have one."
    )
    @PluginProperty(group = "main")
    private Property<String> primaryKey;

    @Schema(
        title = "Batch size",
        description = "The maximum number of documents sent per indexing request."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<@Min(1) @Max(10000) Integer> batchSize = Property.ofValue(1000);

    @Schema(
        title = "Wait for task completion",
        description = "Whether to wait synchronously for the indexing tasks to complete before finishing the execution."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<Boolean> waitForTask = Property.ofValue(true);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rIndex = runContext.render(this.index).as(String.class).orElseThrow(() -> new IllegalArgumentException("'index' is required"));
        var rPrimaryKey = runContext.render(this.primaryKey).as(String.class).orElse(null);
        var rBatchSize = runContext.render(this.batchSize).as(Integer.class).orElse(1000);
        var rWaitForTask = runContext.render(this.waitForTask).as(Boolean.class).orElse(true);

        Client client = this.buildClient(runContext);
        Index index = client.index(rIndex);

        var docIterable = Data.from(this.documents).read(runContext).toIterable();

        long documentCount = 0;
        List<Integer> taskUids = new ArrayList<>();
        List<Map<String, Object>> batch = new ArrayList<>(rBatchSize);

        for (Map<String, Object> doc : docIterable) {
            batch.add(doc);
            documentCount++;
            if (batch.size() >= rBatchSize) {
                TaskInfo info = sendBatch(index, batch, rPrimaryKey);
                taskUids.add(info.getTaskUid());
                if (Boolean.TRUE.equals(rWaitForTask)) {
                    this.waitForTask(client, info, runContext);
                }
                batch = new ArrayList<>(rBatchSize);
            }
        }

        if (!batch.isEmpty()) {
            TaskInfo info = sendBatch(index, batch, rPrimaryKey);
            taskUids.add(info.getTaskUid());
            if (Boolean.TRUE.equals(rWaitForTask)) {
                this.waitForTask(client, info, runContext);
            }
        }

        runContext.logger().info("Indexed {} document(s) in Meilisearch index '{}' (batches={})", documentCount, rIndex, taskUids.size());

        return Output.builder()
            .index(rIndex)
            .documentCount(documentCount)
            .taskUids(taskUids)
            .build();
    }

    private TaskInfo sendBatch(Index index, List<Map<String, Object>> batch, String primaryKey) throws Exception {
        String json = MAPPER.writeValueAsString(batch);
        if (primaryKey != null && !primaryKey.isBlank()) {
            return index.addDocuments(json, primaryKey);
        } else {
            return index.addDocuments(json);
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Index UID")
        private final String index;

        @Schema(title = "Total number of documents indexed")
        private final long documentCount;

        @Schema(title = "Task UIDs triggered for the indexing batches")
        private final List<Integer> taskUids;
    }
}
