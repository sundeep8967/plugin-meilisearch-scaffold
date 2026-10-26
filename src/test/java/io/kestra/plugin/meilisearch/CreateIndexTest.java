package io.kestra.plugin.meilisearch;

import io.kestra.core.models.property.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class CreateIndexTest extends MeilisearchTest {

    @Test
    void run() throws Exception {
        assumeTrue(isMeilisearchAvailable());
        var runContext = runContextFactory.of();

        var task = CreateIndex.builder()
            .host(Property.ofValue(host))
            .apiKey(Property.ofValue(apiKey))
            .index(Property.ofValue(indexUid))
            .primaryKey(Property.ofValue("id"))
            .waitForTask(Property.ofValue(true))
            .build();

        var output = task.run(runContext);

        assertThat(output, notNullValue());
        assertThat(output.getIndex(), is(indexUid));
        assertThat(output.getPrimaryKey(), is("id"));
        assertThat(output.getTaskUid(), notNullValue());

        var client = meilisearchClient();
        var index = client.getIndex(indexUid);
        assertThat(index, notNullValue());
        assertThat(index.getUid(), is(indexUid));
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
