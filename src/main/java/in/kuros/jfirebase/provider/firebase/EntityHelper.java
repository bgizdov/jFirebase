package in.kuros.jfirebase.provider.firebase;

import in.kuros.jfirebase.entity.Entity;
import in.kuros.jfirebase.entity.EntityDeclarationException;
import in.kuros.jfirebase.metadata.AttributeValue;

import in.kuros.jfirebase.util.PropertyNamingStrategy;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EntityHelper {

    <T> String getDocumentPath(T entity);

    <T> String getDocumentPath(List<AttributeValue<T, ?>> attributeValues);

    <T> String getCollectionPath(T entity);

    <T> String getCollectionPath(List<AttributeValue<T, ?>> attributeValues);

    <T> void setId(T entity, String id);

    <T> String getId(T entity);

    <T> void setCreateTime(T entity);

    <T> boolean setUpdateTime(T entity);

    Optional<String> getUpdateTimeFieldName(Class<?> type);

    <T> Set<String> getAllRequiredIdFields(Class<T> type);

    <T> void validateIdsNotNull(T object);

    static String getMappedCollection(final Class<?> aClass) {
        return getEntity(aClass).value();
    }

    static Entity getEntity(final Class<?> aClass) {
        final Entity annotation = aClass.getAnnotation(Entity.class);
        if (annotation == null) {
            throw new EntityDeclarationException("No annotation found for Entity: " + aClass);
        }
        return annotation;
    }

    public PropertyNamingStrategy getPropertyNamingStrategy();

    public void setPropertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy);

    EntityHelper INSTANCE = new EntityHelperImpl();
}
