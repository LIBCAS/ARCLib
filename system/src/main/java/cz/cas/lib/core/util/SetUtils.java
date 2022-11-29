package cz.cas.lib.core.util;

import lombok.NonNull;

import java.util.HashSet;
import java.util.Set;

public class SetUtils {
    public static <T> Set<T> difference(@NonNull Set<T> set1, @NonNull Set<T> set2) {
        Set<T> diff = new HashSet<>(set1);
        diff.removeAll(new HashSet<>(set2));
        return diff;
    }

    public static <T> Set<T> intersection(@NonNull Set<T> set1, @NonNull Set<T> set2) {
        Set<T> intersection = new HashSet<>(set1);
        intersection.retainAll(new HashSet<>(set2));
        return intersection;
    }

    public static <T> Set<T> union(@NonNull Set<T> set1, @NonNull Set<T> set2) {
        Set<T> union = new HashSet<>(set1);
        union.addAll(new HashSet<>(set2));
        return union;
    }

    public static <T> Set<T> symmetricDiff(@NonNull Set<T> set1, @NonNull Set<T> set2) {
        return union(difference(set1, set2), difference(set2, set1));
    }
}
