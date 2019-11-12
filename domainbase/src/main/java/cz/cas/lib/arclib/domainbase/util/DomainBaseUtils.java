package cz.cas.lib.arclib.domainbase.util;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class DomainBaseUtils {
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

    public static boolean isProxy(Object a) {
        return (AopUtils.isAopProxy(a) && a instanceof Advised);
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

    public static <U, T extends RuntimeException> void notEmpty(Collection c, Supplier<T> supplier) {
        if (c == null || c.isEmpty())
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

}
