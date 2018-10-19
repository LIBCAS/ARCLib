package cz.cas.lib.core.index.example;

import cz.cas.lib.core.index.solr.SolrDictionaryStore;

import static cz.cas.lib.core.util.Utils.toSolrReference;

public class MyStore extends SolrDictionaryStore<MyObject, QMyObject, MySolrObject> {
    public MyStore() {
        super(MyObject.class, QMyObject.class, MySolrObject.class);
    }

    @Override
    public MySolrObject toIndexObject(MyObject obj) {
        MySolrObject object = super.toIndexObject(obj);

        object.setState(toSolrReference(obj.getState()));

        return object;
    }
}
