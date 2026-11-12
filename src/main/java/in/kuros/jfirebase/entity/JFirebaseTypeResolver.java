package in.kuros.jfirebase.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a {@link DocumentTypeResolver} that picks the concrete subclass to instantiate when a
 * document of the annotated class is read. Takes precedence over Jackson's {@code @JsonTypeInfo}
 * and {@code @JsonSubTypes} on the same class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface JFirebaseTypeResolver {
    Class<? extends DocumentTypeResolver> value();
}
