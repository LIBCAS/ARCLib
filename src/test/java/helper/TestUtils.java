package helper;


import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.time.Instant;

public class TestUtils {
    public static org.hamcrest.Matcher<Instant> closeTo(Instant operand, long error) {
        return IsInstantCloseTo.closeTo(operand, error);
    }

    /**
     * Is the value a number equal to a value within some range of
     * acceptable error?
     */
    public static class IsInstantCloseTo extends TypeSafeMatcher<Instant> {
        private final long delta;
        private final Instant value;

        public IsInstantCloseTo(Instant value, long error) {
            this.delta = error;
            this.value = value;
        }

        @Override
        public boolean matchesSafely(Instant item) {
            return actualDelta(item) <= 0.0;
        }

        @Override
        public void describeMismatchSafely(Instant item, Description mismatchDescription) {
            mismatchDescription.appendValue(item)
                               .appendText(" differed by ")
                               .appendValue(actualDelta(item));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a numeric value within ")
                       .appendValue(delta)
                       .appendText(" of ")
                       .appendValue(value);
        }

        private long actualDelta(Instant item) {
            return (Math.abs((item.toEpochMilli() - value.toEpochMilli())) - delta);
        }

        /**
         * Creates a matcher of {@link Instant}s that matches when an examined Instant is equal
         * to the specified <code>operand</code>, within a range of +/- <code>error</code> miliseconds.
         * <p/>
         *
         * @param operand
         *     the expected value of matching Instants
         * @param error
         *     the delta (+/-) miliseconds within which matches will be allowed
         */
        @Factory
        public static Matcher<Instant> closeTo(Instant operand, long error) {
            return new IsInstantCloseTo(operand, error);
        }
    }
}
