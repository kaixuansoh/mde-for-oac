package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Runs all five model-to-text emitters in sequence. Wipes the output
 * directory before regenerating so stale artifacts from a previous run
 * never linger.
 */
public final class Generator {

    private final List<Emitter> emitters = List.of(
            new SpanInstrumentationEmitter(),
            new MetricRegistrationEmitter(),
            new LogInstrumentationEmitter(),
            new CollectorYamlEmitter(),
            new PrometheusAlertsEmitter());

    public void generateAll(EObject model, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        try (Stream<Path> existing = Files.list(outDir)) {
            for (Path p : existing.toList()) {
                if (Files.isRegularFile(p)) Files.delete(p);
            }
        }
        for (Emitter e : emitters) e.emit(model, outDir);
    }
}
