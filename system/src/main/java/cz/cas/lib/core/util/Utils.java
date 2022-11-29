package cz.cas.lib.core.util;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import cz.cas.lib.arclib.exception.BadRequestException;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.RootFilterOperation;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
            if (!((Optional) o).isPresent()) {
                throw supplier.get();
            }
        } else if (isProxy(o)) {
            if (unwrap(o) == null) {
                throw supplier.get();
            }
        }
    }

    public static <U, T extends RuntimeException> void notEmpty(Collection<U> c, Supplier<T> supplier) {
        if (c == null || c.isEmpty())
            throw supplier.get();
    }

    public static <U, T extends RuntimeException> void isEmpty(Collection<U> c, Supplier<T> supplier) {
        if (c == null || !c.isEmpty())
            throw supplier.get();
    }

    public static <T extends RuntimeException> void isNull(Object o, Supplier<T> supplier) {
        if (o instanceof Optional) {
            if (((Optional) o).isPresent()) {
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

    public static <U, T extends RuntimeException> void eq(U o1, U o2, Supplier<T> supplier) {
        if (!Objects.equals(o1, o2)) {
            throw supplier.get();
        }
    }

    public static <U, T extends RuntimeException> void in(U o1, Set<U> os2, Supplier<T> supplier) {
        if (!os2.contains(o1)) {
            throw supplier.get();
        }
    }

    public static <U, T extends RuntimeException> void ne(U o1, U o2, Supplier<T> supplier) {
        if (Objects.equals(o1, o2)) {
            throw supplier.get();
        }
    }

    public static <U, T extends RuntimeException> void nin(U o1, Set<U> os2, Supplier<T> supplier) {
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

    public static <T extends RuntimeException> void lte(Integer n, Integer l, Supplier<T> supplier) {
        if (n > l) {
            throw supplier.get();
        }
    }

    public static <T extends RuntimeException> void lt(BigDecimal n, BigDecimal l, Supplier<T> supplier) {
        if (n.compareTo(l) >= 0) {
            throw supplier.get();
        }
    }

    public static <T> void ifPresent(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    /**
     * Checks whether the file at the path exists
     *
     * @param path path to the file
     * @return true if the file exists, false otherwise
     */
    public static boolean fileExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static boolean fileExists(Path path) {
        return fileExists(path.toString());
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
                throw (GeneralException) ex;
            } else {
                throw new GeneralException(ex);
            }

        }
    }

    public static <T extends RuntimeException> void checked(Checked method, Supplier<T> supplier) {
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

    public static void checkUUID(String id) throws BadRequestException {
        try {
            UUID.fromString(id);
        } catch (Exception e) {
            throw new BadRequestException(id + " is not valid UUID");
        }
    }

    public static boolean isProxy(Object a) {
        return (AopUtils.isAopProxy(a) && a instanceof Advised);
    }

    public static <T> T unwrap(T a) {
        if (isProxy(a)) {
            try {
                return (T) ((Advised) a).getTargetSource().getTarget();
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
     *
     * @param v   value to return
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

    /**
     * Adds pre-filter to params and wrap previous filters in one filter (with selected operation)
     *
     * @param params    Params object to change
     * @param preFilter Prefilter to apply
     */
    public static void addPrefilter(Params params, Filter preFilter) {
        if (params.isPrefilterAdded()) {
            params.addFilter(preFilter);
            return;
        }
        Filter oldRootFilter = new Filter();
        oldRootFilter.setOperation(params.getOperation() == RootFilterOperation.AND ? FilterOperation.AND : FilterOperation.OR);
        oldRootFilter.setFilter(params.getFilter());

        params.setOperation(RootFilterOperation.AND);
        params.setFilter(asList(oldRootFilter, preFilter));
        params.setPrefilterAdded(true);

    }

    public static String bytesToHexString(byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    /**
     * Lists files matching glob pattern
     *
     * @param root        root directory
     * @param globPattern glob pattern
     * @return list of files matching glob pattern
     */
    public static List<File> listFilesMatchingGlobPattern(File root, String globPattern) throws IOException {
        return listFilesMatchingPattern(root, globPattern, "glob");
    }

    /**
     * Lists files matching regex
     *
     * @param root                      root directory
     * @param regex                     regex - relative path from the root to the file, using / (slash) as dir separator, e.g. <em>subfolder/.+/bo*m</em> to find <em>bm</em> or <em>booom</em> somewhere in <em>subfolder</em>
     * @param startRegexWithRootDirName if true, then the regex must match the root dir name first..example: to find file at path fox/file.xml (fox = root dir)
     *                                  <ul>
     *                                      <li>if startRegexWithRootDirName=true, regex could be: <em>fox\/\w+.xml</em></li>
     *                                      <li>if startRegexWithRootDirName=true, regex could be: <em>\w+.xml</em></li>
     *                                  </ul>
     * @return list of files matching regex
     */
    public static List<File> listFilesMatchingRegex(File root, String regex, boolean startRegexWithRootDirName) throws IOException {
        if (!root.isDirectory()) {
            throw new IllegalArgumentException(root + " is no directory.");
        }
        String prefix = startRegexWithRootDirName ? "/" + root.getName() : "";
        return Files.walk(root.toPath()).filter(f -> {
            String relativePathWithLinuxSeparators = prefix + f.toAbsolutePath().toString().replace(root.toPath().toAbsolutePath().toString(), "").replace("\\", "/");
            if (relativePathWithLinuxSeparators.length() < 1)
                return false;
            return relativePathWithLinuxSeparators.substring(1).matches(regex);
        }).map(Path::toFile).collect(Collectors.toList());
    }


    public static List<String> readLinesOfInputStreamToList(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    public static InputStream stringToInputStream(String text) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8.name()));
    }

    public static boolean isNullOrEmptyString(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static void executeProcessDefaultResultHandle(String... cmd) {
        File tmp = null;
        try {
            tmp = File.createTempFile("out", null);
            tmp.deleteOnExit();
            final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true).redirectOutput(tmp);
            final Process process = processBuilder.start();
            final int exitCode = process.waitFor();
            if (exitCode != 0)
                throw new IllegalStateException("Process: " + Arrays.toString(cmd) + " has failed " + Files.readAllLines(tmp.toPath()));
        } catch (InterruptedException | IOException ex) {
            throw new GeneralException("unexpected error while executing process", ex);
        } finally {
            if (tmp != null)
                tmp.delete();
        }
    }

    /**
     * @param mergeOutputs merge stdout and stderr outputs
     * @param cmd          cmd to execute
     * @return Pair with return code as key and output (list of lines) as value
     */
    public static Pair<Integer, List<String>> executeProcessCustomResultHandle(boolean mergeOutputs, String... cmd) {
        File stdFile = null;
        File errFile = null;
        try {
            stdFile = File.createTempFile("std.out", null);
            errFile = File.createTempFile("err.out", null);
            final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            if (mergeOutputs)
                processBuilder.redirectErrorStream(true);
            else
                processBuilder.redirectError(errFile);
            processBuilder.redirectOutput(stdFile);
            final Process process = processBuilder.start();
            final int exitCode = process.waitFor();
            List<String> output;
            if (mergeOutputs || exitCode == 0)
                output = Files.readAllLines(stdFile.toPath());
            else
                output = Files.readAllLines(errFile.toPath());
            return Pair.of(exitCode, output);
        } catch (InterruptedException | IOException ex) {
            throw new GeneralException("unexpected error while executing process", ex);
        } finally {
            if (stdFile != null)
                stdFile.delete();
            if (errFile != null)
                errFile.delete();
        }
    }

    public static boolean isIsoDateTimeFormat(String input) {
        if (input == null)
            return false;
        try {
            LocalDateTime.parse(input, DateTimeFormatter.ISO_DATE_TIME);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static List<FileInputStream> getDirectoryInputStreams(Path directoryPath, boolean includeHiddenFiles) throws IOException {
        File directory = directoryPath.toFile();
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory");
        }
        List<FileInputStream> fileStreams = new ArrayList<>();
        collectFiles(directory, fileStreams, includeHiddenFiles);
        return fileStreams;
    }

    private static void collectFiles(File directory, List<FileInputStream> fileInputStreams, boolean includeHiddenFiles) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File file : files) {
                try {
                    if (includeHiddenFiles || !Files.isHidden(file.toPath())) {
                        if (file.isDirectory()) {
                            collectFiles(file, fileInputStreams, includeHiddenFiles);
                        } else {
                            fileInputStreams.add(new FileInputStream(file));
                        }
                    }
                } catch (Exception e) {
                    fileInputStreams.forEach(IOUtils::closeQuietly);
                    throw e;
                }
            }
        }
    }

    public static String toString(Collection<?> c) {
        return c.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    /**
     * Converts string to enum equivalent.
     * <p>
     * String is converted to uppercase before conversion.
     * </p>
     *
     * @param value            string value
     * @param enumerationClass enum class
     * @return Enum equivalent of string or null if input does not match any enum equivalent or is null.
     */
    public static <T extends Enum<T>> T toEnum(String value, Class<T> enumerationClass) {
        for (Enum enumeration : enumerationClass.getEnumConstants()) {
            if (enumeration.toString().equals(value.toUpperCase()))
                return enumeration.valueOf(enumerationClass, value.toUpperCase());
        }
        return null;
    }

    /**
     * Converts instant to CRON expression triggering once at the given date time
     *
     * @param instant instant to convert to CRON expression
     * @return CRON expression of 6 fields in the format `%s %s %s %s %s %s`. From left to right it matches the seconds,
     * minutes, hours, day of month and month of the respective instant. The last field that represents
     * the day of week is set to `?`.
     */
    public static String toCron(Instant instant) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return String.format("%s %s %s %s %s %s",
                String.valueOf(localDateTime.getSecond()),
                String.valueOf(localDateTime.getMinute()),
                String.valueOf(localDateTime.getHour()),
                String.valueOf(localDateTime.getDayOfMonth()),
                String.valueOf(localDateTime.getMonthValue()),
                "?");
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }


    private static List<File> listFilesMatchingPattern(File root, String pattern, String patternName) throws IOException {
        assert patternName.equals("regex") || patternName.equals("glob");
        final List<File> result = new ArrayList<>();

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(String.format("%s:%s", patternName, pattern));
        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attribs) {
                if (matcher.matches(root.toPath().relativize(file))) {
                    result.add(new File(file.toAbsolutePath().toString()));
                }
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(root.toPath(), visitor);
        return result;
    }

    public static boolean pathIsNestedInParent(@NonNull String path, @NonNull Collection<String> parentPaths) {
        Path childPath = Paths.get(path);
        return parentPaths.stream().anyMatch(parentPath -> childPath.normalize().toAbsolutePath().startsWith(Paths.get(parentPath).normalize().toAbsolutePath()));
    }
}
