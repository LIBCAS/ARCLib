package cz.inqool.uas.detach.store;

import cz.inqool.uas.detach.objects.Object2;
import cz.inqool.uas.detach.objects.QObject2;
import cz.inqool.uas.store.DatedStore;

public class Object2Store extends DatedStore<Object2, QObject2> {

    public Object2Store() {
        super(Object2.class, QObject2.class);
    }
}
