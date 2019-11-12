package cz.cas.lib.core.detach.store;

import cz.cas.lib.core.detach.objects.Object1;
import cz.cas.lib.core.detach.objects.QObject1;
import cz.cas.lib.arclib.domainbase.store.DatedStore;

public class Object1Store extends DatedStore<Object1, QObject1> {

    public Object1Store() {
        super(Object1.class, QObject1.class);
    }
}
