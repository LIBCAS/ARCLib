package cz.inqool.uas.domain;

import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class DomainObjectTest {

    @Test
    public void idTest() {
        DomainEntity entity = new DomainEntity();

        assertThat(entity.getId(), is(notNullValue()));
        assertThat(UUID.fromString(entity.getId()), is(notNullValue()));
    }

    @Test
    public void toStringTest() {
        DomainEntity entity = new DomainEntity();

        assertThat(entity.toString(), containsString(DomainEntity.class.getSimpleName()));
        assertThat(entity.toString(), containsString(entity.getId()));
    }

    @Test
    public void equalsFalseTest() {
        DomainEntity e1 = new DomainEntity();
        DomainEntity e2 = new DomainEntity();

        e1.setTest("test1");
        e2.setTest("test2");

        assertThat(e1.equals(e2), is(false));
        assertThat(e1.hashCode() == e2.hashCode(), is(false));
    }

    @Test
    public void equalsTrueTest() {
        DomainEntity e1 = new DomainEntity();
        DomainEntity e2 = new DomainEntity();

        e1.setTest("test1");
        e2.setTest("test2");

        e2.setId(e1.getId());

        assertThat(e1.equals(e2), is(true));
        assertThat(e1.hashCode() == e2.hashCode(), is(true));
    }
}
