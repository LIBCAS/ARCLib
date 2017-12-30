package cz.inqool.uas.detach.store;

import cz.inqool.uas.detach.objects.Object1;
import cz.inqool.uas.detach.objects.QObject1;
import cz.inqool.uas.store.DatedStore;

public class Object1Store extends DatedStore<Object1, QObject1> {

    public Object1Store() {
        super(Object1.class, QObject1.class);
    }
}
