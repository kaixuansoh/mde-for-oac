package my.edu.um.dissertation.observability.model;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.nio.file.Path;

/**
 * Bootstraps EMF for standalone (non-Eclipse) use, loads the
 * platform-independent observability metamodel from disk, and exposes
 * helpers for loading {@code .observability} instance files.
 *
 * <p>Standalone EMF needs three pieces of one-time setup:
 * <ul>
 *   <li>resource-factory registration so {@code .ecore} and {@code
 *       .observability} files can be parsed off the filesystem,</li>
 *   <li>loading the metamodel as a dynamic Ecore (instances become
 *       {@code DynamicEObject}s — no generated Java required),</li>
 *   <li>registering the metamodel's nsURI in the global EPackage
 *       registry so XMI references inside instance files resolve.</li>
 * </ul>
 */
public final class ObservabilityRuntime {

    private static final String OBSERVABILITY_FILE_EXT = "observability";
    private static final String ECORE_FILE_EXT = "ecore";

    private final ResourceSet resourceSet;
    private final EPackage observabilityPackage;

    private ObservabilityRuntime(ResourceSet resourceSet, EPackage observabilityPackage) {
        this.resourceSet = resourceSet;
        this.observabilityPackage = observabilityPackage;
    }

    /**
     * Initialises EMF and loads the metamodel from disk.
     *
     * @param metamodelPath filesystem path to {@code observability.ecore}
     */
    public static ObservabilityRuntime bootstrap(Path metamodelPath) {
        Resource.Factory.Registry factories = Resource.Factory.Registry.INSTANCE;
        factories.getExtensionToFactoryMap().put(ECORE_FILE_EXT,
                new EcoreResourceFactoryImpl());
        factories.getExtensionToFactoryMap().put(OBSERVABILITY_FILE_EXT,
                new XMIResourceFactoryImpl());

        ResourceSet resourceSet = new ResourceSetImpl();

        URI metamodelUri = URI.createFileURI(metamodelPath.toAbsolutePath().toString());
        Resource metamodelResource = resourceSet.getResource(metamodelUri, true);
        if (metamodelResource.getContents().isEmpty()
                || !(metamodelResource.getContents().get(0) instanceof EPackage pkg)) {
            throw new IllegalStateException("Metamodel root is not an EPackage: " + metamodelUri);
        }

        EPackage.Registry.INSTANCE.put(pkg.getNsURI(), pkg);
        return new ObservabilityRuntime(resourceSet, pkg);
    }

    /** Loads a {@code .observability} instance and returns its root {@code EObject}. */
    public EObject loadInstance(Path instancePath) {
        URI uri = URI.createFileURI(instancePath.toAbsolutePath().toString());
        Resource resource = resourceSet.getResource(uri, true);
        if (resource.getContents().isEmpty()) {
            throw new IllegalStateException("Instance has no contents: " + uri);
        }
        EObject root = resource.getContents().get(0);
        EcoreUtil.resolveAll(resource);
        return root;
    }

    public EPackage observabilityPackage() {
        return observabilityPackage;
    }

    public ResourceSet resourceSet() {
        return resourceSet;
    }
}
