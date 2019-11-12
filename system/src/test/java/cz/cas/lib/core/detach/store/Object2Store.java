package cz.cas.lib.core.detach.store;

import cz.cas.lib.core.detach.objects.Object2;
import cz.cas.lib.core.detach.objects.QObject2;
import cz.cas.lib.arclib.domainbase.store.DatedStore;

public class Object2Store extends DatedStore<Object2, QObject2> {

    public Object2Store() {
        super(Object2.class, QObject2.class);
    }
}
