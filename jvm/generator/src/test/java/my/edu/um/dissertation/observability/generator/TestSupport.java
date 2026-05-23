package my.edu.um.dissertation.observability.generator;

import my.edu.um.dissertation.observability.model.ObservabilityRuntime;
import org.eclipse.emf.ecore.EObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generator-side mirror of the model module's TestSupport. Bootstraps the
 * EMF runtime once and turns inline XMI bodies into loaded {@code EObject}
 * roots that can be fed straight into an {@link Emitter}.
 */
final class TestSupport {

    private static final String NS_PROLOGUE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <obs:ObservabilityModel xmi:version="2.0"
                xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:obs="http://dissertation.um.edu.my/observability/1.0"
                name="%s" version="1.0.0">
            %s
            </obs:ObservabilityModel>
            """;

    private static final AtomicLong COUNTER = new AtomicLong();
    private static volatile ObservabilityRuntime RUNTIME;

    private TestSupport() {}

    static synchronized ObservabilityRuntime runtime() {
        if (RUNTIME == null) RUNTIME = ObservabilityRuntime.bootstrap(locateMetamodel());
        return RUNTIME;
    }

    static EObject load(Path tempDir, String body) throws IOException {
        return load(tempDir, body, "test-model");
    }

    static EObject load(Path tempDir, String body, String modelName) throws IOException {
        Path file = tempDir.resolve("fixture-" + COUNTER.incrementAndGet() + ".observability");
        Files.writeString(file, NS_PROLOGUE.formatted(modelName, body));
        return runtime().loadInstance(file);
    }

    static Path locateMetamodel() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("metamodel/model/observability.ecore");
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException("Could not locate observability.ecore from "
                + System.getProperty("user.dir"));
    }
}
