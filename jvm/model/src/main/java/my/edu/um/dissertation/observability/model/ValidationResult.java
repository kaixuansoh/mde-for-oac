package my.edu.um.dissertation.observability.model;

import java.util.List;

/** Aggregated outcome of a {@link Validator} pass. */
public record ValidationResult(List<Diagnostic> errors, List<Diagnostic> warnings) {

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public int errorCount()   { return errors.size(); }
    public int warningCount() { return warnings.size(); }
}
