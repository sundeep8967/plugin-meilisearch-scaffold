package io.kestra.plugin.meilisearch;

import io.kestra.core.models.property.Property;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class DropIndexTest extends MeilisearchTest {

    @Test
    void run() throws Exception {
        assumeTrue(isMeilisearchAvailable());
        var runContext = runContextFactory.of();

        var client = meilisearchClient();
        var taskInfo = client.createIndex(indexUid);
        client.waitForTask(taskInfo.getTaskUid());

        var task = DropIndex.builder()
            .host(Property.ofValue(host))
            .apiKey(Property.ofValue(apiKey))
            .index(Property.ofValue(indexUid))
            .waitForTask(Property.ofValue(true))
            .build();

        var output = task.run(runContext);

        assertThat(output, is((Object) null));

        assertThrows(Exception.class, () -> client.getIndex(indexUid));
    }
}
