package my.edu.um.dissertation.observability.model;

import org.eclipse.emf.ecore.EObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tiny helper shared by all JVM tests. Locates {@code observability.ecore}
 * relative to the running module (walking up the filesystem so tests work
 * whether Maven invokes us per-module or via the parent reactor), boots the
 * EMF runtime once per JVM, and exposes a {@code load(body)} convenience
 * that writes an inline XMI body to a unique temp file and loads it.
 *
 * <p>Mirrors the {@code _helpers.py} module used by the Python tests so
 * fixtures look the same across both runtimes.
 */
public final class TestSupport {

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

    public static synchronized ObservabilityRuntime runtime() {
        if (RUNTIME == null) {
            RUNTIME = ObservabilityRuntime.bootstrap(locateMetamodel());
        }
        return RUNTIME;
    }

    /** Writes the body into a unique file under {@code tempDir} and loads it. */
    public static EObject load(Path tempDir, String body) throws IOException {
        return load(tempDir, body, "test-model");
    }

    public static EObject load(Path tempDir, String body, String modelName) throws IOException {
        Path file = tempDir.resolve("fixture-" + COUNTER.incrementAndGet() + ".observability");
        Files.writeString(file, NS_PROLOGUE.formatted(modelName, body));
        return runtime().loadInstance(file);
    }

    /** Walks up from {@code user.dir} until it finds {@code metamodel/model/observability.ecore}. */
    public static Path locateMetamodel() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("metamodel/model/observability.ecore");
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "Could not locate metamodel/model/observability.ecore from "
                        + System.getProperty("user.dir"));
    }

    public static Path repoRoot() {
        Path mm = locateMetamodel();
        return mm.getParent().getParent().getParent();
    }
}
