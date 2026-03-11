package io.kestra.plugin.trello;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import io.kestra.core.junit.annotations.KestraTest;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;

@KestraTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractTrelloTest implements TestPropertyProvider {

    @Override
    public Map<String, String> getProperties() {
        return Map.of("mock.trello.enabled", "true");
    }

    @Inject
    protected ApplicationContext applicationContext;

    protected EmbeddedServer embeddedServer;

    @BeforeEach
    void setUp() {
        if (embeddedServer == null || !embeddedServer.isRunning()) {
            embeddedServer = applicationContext.getBean(EmbeddedServer.class);
            embeddedServer.start();
        }
    }

    @AfterAll
    void tearDown() {
        if (embeddedServer != null && embeddedServer.isRunning()) {
            embeddedServer.stop();
        }
    }

    protected String getApiBaseUrl() {
        return embeddedServer.getURI().toString();
    }
}
