package cz.inqool.uas.security;


import cz.inqool.uas.security.password.GoodPasswordGenerator;
import helper.DbTest;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class GoodPasswordGeneratorTest extends DbTest {
    private GoodPasswordGenerator passwordGenerator;

    @Test
    public void generateNumberPassword() {
        passwordGenerator = new GoodPasswordGenerator(5, true, false);

        String password = passwordGenerator.generate();

        assertThat(password.length(), is(5));
        assertThat(password.matches("[0-9]+"), is(true));
    }

    @Test
    public void generateAlphabetPassword() {
        passwordGenerator = new GoodPasswordGenerator(5, false, true);

        String password = passwordGenerator.generate();

        assertThat(password.length(), is(5));
        assertThat(password.matches("[0-9]+"), is(false));
    }

    @Test
    public void generateCombinedPassword() {
        passwordGenerator = new GoodPasswordGenerator(5, true, true);

        String password = passwordGenerator.generate();

        assertThat(password.length(), is(5));
        assertThat(password.matches(".*[0-9]+.*"), is(true));
        assertThat(password.matches(".*[a-zA-Z]+.*"), is(true));

    }
}
