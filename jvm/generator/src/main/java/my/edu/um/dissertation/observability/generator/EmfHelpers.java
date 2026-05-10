package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.util.List;

/** Tiny shared helpers for walking dynamic EMF instances. */
final class EmfHelpers {
    private EmfHelpers() {}

    @SuppressWarnings("unchecked")
    static List<EObject> children(EObject parent, String featureName) {
        EStructuralFeature f = parent.eClass().getEStructuralFeature(featureName);
        if (!(f instanceof EReference) || !f.isMany()) {
            EObject c = single(parent, featureName);
            return c == null ? List.of() : List.of(c);
        }
        Object value = parent.eGet(f);
        return value instanceof EList<?> list ? (List<EObject>) list : List.of();
    }

    static EObject single(EObject parent, String featureName) {
        EStructuralFeature f = parent.eClass().getEStructuralFeature(featureName);
        if (f == null) return null;
        Object value = parent.eGet(f);
        return value instanceof EObject eo ? eo : null;
    }

    static String str(EObject obj, String featureName) {
        EStructuralFeature f = obj.eClass().getEStructuralFeature(featureName);
        if (f == null) return null;
        Object v = obj.eGet(f);
        return v == null ? null : v.toString();
    }

    static Integer intValue(EObject obj, String featureName) {
        EStructuralFeature f = obj.eClass().getEStructuralFeature(featureName);
        if (f == null) return null;
        Object v = obj.eGet(f);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }

    static String typeName(EObject obj) {
        return obj.eClass().getName();
    }

    static String packageName(EObject service) {
        String ns = str(service, "namespace");
        return ns == null || ns.isBlank() ? "com.acme.observability" : ns;
    }

    static String identifier(String s) {
        return s == null ? "" : s.replaceAll("[^A-Za-z0-9]", "_");
    }
}
