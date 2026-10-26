package io.kestra.plugin.meilisearch;

import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class SearchTest extends MeilisearchTest {

    @BeforeEach
    void setUp() throws Exception {
        if (!isMeilisearchAvailable()) return;
        var client = meilisearchClient();
        client.createIndex(indexUid, "id");
        var index = client.index(indexUid);

        List<Map<String, Object>> documents = List.of(
            Map.of("id", "1", "title", "The Matrix", "genre", "Sci-Fi"),
            Map.of("id", "2", "title", "The Matrix Reloaded", "genre", "Sci-Fi"),
            Map.of("id", "3", "title", "Gladiator", "genre", "Action")
        );
        var taskInfo = index.addDocuments(AbstractMeilisearch.MAPPER.writeValueAsString(documents));
        client.waitForTask(taskInfo.getTaskUid());
    }

    @Test
    void runFetch() throws Exception {
        assumeTrue(isMeilisearchAvailable());
        var runContext = runContextFactory.of();

        var task = Search.builder()
            .host(Property.ofValue(host))
            .apiKey(Property.ofValue(apiKey))
            .index(Property.ofValue(indexUid))
            .q(Property.ofValue("Matrix"))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getSize(), is(2L));
        assertThat(output.getRows(), hasSize(2));
    }

    @Test
    void runFetchOne() throws Exception {
        assumeTrue(isMeilisearchAvailable());
        var runContext = runContextFactory.of();

        var task = Search.builder()
            .host(Property.ofValue(host))
            .apiKey(Property.ofValue(apiKey))
            .index(Property.ofValue(indexUid))
            .q(Property.ofValue("Gladiator"))
            .fetchType(Property.ofValue(FetchType.FETCH_ONE))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getSize(), is(1L));
        assertThat(output.getRow(), notNullValue());
        assertThat(output.getRow().get("title"), is("Gladiator"));
    }

    @Test
    void runStore() throws Exception {
        assumeTrue(isMeilisearchAvailable());
        var runContext = runContextFactory.of();

        var task = Search.builder()
            .host(Property.ofValue(host))
            .apiKey(Property.ofValue(apiKey))
            .index(Property.ofValue(indexUid))
            .q(Property.ofValue("Matrix"))
            .fetchType(Property.ofValue(FetchType.STORE))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getSize(), is(2L));
        assertThat(output.getUri(), notNullValue());
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
