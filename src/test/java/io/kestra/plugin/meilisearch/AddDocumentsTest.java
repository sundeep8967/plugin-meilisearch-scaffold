package io.kestra.plugin.meilisearch;

import io.kestra.core.models.property.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class AddDocumentsTest extends MeilisearchTest {

    @BeforeEach
    void setUp() throws Exception {
        if (!isMeilisearchAvailable()) return;
        var client = meilisearchClient();
        client.createIndex(indexUid, "id");
    }

    @Test
    void run() throws Exception {
        assumeTrue(isMeilisearchAvailable());
        var runContext = runContextFactory.of();

        List<Map<String, Object>> documents = List.of(
            Map.of("id", "1", "title", "Inception", "genre", "Sci-Fi"),
            Map.of("id", "2", "title", "Interstellar", "genre", "Sci-Fi"),
            Map.of("id", "3", "title", "The Dark Knight", "genre", "Action")
        );

        var task = AddDocuments.builder()
            .host(Property.ofValue(host))
            .apiKey(Property.ofValue(apiKey))
            .index(Property.ofValue(indexUid))
            .documents(documents)
            .batchSize(Property.ofValue(2))
            .waitForTask(Property.ofValue(true))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getDocumentCount(), is(3L));
        assertThat(output.getTaskUids(), hasSize(2));

        var client = meilisearchClient();
        var index = client.getIndex(indexUid);
        var searchResult = index.search("");
        assertThat(searchResult.getEstimatedTotalHits(), is(3));
    }

    @AfterEach
    void tearDown() {
        if (!isMeilisearchAvailable()) return;
        var client = meilisearchClient();
        try {
            client.deleteIndex(indexUid);
        } catch (Exception ignored) {
        }
    }
}
