# JFirebase

[![](https://jitpack.io/v/bgizdov/jFirebase.svg)](https://jitpack.io/#bgizdov/jFirebase)

I decided to tryout firebase for my personal projects & learning, having come from traditional SQL background, I am well versed with JPA/Hibernate when dealing with databases. But when I started firebase, I found it completely different but also having some similarities. After spending some time understanding its feature I started looking a framework similar to JPA for firebase. I found none, so I decided to write one for myself.

# For detailed usage, please visit [my blog link - jFirebase](https://kuros.in/jfirebase/jFirebase-a-firebase-wrapper-for-java-developers/)

# Maven package

https://jitpack.io/#bgizdov/jFirebase

To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

```
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
  ```
  Step 2. Add the dependency
  
  ```
  <dependency>
    <groupId>com.github.bgizdov</groupId>
    <artifactId>jFirebase</artifactId>
    <version>1.6.0</version>
</dependency>
  ```

# Choosing a subclass from the whole document

`@JsonTypeInfo`/`@JsonSubTypes` can only look at one property. To decide the concrete class from several fields, annotate the declared class with `@JFirebaseTypeResolver` and implement `DocumentTypeResolver` (public no-argument constructor, one cached instance per annotated class):

```java
@JFirebaseTypeResolver(ShapeResolver.class)
public class Shape { ... }

public class ShapeResolver implements DocumentTypeResolver {
    public Class<?> resolve(Map<String, Object> values) {
        return "round".equals(values.get("kind")) ? Circle.class : null; // null = Shape itself
    }
}
```

The returned class must be `Shape` or a subclass. The annotation takes precedence over `@JsonSubTypes` on the same class.

The map keys are the stored Firestore field names (before naming-strategy translation). A resolver for a concrete base class should return a concrete fallback class rather than `null` if base instances must be writable. Reading a subclass directly keeps that subclass, even though the annotation is inherited.

`BigDecimal` properties are written as Firestore doubles when the value is exactly representable as a double; otherwise saving throws an exception naming the value.
