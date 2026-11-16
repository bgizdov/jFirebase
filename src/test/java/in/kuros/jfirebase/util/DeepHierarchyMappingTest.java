package in.kuros.jfirebase.util;

import in.kuros.jfirebase.entity.DocumentTypeResolver;
import in.kuros.jfirebase.entity.Id;
import in.kuros.jfirebase.entity.JFirebaseTypeResolver;
import in.kuros.jfirebase.entity.Transient;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepHierarchyMappingTest {

    public static class LeafResolver implements DocumentTypeResolver {
        @Override
        public Class<?> resolve(Map<String, Object> values) {
            return Leaf.class;
        }
    }

    @JFirebaseTypeResolver(LeafResolver.class)
    public static class Root {
        private String result;
        private Long matchId;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public Long getMatchId() {
            return matchId;
        }

        public void setMatchId(Long matchId) {
            this.matchId = matchId;
        }
    }

    public static class Mid extends Root {
        private Long line;

        public Long getLine() {
            return line;
        }

        public void setLine(Long line) {
            this.line = line;
        }
    }

    public static class Leaf extends Mid {
        private String prediction;

        public String getPrediction() {
            return prediction;
        }

        public void setPrediction(String prediction) {
            this.prediction = prediction;
        }

        // getter without any backing field
        public String getMarket() {
            return "WINNER";
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Leaf)) {
                return false;
            }
            Leaf l = (Leaf) o;
            return Objects.equals(getResult(), l.getResult()) && Objects.equals(getMatchId(), l.getMatchId())
                    && Objects.equals(getLine(), l.getLine()) && Objects.equals(prediction, l.prediction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getResult(), getMatchId(), getLine(), prediction);
        }
    }

    // one-level class with a private final field in the subclass
    public static class Parent1 {
        private String a;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }
    }

    public static class Child1 extends Parent1 {
        private final String market = "fixed";

        public String getMarket() {
            return market;
        }

        public void setMarket(String m) {
            throw new IllegalStateException("setter must be skipped");
        }
    }

    // annotated fields two levels up
    public static class Top {
        @Id
        private String key;
        @Transient
        private String secret;
        private String visible;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getVisible() {
            return visible;
        }

        public void setVisible(String visible) {
            this.visible = visible;
        }
    }

    public static class Middle extends Top {
    }

    public static class Bottom extends Middle {
        private String own;

        public String getOwn() {
            return own;
        }

        public void setOwn(String own) {
            this.own = own;
        }
    }

    private static Leaf leaf() {
        Leaf l = new Leaf();
        l.setResult("WIN");
        l.setMatchId(7L);
        l.setLine(2L);
        l.setPrediction("home");
        return l;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> ser(Object o) {
        return (Map<String, Object>) ClassMapper.serialize(o);
    }

    @Test
    void aThreeLevelClassIsSerializedWithFieldsOfEveryLevel() {
        Map<String, Object> m = ser(leaf());
        assertEquals("WIN", m.get("result"));
        assertEquals(7L, m.get("matchId"));
        assertEquals(2L, m.get("line"));
        assertEquals("home", m.get("prediction"));
    }

    @Test
    void aGetterWithoutABackingFieldIsSerialized() {
        assertEquals("WINNER", ser(leaf()).get("market"));
    }

    @Test
    void aThreeLevelClassRoundTripsThroughTheResolver() {
        Map<String, Object> m = ser(leaf());
        Root back = NamingStrategyClassMapper.convertToCustomClass(m, Root.class, null);
        assertSame(Leaf.class, back.getClass());
        assertEquals(leaf(), back);
    }

    @Test
    void existingOneLevelClassesBehaveAsBefore() {
        Child1 c = new Child1();
        c.setA("x");
        Map<String, Object> expected = new HashMap<>();
        expected.put("a", "x");
        expected.put("market", "fixed");
        assertEquals(expected, ser(c));

        Map<String, Object> in = new HashMap<>();
        in.put("a", "y");
        in.put("market", "ignored");
        Child1 back = NamingStrategyClassMapper.convertToCustomClass(in, Child1.class, null);
        assertEquals("y", back.getA());
        assertEquals("fixed", back.getMarket());
    }

    @Test
    void transientAndIdAnnotationsOnInheritedFieldsStillApply() {
        Bottom b = new Bottom();
        b.setKey("k1");
        b.setSecret("s");
        b.setVisible("v");
        b.setOwn("o");
        Map<String, Object> m = ser(b);
        assertFalse(m.containsKey("secret"));
        assertEquals("v", m.get("visible"));
        assertEquals("o", m.get("own"));
        assertTrue(ClassMapper.getBeanMapper(Bottom.class).getId().isPresent());
        assertEquals("key", ClassMapper.getBeanMapper(Bottom.class).getId().get());
    }
}
