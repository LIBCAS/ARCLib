package cz.inqool.uas.store;

import org.hibernate.Session;
import org.hibernate.tuple.ValueGenerator;

import java.time.Instant;

public class InstantGenerator implements ValueGenerator<Instant> {
    @Override
    public Instant generateValue(Session session, Object owner) {
        return Instant.now();
    }
}
