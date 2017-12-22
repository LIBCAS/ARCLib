package cz.inqool.uas.util;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import cz.inqool.uas.domain.DictionaryObject;
import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.exception.GeneralException;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.index.IndexedDictionaryObject;
import cz.inqool.uas.index.Labeled;
import cz.inqool.uas.index.LabeledReference;
import cz.inqool.uas.index.dto.Filter;
import cz.inqool.uas.index.dto.FilterOperation;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.RootFilterOperation;
import cz.inqool.uas.index.solr.SolrReference;
import lombok.AllArgsConstructor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.*;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Utils {
    public static <T, U> List<U> map(List<T> objects, Function<T, U> func) {
        if (objects != null) {
            return objects.stream()
                          .map(func)
                          .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public static <T, U> Set<U> map(Set<T> objects, Function<T, U> func) {
        if (objects != null) {
            return objects.stream()
                          .map(func)
                          .collect(Collectors.toSet());
        } else {
            return null;
        }
    }

    public static <T, U> Map<T, U> asMap(T key, U value) {
        Map<T, U> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    public static <T, U> Map<T, U> asMap(T key1, U value1, T key2, U value2) {
        Map<T, U> map = new LinkedHashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    public static <T> List<T> asList(Collection<T> a) {
        return a.stream().collect(Collectors.toList());
    }

    public static <T> List<T> asList(T... a) {
        return Arrays.asList(a);
    }

    public static <T> T[] asArray(T... a) {
        return a;
    }

    public static <T> List<T> asList(Collection<T> base, T... a) {
        List<T> list = new ArrayList<>(base);
        list.addAll(Arrays.asList(a));

        return list;
    }

    public static <T> Set<T> asSet(Collection<T> base, T... a) {
        Set<T> set = new LinkedHashSet<>(base);
        set.addAll(Arrays.asList(a));

        return set;
    }

    public static <T> Set<T> asSet(Collection<T> a) {
        return new HashSet<>(a);
    }

    public static <T> Set<T> asSet(T... a) {
        return new HashSet<>(Arrays.asList(a));
    }

    public static <T> Object[] asObjectArray(T... a) {
        return Arrays.copyOf(a, a.length, Object[].class);
    }

    public static <T extends RuntimeException> void notNull(Object o, Supplier<T> supplier) {
        if (o == null) {
            throw supplier.get();
        } else if (o instanceof Optional) {
            if(!((Optional)o).isPresent()) {
                throw supplier.get();
            }
        } else if (isProxy(o)) {
            if (unwrap(o) == null) {
                throw supplier.get();
            }
        }
    }

    public static <T extends RuntimeException> void isNull(Object o, Supplier<T> supplier) {
        if (o instanceof Optional) {
            if(((Optional)o).isPresent()) {
                throw supplier.get();
            }
        } else if (isProxy(o)) {
            if (unwrap(o) != null) {
                throw supplier.get();
            }
        } else if (o != null) {
            throw supplier.get();
        }
    }

    public static <T extends Exception> void notNullEx(Object o, Supplier<T> supplier) throws T {
        if (o == null) {
            throw supplier.get();
        }
    }

    public static Instant plus(Instant time, TemporalUnit unit, int value) {
        return LocalDateTime.ofInstant(time, ZoneOffset.UTC).plus(value, unit).toInstant(ZoneOffset.UTC);
    }

    public static <U, T extends RuntimeException> void eq(U o1,  U o2, Supplier<T> supplier) {
        if (!Objects.equals(o1, o2)) {
            throw supplier.get();
        }
    }

    public static <U, T extends RuntimeException> void in(U o1,  Set<U> os2, Supplier<T> supplier) {
        if (!os2.contains(o1)) {
            throw supplier.get();
        }
    }

    public static <U, T extends RuntimeException> void ne(U o1,  U o2, Supplier<T> supplier) {
        if (Objects.equals(o1, o2)) {
            throw supplier.get();
        }
    }

    public static <U, T extends RuntimeException> void nin(U o1,  Set<U> os2, Supplier<T> supplier) {
        if (os2.contains(o1)) {
            throw supplier.get();
        }
    }

    public static <T extends RuntimeException> void in(Integer n, Integer min, Integer max, Supplier<T> supplier) {
        if (n < min || n > max) {
            throw supplier.get();
        }
    }

    public static <T extends RuntimeException> void gte(Integer n, Integer l, Supplier<T> supplier) {
        if (n < l) {
            throw supplier.get();
        }
    }

    public static <T extends RuntimeException> void gt(BigDecimal n, BigDecimal l, Supplier<T> supplier) {
        if (n.compareTo(l) <= 0) {
            throw supplier.get();
        }
    }

    public static <T> void ifPresent(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    @FunctionalInterface
    public interface Checked {
        void checked() throws Exception;
    }

    public static void checked(Checked method) {
        try {
            method.checked();
        } catch (Exception ex) {
            if (ex instanceof GeneralException) {
                throw (GeneralException)ex;
            } else {
                throw new GeneralException(ex);
            }

        }
    }

    public static <T extends RuntimeException> void checked(Checked method, Supplier<T> supplier ) {
        try {
            method.checked();
        } catch (Exception ex) {
            throw supplier.get();
        }
    }

    public static Double toDouble(BigDecimal decimal) {
        if (decimal == null) {
            return null;
        }

        return decimal.doubleValue();
    }

    public static Date toDate(Instant instant) {
        if (instant == null) {
            return null;
        }

        return Date.from(instant);
    }

    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        ZonedDateTime zdt = dateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    public static Date toDate(LocalDate date) {
        if (date == null) {
            return null;
        }

        ZonedDateTime zdt = date.atStartOfDay().atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    public static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }

    public static LocalDate extractDate(Instant instant) {
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        return zdt.toLocalDate();
    }

    public static LocalTime extractTime(Instant instant) {
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        return zdt.toLocalTime();
    }

    public static boolean isUUID(String id) {
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean isProxy(Object a) {
        return (AopUtils.isAopProxy(a) && a instanceof Advised);
    }

    public static <T> T unwrap(T a) {
        if(isProxy(a)) {
            try {
                return (T) ((Advised)a).getTargetSource().getTarget();
            } catch (Exception ignored) {
                // return null if not in scope
                return null;
            }
        } else {
            return a;
        }
    }

    public static <T extends DomainObject> List<T> sortByIdList(List<String> ids, Iterable<T> objects) {
        Map<String, T> map = StreamSupport.stream(objects.spliterator(), true)
                .collect(Collectors.toMap(DomainObject::getId, o -> o));

        return ids.stream()
                .map(map::get)
                .filter(o -> o != null)
                .collect(Collectors.toList());
    }

    public static <T> List<T> reverse(List<T> input) {
        List<T> output = new ArrayList<>(input);

        Collections.reverse(output);
        return output;
    }

    public static <T> T[] reverse(T[] array) {
        T[] copy = array.clone();
        Collections.reverse(Arrays.asList(copy));
        return copy;
    }

    public static <T extends DomainObject> LabeledReference toLabeledReference(T obj, Function<T, String> nameMapper) {
        if (obj != null) {
            return new LabeledReference(obj.getId(), nameMapper.apply(obj));
        } else {
            return null;
        }
    }

    public static <T extends DictionaryObject> LabeledReference toLabeledReference(T obj) {
        if (obj != null) {
            return new LabeledReference(obj.getId(), obj.getName());
        } else {
            return null;
        }
    }

    public static <T extends Labeled> LabeledReference toLabeledReference(T obj) {
        if (obj != null) {
            return new LabeledReference(obj.name(), obj.getLabel());
        } else {
            return null;
        }
    }

    public static <T extends Enum> LabeledReference toLabeledReference(T obj, Function<T, String> nameMapper) {
        if (obj != null) {
            return new LabeledReference(obj.toString(), nameMapper.apply(obj));
        } else {
            return null;
        }
    }

    public static LabeledReference toLabeledReference(IndexedDictionaryObject obj) {
        if (obj != null) {
            return new LabeledReference(obj.getId(), obj.getName());
        } else {
            return null;
        }
    }

    public static <T extends DomainObject> SolrReference toSolrReference(T obj, Function<T, String> nameMapper) {
        if (obj != null) {
            return new SolrReference(obj.getId(), nameMapper.apply(obj));
        } else {
            return null;
        }
    }

    public static <T extends DictionaryObject> SolrReference toSolrReference(T obj) {
        if (obj != null) {
            return new SolrReference(obj.getId(), obj.getName());
        } else {
            return null;
        }
    }

    public static <T extends Labeled> SolrReference toSolrReference(T obj) {
        if (obj != null) {
            return new SolrReference(obj.name(), obj.getLabel());
        } else {
            return null;
        }
    }

    public static <T extends Enum> SolrReference toSolrReference(T obj, Function<T, String> nameMapper) {
        if (obj != null) {
            return new SolrReference(obj.toString(), nameMapper.apply(obj));
        } else {
            return null;
        }
    }

    public static SolrReference toSolrReference(IndexedDictionaryObject obj) {
        if (obj != null) {
            return new SolrReference(obj.getId(), obj.getName());
        } else {
            return null;
        }
    }

    public static InputStream resource(String path) throws IOException {
        try {
            URL url = Resources.getResource(path);
            ByteSource source = Resources.asByteSource(url);
            return source.openStream();
        } catch (IllegalArgumentException ex) {
            throw new MissingObject("template", path);
        }
    }

    public static byte[] resourceBytes(String path) throws IOException {
        try {
            URL url = Resources.getResource(path);
            return Resources.toByteArray(url);
        } catch (IllegalArgumentException ex) {
            throw new MissingObject("template", path);
        }
    }

    public static String resourceString(String path) throws IOException {
        try {
            URL url = Resources.getResource(path);
            return Resources.toString(url, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new MissingObject("template", path);
        }
    }

    public static String join(Collection<String> data) {
        if (data == null) {
            return "";
        }

        return data.stream()
                   .collect(Collectors.joining(", "));
    }

    public static <T> String join(Collection<T> data, Function<T, String> nameMapper) {
        if (data == null) {
            return "";
        }

        return data.stream()
                   .map(nameMapper)
                   .collect(Collectors.joining(", "));
    }

    @SuppressWarnings("unchecked")
    public static <T> T coalesce(Supplier<T>... ts) {
        return asList(ts)
                .stream()
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the value from object by mapper in case the object is not null
     */
    public static <T, U> U get(T obj, Function<T, U> mapper, U defaultValue) {
        if (obj != null) {
            return mapper.apply(obj);
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the value from object by mapper in case the object is not null
     */
    public static <T, U> U get(T obj, Function<T, U> mapper) {
        return get(obj, mapper, null);
    }

    /**
     * Returns supplier for specified value
     * @param v value to return
     * @param <T> type of the value
     * @return supplier
     */
    public static <T> Supplier<T> val(T v) {
        return new ValueSupplier<>(v);
    }

    @AllArgsConstructor
    static class ValueSupplier<T> implements Supplier<T> {
        private T value;

        @Override
        public T get() {
            return value;
        }
    }

    public static <T, U> boolean contains(Collection<T> collection, Function<T, U> mapper, U value) {
        return collection.stream()
                .map(mapper)
                .anyMatch(p -> p.equals(value));
    }

    public static <T, U> T get(Collection<T> collection, Function<T, U> mapper, U value) {
        return collection.stream()
                .filter(t -> Objects.equals(mapper.apply(t), value))
                .findAny()
                .orElse(null);
    }

    public static <T, U> T getItem(Collection<T> collection, Function<T, U> mapper, U value) {
        return get(collection, mapper, value);
    }

    public static String normalize(String s) {
        if (s != null) {
            return stripAccents(s).toLowerCase();
        } else {
            return null;
        }
    }

    public static String stripAccents(String s) {
        if (s != null) {
            s = Normalizer.normalize(s, Normalizer.Form.NFD);
            s = s.replaceAll("[^\\p{ASCII}]", "");
            return s;
        } else {
            return null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isNumber(String text) {
        try {
            Integer.valueOf(text);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static String sanitizeElasticsearch(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // These characters are part of the query syntax and must be escaped
            // matus removed those: - and :
            if (c == '\\' || c == '+' || c == '!' || c == '(' || c == ')'
                    || c == '^' || c == '[' || c == ']' || c == '\"'
                    || c == '{' || c == '}' || c == '~' || c == '*' || c == '?'
                    || c == '|' || c == '&' || c == '/') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Adds pre-filter to params and wrap previous filters in one filter (with selected operation)
     *
     * @param params Params object to change
     * @param preFilter Prefilter to apply
     */
    public static void addPrefilter(Params params, Filter preFilter) {
        Filter oldRootFilter = new Filter();
        oldRootFilter.setOperation(params.getOperation() == RootFilterOperation.AND ? FilterOperation.AND : FilterOperation.OR);
        oldRootFilter.setFilter(params.getFilter());

        params.setOperation(RootFilterOperation.AND);
        params.setFilter(asList(oldRootFilter, preFilter));
    }
}
