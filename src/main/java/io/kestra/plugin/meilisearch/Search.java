package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.SearchResult;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.common.FetchOutput;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Search documents in a Meilisearch index",
    description = "Runs a fast, typo-tolerant full-text and faceted search query against a Meilisearch index."
)
@Plugin(
    examples = {
        @Example(
            title = "Search for movies with typo-tolerance and faceted filtering.",
            full = true,
            code = """
                id: meilisearch_search
                namespace: company.team

                tasks:
                  - id: search_movies
                    type: io.kestra.plugin.meilisearch.Search
                    host: http://localhost:7700
                    apiKey: "{{ secret('MEILISEARCH_API_KEY') }}"
                    index: movies
                    q: "wonder wman"
                    filter: "genres = 'Action'"
                    limit: 10
                    fetchType: FETCH
                """
        )
    }
)
public class Search extends AbstractMeilisearch implements RunnableTask<FetchOutput> {
    @Schema(
        title = "Index UID",
        description = "The unique identifier of the Meilisearch index to search."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> index;

    @Schema(
        title = "Search query",
        description = "The full-text search query string."
    )
    @PluginProperty(group = "main")
    private Property<String> q;

    @Schema(
        title = "Filter expression",
        description = "Filter query to narrow results, e.g. \"genres = 'Action' AND release_year > 2020\"."
    )
    @PluginProperty(group = "processing")
    private Property<String> filter;

    @Schema(
        title = "Result limit",
        description = "Maximum number of search results to return."
    )
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<@Min(1) @Max(1000) Integer> limit = Property.ofValue(20);

    @Schema(
        title = "Result offset",
        description = "Number of results to skip for pagination."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<@Min(0) Integer> offset = Property.ofValue(0);

    @Schema(
        title = "Sort rules",
        description = "Attributes and order by which to sort results, e.g. [\"price:asc\", \"rating:desc\"]."
    )
    @PluginProperty(group = "processing")
    private Property<List<String>> sort;

    @Schema(
        title = "Attributes to retrieve",
        description = "Attributes displayed in the returned documents. Defaults to all attributes [\"*\"]."
    )
    @PluginProperty(group = "processing")
    private Property<List<String>> attributesToRetrieve;

    @Schema(
        title = "Show ranking score",
        description = "Whether to include the `_rankingScore` for each hit in the results."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<Boolean> showRankingScore = Property.ofValue(false);

    @Schema(
        title = "Fetch type",
        description = "The format of the returned search results: inline list (FETCH), single record (FETCH_ONE), or Kestra internal storage URI (STORE)."
    )
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Override
    public FetchOutput run(RunContext runContext) throws Exception {
        var rIndex = runContext.render(this.index).as(String.class).orElseThrow(() -> new IllegalArgumentException("'index' is required"));
        var rQ = runContext.render(this.q).as(String.class).orElse("");
        var rFilter = runContext.render(this.filter).as(String.class).orElse(null);
        var rLimit = runContext.render(this.limit).as(Integer.class).orElse(20);
        var rOffset = runContext.render(this.offset).as(Integer.class).orElse(0);
        var rSort = runContext.render(this.sort).asList(String.class);
        var rAttributes = runContext.render(this.attributesToRetrieve).asList(String.class);
        var rShowScore = runContext.render(this.showRankingScore).as(Boolean.class).orElse(false);
        var rFetchType = runContext.render(this.fetchType).as(FetchType.class).orElse(FetchType.FETCH);

        Client client = this.buildClient(runContext);
        Index index = client.index(rIndex);

        SearchRequest.SearchRequestBuilder requestBuilder = SearchRequest.builder()
            .q(rQ)
            .limit(rLimit)
            .offset(rOffset)
            .showRankingScore(rShowScore);

        if (rFilter != null && !rFilter.isBlank()) {
            requestBuilder.filter(new String[]{rFilter});
        }
        if (!rSort.isEmpty()) {
            requestBuilder.sort(rSort.toArray(new String[0]));
        }
        if (!rAttributes.isEmpty()) {
            requestBuilder.attributesToRetrieve(rAttributes.toArray(new String[0]));
        }

        SearchResult searchResult = (SearchResult) index.search(requestBuilder.build());

        List<HashMap<String, Object>> rawHits = searchResult.getHits();
        List<Map<String, Object>> rows = new ArrayList<>();
        if (rawHits != null) {
            rows.addAll(rawHits);
        }

        runContext.logger().info("Meilisearch search on index '{}' returned {} hit(s) in {} ms", rIndex, rows.size(), searchResult.getProcessingTimeMs());

        return switch (rFetchType) {
            case FETCH_ONE -> FetchOutput.builder()
                .size(rows.isEmpty() ? 0L : 1L)
                .row(rows.isEmpty() ? null : rows.get(0))
                .build();
            case STORE -> {
                var tempFile = runContext.workingDir().createTempFile(".ion").toFile();
                try (var output = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                    for (Map<String, Object> row : rows) {
                        FileSerde.write(output, row);
                    }
                    output.flush();
                }
                yield FetchOutput.builder()
                    .size((long) rows.size())
                    .uri(runContext.storage().putFile(tempFile))
                    .build();
            }
            default -> FetchOutput.builder()
                .size((long) rows.size())
                .rows(new ArrayList<>(rows))
                .build();
        };
    }
}
