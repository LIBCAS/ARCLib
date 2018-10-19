package cz.cas.lib.core.config.change.checker;

import org.springframework.util.Assert;

import java.util.regex.Pattern;

/**
 * Internal strategy used to sanitize potentially sensitive keys.
 * <p>
 * Based on ASF 2.0 licenced code from Spring Boot.
 *
 * @author Christian Dupuis
 * @author Toshiaki Maki
 * @author Phillip Webb
 * @author Nicolas Lejeune
 * @author Stephane Nicoll
 * @author Matus Zamborsky
 */
class Sanitizer {

    private static final String[] REGEX_PARTS = {"*", "$", "^", "+"};

    private Pattern[] keysToSanitize;

    Sanitizer() {
        setKeysToSanitize("password", "secret", "key", ".*credentials.*",
                "vcap_services");
    }

    /**
     * Keys that should be sanitized. Keys can be simple strings that the property ends
     * with or regex expressions.
     *
     * @param keysToSanitize the keys to sanitize
     */
    void setKeysToSanitize(String... keysToSanitize) {
        Assert.notNull(keysToSanitize, "KeysToSanitize must not be null");
        this.keysToSanitize = new Pattern[keysToSanitize.length];
        for (int i = 0; i < keysToSanitize.length; i++) {
            this.keysToSanitize[i] = getPattern(keysToSanitize[i]);
        }
    }

    private Pattern getPattern(String value) {
        if (isRegex(value)) {
            return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
        }
        return Pattern.compile(".*" + value + "$", Pattern.CASE_INSENSITIVE);
    }

    private boolean isRegex(String value) {
        for (String part : REGEX_PARTS) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sanitize the given value if necessary.
     *
     * @param key   the key to sanitize
     * @param value the value
     * @return the potentially sanitized value
     */
    Object sanitize(String key, Object value) {
        for (Pattern pattern : this.keysToSanitize) {
            if (pattern.matcher(key).matches()) {
                return (value == null ? null : "******");
            }
        }
        return value;
    }

}
