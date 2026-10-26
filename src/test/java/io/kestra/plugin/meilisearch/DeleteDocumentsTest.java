package io.kestra.plugin.meilisearch;

import io.kestra.core.models.property.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class DeleteDocumentsTest extends MeilisearchTest {

    @BeforeEach
    void setUp() throws Exception {
        if (!isMeilisearchAvailable()) return;
        var client = meilisearchClient();
        client.createIndex(indexUid, "id");
        var index = client.index(indexUid);

        List<Map<String, Object>> documents = List.of(
            Map.of("id", "1", "title", "The Matrix"),
            Map.of("id", "2", "title", "The Matrix Reloaded"),
            Map.of("id", "3", "title", "Gladiator")
        );
        var taskInfo = index.addDocuments(AbstractMeilisearch.MAPPER.writeValueAsString(documents));
        client.waitForTask(taskInfo.getTaskUid());
    }

    @Test
    void runDeleteByIds() throws Exception {
        assumeTrue(isMeilisearchAvailable());
        var runContext = runContextFactory.of();

        var task = DeleteDocuments.builder()
            .host(Property.ofValue(host))
            .apiKey(Property.ofValue(apiKey))
            .index(Property.ofValue(indexUid))
            .ids(Property.ofValue(List.of("1", "2")))
            .waitForTask(Property.ofValue(true))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getTaskUid(), notNullValue());

        var client = meilisearchClient();
        var index = client.getIndex(indexUid);
        var searchResult = index.search("");
        assertThat(searchResult.getEstimatedTotalHits(), is(1));
    }

    @Test
    void shouldThrowWhenBothIdsAndFilterSpecified() {
        var runContext = runContextFactory.of();

        var task = DeleteDocuments.builder()
            .host(Property.ofValue(host))
            .apiKey(Property.ofValue(apiKey))
            .index(Property.ofValue(indexUid))
            .ids(Property.ofValue(List.of("1")))
            .filter(Property.ofValue("id = 1"))
            .build();

        assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
    }

    @Test
    void shouldThrowWhenNeitherIdsNorFilterSpecified() {
        var runContext = runContextFactory.of();

        var task = DeleteDocuments.builder()
            .host(Property.ofValue(host))
            .apiKey(Property.ofValue(apiKey))
            .index(Property.ofValue(indexUid))
            .build();

        assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
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
