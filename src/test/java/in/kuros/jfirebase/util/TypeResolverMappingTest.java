package in.kuros.jfirebase.util;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import in.kuros.jfirebase.entity.DocumentTypeResolver;
import in.kuros.jfirebase.entity.JFirebaseTypeResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeResolverMappingTest {

    // ---- model ----
    public static class ShapeResolver implements DocumentTypeResolver {
        static final AtomicInteger CREATED = new AtomicInteger();
        static volatile Map<String, Object> lastSeen;

        public ShapeResolver() {
            CREATED.incrementAndGet();
        }

        @Override
        public Class<?> resolve(Map<String, Object> values) {
            lastSeen = values;
            Object kind = values.get("kind");
            Object sides = values.get("sides");
            if ("round".equals(kind) && sides instanceof Number && ((Number) sides).intValue() == 0) {
                return Circle.class;
            }
            if ("flat".equals(kind)) {
                return Polygon.class;
            }
            return null;
        }
    }

    @JFirebaseTypeResolver(ShapeResolver.class)
    public static class Shape {
        public String kind;
        public Long sides;
    }

    public static class Circle extends Shape {
    }

    public static class Polygon extends Shape {
    }

    public static class Holder {
        public List<Shape> shapes;
        public Shape main;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({@JsonSubTypes.Type(value = LegacyA.class, name = "a"),
            @JsonSubTypes.Type(value = LegacyB.class, name = "b")})
    public static class Legacy {
        public String type;
    }

    public static class LegacyA extends Legacy {
    }

    public static class LegacyB extends Legacy {
    }

    public static class Plain {
        public String name;
    }

    public static class ThrowingResolver implements DocumentTypeResolver {
        @Override
        public Class<?> resolve(Map<String, Object> values) {
            throw new IllegalStateException("boom");
        }
    }

    @JFirebaseTypeResolver(ThrowingResolver.class)
    public static class Broken {
        public String name;
    }

    public static class UnrelatedResolver implements DocumentTypeResolver {
        @Override
        public Class<?> resolve(Map<String, Object> values) {
            return Plain.class;
        }
    }

    @JFirebaseTypeResolver(UnrelatedResolver.class)
    public static class Wrong {
        public String name;
    }

    public static class OtherResolver implements DocumentTypeResolver {
        @Override
        public Class<?> resolve(Map<String, Object> values) {
            return "x".equals(values.get("flag")) ? Special.class : null;
        }
    }

    @JFirebaseTypeResolver(OtherResolver.class)
    public static class Item {
        public String flag;
    }

    public static class Special extends Item {
    }

    public static class Basket {
        public Item item;
        public Shape shape;
    }

    // ---- helpers ----
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static <T> T read(Map<String, Object> m, Class<T> c) {
        return NamingStrategyClassMapper.convertToCustomClass(m, c, null);
    }

    // ---- tests ----
    @Test
    void resolverPicksClassFromTwoFields() {
        Shape a = read(map("kind", "round", "sides", 0L), Shape.class);
        Shape b = read(map("kind", "flat", "sides", 4L), Shape.class);
        assertSame(Circle.class, a.getClass());
        assertSame(Polygon.class, b.getClass());
        assertEquals(Long.valueOf(4), b.sides);
        assertEquals("flat", b.kind);
    }

    @Test
    void nullFromResolverGivesDeclaredClass() {
        Shape s = read(map("kind", "unknown"), Shape.class);
        assertSame(Shape.class, s.getClass());
    }

    @Test
    void nestedObjectAndListElementsAreResolved() {
        List<Object> list = new ArrayList<>(Arrays.asList(
                map("kind", "flat", "sides", 3L), map("kind", "round", "sides", 0L)));
        Holder h = read(map("main", map("kind", "round", "sides", 0L), "shapes", list), Holder.class);
        assertSame(Circle.class, h.main.getClass());
        assertEquals(2, h.shapes.size());
        assertSame(Polygon.class, h.shapes.get(0).getClass());
        assertSame(Circle.class, h.shapes.get(1).getClass());
    }

    @Test
    void nestedClassesWithDifferentResolversUseTheirOwn() {
        Basket b = read(map("item", map("flag", "x"), "shape", map("kind", "flat")), Basket.class);
        assertSame(Special.class, b.item.getClass());
        assertSame(Polygon.class, b.shape.getClass());
    }

    @Test
    void classWithoutAnnotationUsesJsonSubTypes() {
        assertSame(LegacyA.class, read(map("type", "a"), Legacy.class).getClass());
        assertSame(LegacyB.class, read(map("type", "b"), Legacy.class).getClass());
        assertSame(Legacy.class, read(map("type", "zzz"), Legacy.class).getClass());
    }

    @Test
    void classWithoutAnyTypeAnnotationsIsUnchanged() {
        Plain p = read(map("name", "n"), Plain.class);
        assertSame(Plain.class, p.getClass());
        assertEquals("n", p.name);
    }

    @Test
    void jsonTypeInfoWithoutSubTypesDoesNotThrowNpe() {
        assertSame(NoSubTypes.class, read(map("type", "a"), NoSubTypes.class).getClass());
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public static class NoSubTypes {
        public String type;
    }

    @Test
    void resolverExceptionNamesTheClass() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> read(map("name", "n"), Broken.class));
        assertTrue(e.getMessage().contains(Broken.class.getName()), e.getMessage());
        assertNotNull(e.getCause());
    }

    @Test
    void resolverReturningUnrelatedClassIsRejected() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> read(map("name", "n"), Wrong.class));
        assertTrue(e.getMessage().contains(Wrong.class.getName()), e.getMessage());
        assertTrue(e.getMessage().contains(Plain.class.getName()), e.getMessage());
    }

    @Test
    void missingOrNonStringDiscriminatorsReachResolverAsIs() {
        Map<String, Object> m = map("kind", null, "tag", 42, "other", true);
        Shape s = read(m, Shape.class);
        assertSame(Shape.class, s.getClass());
        assertEquals(m, ShapeResolver.lastSeen);
        assertEquals(42, ShapeResolver.lastSeen.get("tag"));
        Shape empty = read(map(), Shape.class);
        assertSame(Shape.class, empty.getClass());
    }

    @Test
    void resolverIsCreatedOncePerClass() {
        read(map("kind", "round", "sides", 0L), Shape.class);
        read(map("kind", "flat"), Shape.class);
        read(map("kind", "x"), Shape.class);
        assertEquals(1, ShapeResolver.CREATED.get());
    }
}
