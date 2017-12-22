package cz.inqool.uas.index.solr;

import static cz.inqool.uas.util.Utils.toSolrReference;

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
