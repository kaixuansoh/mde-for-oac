package my.edu.um.dissertation.observability.cli;

import my.edu.um.dissertation.observability.generator.Generator;
import my.edu.um.dissertation.observability.model.Diagnostic;
import my.edu.um.dissertation.observability.model.ObservabilityRuntime;
import my.edu.um.dissertation.observability.model.ValidationResult;
import my.edu.um.dissertation.observability.model.Validator;
import org.eclipse.emf.ecore.EObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "obs-generate",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Validate an .observability instance and generate "
                + "Java + YAML Observability-as-Code artifacts.")
public final class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to a .observability instance file")
    private Path instance;

    @Parameters(index = "1", description = "Output directory for generated artifacts")
    private Path outputDir;

    @Option(names = {"-m", "--metamodel"},
            description = "Path to observability.ecore "
                    + "(default: ../metamodel/model/observability.ecore)")
    private Path metamodel;

    @Option(names = "--validate-only",
            description = "Run validation but do not generate artifacts.")
    private boolean validateOnly;

    @Option(names = "--strict",
            description = "Treat warnings as errors and refuse to generate.")
    private boolean strict;

    @Override
    public Integer call() throws Exception {
        Path mm = metamodel != null
                ? metamodel
                : Path.of("..", "metamodel", "model", "observability.ecore");
        if (!Files.exists(mm)) {
            err("Metamodel not found: " + mm.toAbsolutePath());
            return 2;
        }
        if (!Files.exists(instance)) {
            err("Instance not found: " + instance.toAbsolutePath());
            return 2;
        }

        ObservabilityRuntime rt = ObservabilityRuntime.bootstrap(mm);
        EObject model = rt.loadInstance(instance);

        ValidationResult result = new Validator().validate(model);
        printDiagnostics(result);

        if (result.hasErrors() || (strict && result.warningCount() > 0)) {
            err("Refusing to generate: "
                    + result.errorCount() + " error(s), "
                    + result.warningCount() + " warning(s)");
            return 1;
        }
        if (validateOnly) {
            out("Validation passed (no generation requested).");
            return 0;
        }

        Files.createDirectories(outputDir);
        new Generator().generateAll(model, outputDir);
        long count = Files.list(outputDir).filter(Files::isRegularFile).count();
        out("Generated " + count + " artifact(s) in " + outputDir);
        return 0;
    }

    private void printDiagnostics(ValidationResult r) {
        for (Diagnostic d : r.errors())   err("  X " + d);
        for (Diagnostic d : r.warnings()) out("  ! " + d);
    }

    private void out(String s) { System.out.println(s); }
    private void err(String s) { System.err.println(s); }

    public static void main(String[] args) {
        int code = new CommandLine(new Main()).execute(args);
        System.exit(code);
    }
}
