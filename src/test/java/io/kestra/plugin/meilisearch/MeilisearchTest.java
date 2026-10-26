package io.kestra.plugin.meilisearch;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@KestraTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class MeilisearchTest {
    protected static final String MASTER_KEY = "masterKey123456789";

    @Container
    protected static GenericContainer<?> meilisearchContainer = new GenericContainer<>("getmeili/meilisearch:v1.11.0")
        .withExposedPorts(7700)
        .withEnv("MEILI_MASTER_KEY", MASTER_KEY)
        .withEnv("MEILI_NO_ANALYTICS", "true")
        .waitingFor(Wait.forHttp("/health").forStatusCode(200));

    protected static String host;
    protected static String apiKey = MASTER_KEY;

    @Inject
    protected RunContextFactory runContextFactory;

    protected final String indexUid = "kestra_" + getClass().getSimpleName().toLowerCase() + "_" + Long.toHexString(System.nanoTime());

    @BeforeAll
    public static void startMeilisearch() {
        if (meilisearchContainer != null && !meilisearchContainer.isRunning()) {
            try {
                meilisearchContainer.start();
            } catch (Throwable t) {
                // Docker unavailable
            }
        }
        if (meilisearchContainer != null && meilisearchContainer.isRunning()) {
            host = "http://" + meilisearchContainer.getHost() + ":" + meilisearchContainer.getMappedPort(7700);
        }
    }

    @AfterAll
    public static void stopMeilisearch() {
        if (meilisearchContainer != null && meilisearchContainer.isRunning()) {
            meilisearchContainer.stop();
        }
    }

    protected boolean isMeilisearchAvailable() {
        return meilisearchContainer != null && meilisearchContainer.isRunning();
    }

    protected Client meilisearchClient() {
        return new Client(new Config(host, apiKey));
    }
}
