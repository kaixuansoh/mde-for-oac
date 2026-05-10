package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;

import java.io.IOException;
import java.nio.file.Path;

/** A model-to-text transformation: takes a model root and writes file(s) to {@code outDir}. */
public interface Emitter {
    void emit(EObject model, Path outDir) throws IOException;
}
