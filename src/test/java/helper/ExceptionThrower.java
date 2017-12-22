package helper;

@FunctionalInterface
public interface ExceptionThrower {
    void throwException() throws Throwable;
}
