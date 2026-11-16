package in.kuros.jfirebase.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BigDecimalMappingTest {

    public static class Money {
        private BigDecimal amount;
        private Integer i;
        private Long l;
        private Double d;
        private Float f;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Integer getI() {
            return i;
        }

        public void setI(Integer i) {
            this.i = i;
        }

        public Long getL() {
            return l;
        }

        public void setL(Long l) {
            this.l = l;
        }

        public Double getD() {
            return d;
        }

        public void setD(Double d) {
            this.d = d;
        }

        public Float getF() {
            return f;
        }

        public void setF(Float f) {
            this.f = f;
        }
    }

    private static Money money(String amount) {
        Money m = new Money();
        m.setAmount(amount == null ? null : new BigDecimal(amount));
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> ser(Object o) {
        return (Map<String, Object>) ClassMapper.serialize(o);
    }

    @Test
    void aBigDecimalIsWrittenAsADouble() {
        assertEquals(210.5d, ser(money("210.5")).get("amount"));
        assertEquals(40.0d, ser(money("40")).get("amount"));
        assertEquals(0.5d, ser(money("0.5")).get("amount"));
        assertEquals(-3.25d, ser(money("-3.25")).get("amount"));
    }

    @Test
    void aBigDecimalRoundTrips() {
        for (String v : new String[]{"210.5", "23.5", "40", "210.50", "-3.25"}) {
            Money back = NamingStrategyClassMapper.convertToCustomClass(ser(money(v)), Money.class, null);
            assertEquals(0, new BigDecimal(v).compareTo(back.getAmount()), v + " -> " + back.getAmount());
        }
    }

    @Test
    void aBigDecimalThatIsNotAnExactDoubleIsRejected() {
        String v = "0.1234567890123456789012345";
        RuntimeException e = assertThrows(RuntimeException.class, () -> ser(money(v)));
        assertTrue(e.getMessage().contains(v), e.getMessage());
        assertTrue(e.getMessage().contains("double"), e.getMessage());
    }

    @Test
    void aNullBigDecimalIsWrittenAsNull() {
        Map<String, Object> m = ser(money(null));
        assertTrue(m.containsKey("amount"));
        assertNull(m.get("amount"));
    }

    @Test
    void otherNumberTypesAreUnchanged() {
        Money m = new Money();
        m.setI(5);
        m.setL(6L);
        m.setD(7.5d);
        m.setF(8.5f);
        Map<String, Object> out = ser(m);
        assertEquals(Integer.valueOf(5), out.get("i"));
        assertEquals(Long.valueOf(6), out.get("l"));
        assertEquals(Double.valueOf(7.5), out.get("d"));
        assertEquals(Float.valueOf(8.5f), out.get("f"));
    }
}
