package my.edu.um.dissertation.observability.model;

/**
 * One outcome from a validation pass. Severity is either {@code ERROR}
 * (blocks generation) or {@code WARNING} (informational).
 */
public record Diagnostic(Severity severity, String code, String message, String path) {

    public enum Severity { ERROR, WARNING }

    public static Diagnostic error(String code, String message, String path) {
        return new Diagnostic(Severity.ERROR, code, message, path);
    }

    public static Diagnostic warning(String code, String message, String path) {
        return new Diagnostic(Severity.WARNING, code, message, path);
    }

    @Override
    public String toString() {
        return "%s %s @ %s — %s".formatted(severity, code, path, message);
    }
}
