package cz.cas.lib.core.util;

import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.HelperObj.A;
import static cz.cas.lib.core.util.HelperObj.B;
import static cz.cas.lib.core.util.Utils.*;
import static helper.ThrowableAssertion.assertThrown;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class UtilsTest {

    @Test
    public void mapTest() {
        List<A> as = asList(new A("a", 1), new A("b", 2), new A("c", 3));
        List<B> bs = asList(new B("a", 1, 0.1), new B("b", 2, 0.1), new B("c", 3, 0.1));
        Function<A, B> mapper = a -> new B(a.getA(), a.getB(), 0.1);

        List<A> from = as;
        List<B> to = map(from, mapper);
        assertThat(to, hasSize(3));
        assertThat(to, contains(bs.toArray()));

        from = Collections.emptyList();
        to = map(from, mapper);
        assertThat(to, is(empty()));
    }

    @Test
    public void asMapTest() {
        A a1 = new A("a", 1);
        A a2 = new A("b", 2);
        B b1 = new B("a", 1, 0.1);
        B b2 = new B("b", 2, 0.1);

        Map<A, B> abMap = asMap(a1, b1);
        assertThat(abMap.size(), is(1));
        assertThat(abMap.get(a1), is(b1));

        Map<A, B> abMap2 = asMap(a1, b1, a2, b2);
        assertThat(abMap2.size(), is(2));
        assertThat(abMap2.get(a1), is(b1));
        assertThat(abMap2.get(a2), is(b2));
    }

    @Test
    public void asListTest() {
        List<A> as = asList(new A("a", 1), new A("b", 2), new A("c", 3));
        assertThat(as, hasSize(3));
        assertThat(as, contains(new A("a", 1), new A("b", 2), new A("c", 3)));

        as = asList(as);
        assertThat(as, hasSize(3));
        assertThat(as, contains(new A("a", 1), new A("b", 2), new A("c", 3)));
    }

    @Test
    public void asSetTest() {
        Set<A> as = asSet(new A("a", 1), new A("b", 2), new A("c", 3));
        assertThat(as, hasSize(3));
        assertThat(as, containsInAnyOrder(new A("a", 1), new A("b", 2), new A("c", 3)));

        as = asSet(as);
        assertThat(as, hasSize(3));
        assertThat(as, containsInAnyOrder(new A("a", 1), new A("b", 2), new A("c", 3)));
    }

    @Test
    public void asObjectArrayTest() {
        Object[] objects = asObjectArray(new A("a", 1), new A("b", 2), new A("c", 3));
        assertThat(objects.length, is(3));
        assertThat(objects[0], is(new A("a", 1)));
        assertThat(objects[1], is(new A("b", 2)));
        assertThat(objects[2], is(new A("c", 3)));
    }

    @Test
    public void notNullTest() throws Exception {
        assertThrown(() -> notNull(null, IllegalArgumentException::new));
        notNull(new A(), IllegalArgumentException::new);

        assertThrown(() -> notNullEx(null, Exception::new));
        notNullEx(new A(), Exception::new);
    }

    @Test
    public void isNullTest() {
        assertThrown(() -> isNull(new A(), IllegalArgumentException::new));
        isNull(null, IllegalArgumentException::new);
    }

    @Test
    public void plusTest() {
        Instant a = LocalDate.ofYearDay(2016, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant days = plus(a, ChronoUnit.DAYS, 3);
        Instant years = plus(a, ChronoUnit.YEARS, -2);

        LocalDate daysDate = LocalDateTime.ofInstant(days, ZoneId.systemDefault()).toLocalDate();
        LocalDate yearsDate = LocalDateTime.ofInstant(years, ZoneId.systemDefault()).toLocalDate();
        assertThat(daysDate.getDayOfYear(), is(4));
        assertThat(yearsDate.getYear(), is(2014));
    }

    @Test
    public void eqTest() {
        A a1 = new A("a", 1);
        A a2 = new A("a", 1);
        A a3 = new A("b", 2);

        eq(a1, a2, IllegalArgumentException::new);
        assertThrown(() -> eq(a2, a3, IllegalArgumentException::new));
        assertThrown(() -> eq(a1, null, IllegalArgumentException::new));
        assertThrown(() -> eq(null, a2, IllegalArgumentException::new));
        eq(null, null, IllegalArgumentException::new);
    }

    @Test
    public void neTest() {
        A a1 = new A("a", 1);
        A a2 = new A("a", 1);
        A a3 = new A("b", 2);

        ne(a1, a3, IllegalArgumentException::new);
        ne(null, a3, IllegalArgumentException::new);
        ne(a1, null, IllegalArgumentException::new);

        assertThrown(() -> ne(a1, a2, IllegalArgumentException::new));
    }

    @Test
    public void ninTest() {
        A a = new A("a", 1);
        A b = new A("b", 1);

        Set<A> set = asSet(a);

        nin(b, set, IllegalArgumentException::new);
        nin(null, set, IllegalArgumentException::new);
        assertThrown(() -> nin(a, set, IllegalArgumentException::new));
    }

    @Test
    public void inTest() {
        assertThrown(() -> in(0, 1, 2, IllegalArgumentException::new));
        in(1, 1, 2, IllegalArgumentException::new);
        in(2, 1, 2, IllegalArgumentException::new);
        assertThrown(() -> in(3, 1, 2, IllegalArgumentException::new));
    }

    @Test
    public void gteTest() {
        assertThrown(() -> gte(0, 1, IllegalArgumentException::new));
        gte(1, 1, IllegalArgumentException::new);
        gte(2, 1, IllegalArgumentException::new);
    }

    @Test
    public void gtTest() {
        assertThrown(() -> gt(new BigDecimal(0), new BigDecimal(1), IllegalArgumentException::new));
        assertThrown(() -> gt(new BigDecimal(1), new BigDecimal(1), IllegalArgumentException::new));
        gt(new BigDecimal(2), new BigDecimal(1), IllegalArgumentException::new);
    }

    @Test
    public void toDoubleTest() {
        assertThat(toDouble(BigDecimal.TEN), is(10.0));
        assertThat(toDouble(null), is(nullValue()));
    }

    @Test
    public void ifPresentTest() {
        A a = new A("a", 0);
        ifPresent(a, (val) -> a.setB(1));
        assertThat(a.getB(), is(1));

        ifPresent(null, (val) -> a.setB(2));
        assertThat(a.getB(), is(1));
    }

    @Test
    public void checkedTest() {
        checked(() -> {
        });
        assertThrown(() ->
                checked(() -> {
                    throw new Exception();
                })
        ).isInstanceOf(GeneralException.class);

        checked(() -> {
        }, IllegalArgumentException::new);
        assertThrown(() ->
                checked(() -> {
                    throw new Exception();
                }, IllegalArgumentException::new)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void toDateTest() {
        Instant a = LocalDateTime.of(2016, 1, 1, 12, 30, 15).atZone(ZoneId.systemDefault()).toInstant();
        Date date = toDate(a);
        assertThat(date.toInstant(), is(a));

        LocalDateTime b = LocalDateTime.ofInstant(a, ZoneId.systemDefault());
        date = toDate(b);
        assertThat(date.toInstant(), is(a));

        Instant a2 = LocalDateTime.of(2016, 1, 1, 12, 30, 15).atZone(ZoneId.systemDefault()).toInstant();
        Date date2 = toDate(a2);
        assertThat(date2.toInstant(), is(a2));

        LocalDate b2 = a2.atZone(ZoneId.systemDefault()).toLocalDate();
        date2 = toDate(b2);
        assertThat(date2.toInstant().atZone(ZoneId.systemDefault()),
                is(b2.atStartOfDay().atZone(ZoneId.systemDefault())));
    }

    @Test
    public void joinTest() {
        String s1 = "a";
        String s2 = "b";
        String s3 = "c";

        List<String> data = asList(s1, s2, s3);
        String collect = data.stream()
                .collect(Collectors.joining(", "));

        String join1 = join(data);
        assertThat(collect, is(join1));

        assertThat(join(null), is(""));

        Function<String, String> toStringFunction = (String s) -> s.toString();
        String join2 = join(data, toStringFunction);

        assertThat(collect, is(join2));

        assertThat(join(null, toStringFunction), is(""));
    }

    @Test
    public void coalesceTest() {
        A nullName = new A();
        A a = new A("a", 1);
        A b = new A("b", 1);

        assertThat("a", is(coalesce(nullName::getA, a::getA, b::getA)));

        A c = new A("c", 1) {
            @Override
            public String getA() {
                fail("Test fail.");
                return "angry";
            }
        };

        assertThat("a", is(coalesce(a::getA, c::getA)));

        assertThat("a", is(coalesce(() -> new A("a", 1).getA(), () -> new A("b", 1).getA())));
    }

    @Test
    public void extractDateTimeTest() {
        Instant a = LocalDateTime.of(2016, 1, 1, 12, 30, 15).atZone(ZoneId.systemDefault()).toInstant();

        LocalDate date = extractDate(a);
        assertThat(date.getYear(), is(2016));
        assertThat(date.getDayOfYear(), is(1));

        LocalTime time = extractTime(a);
        assertThat(time.getHour(), is(12));
        assertThat(time.getMinute(), is(30));
        assertThat(time.getSecond(), is(15));
    }

    @Test
    public void isUUIDTest() {
        assertThat(isUUID("notUuid"), is(false));
        assertThat(isUUID("cba238af-507a-4a65-bb8f-cd446e0fbabc"), is(true));
    }

    @Test
    public void filePresenceCheckExistentFileTest() {
        boolean success = Utils.fileExists("src/test/resources/cz/cas/lib/core/file/test.txt");
        Assert.assertThat(success, is(true));
    }

    @Test
    public void filePresenceCheckNonExistentFileTest() {
        boolean success = Utils.fileExists("/nonExistentPath");
        Assert.assertThat(success, is(false));
    }
}
