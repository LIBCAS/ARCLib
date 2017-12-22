package helper;

public class ExceptionNotThrownAssertionError extends AssertionError {
    public ExceptionNotThrownAssertionError() {
        super("Expected exception was not thrown.");
    }
}
