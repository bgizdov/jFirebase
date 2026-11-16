package in.kuros.jfirebase.entity;

import java.util.Map;

/**
 * Chooses the concrete class to instantiate for a document, based on the whole Firestore document map.
 * <p>
 * Implementations must have a public no-argument constructor. One instance is created per annotated
 * class and reused, so implementations must be thread safe.
 * <p>
 * The map keys are the stored Firestore field names (before naming-strategy translation). A resolver
 * for a concrete base class should return a concrete fallback class rather than {@code null} if base
 * instances must be writable.
 */
public interface DocumentTypeResolver {

    /**
     * @param values the raw document (or nested object) map, exactly as read from Firestore
     * @return the class to instantiate, which must be the declared class or one of its subclasses,
     * or {@code null} to use the declared class
     */
    Class<?> resolve(Map<String, Object> values);
}
