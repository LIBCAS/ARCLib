package cz.cas.lib.core.dictionary;

import cz.cas.lib.core.index.solr.SolrDictionaryStore;
import org.springframework.stereotype.Repository;

import static cz.cas.lib.core.util.Utils.toSolrReference;


/**
 * Implementation of {@link cz.cas.lib.core.index.solr.SolrStore} for storing {@link Dictionary} and indexing {@link SolrDictionary}.
 */
@Repository
public class UDictionaryStore extends SolrDictionaryStore<Dictionary, QDictionary, SolrDictionary> {

    public UDictionaryStore() {
        super(Dictionary.class, QDictionary.class, SolrDictionary.class);
    }

    @Override
    public SolrDictionary toIndexObject(Dictionary obj) {
        SolrDictionary indexed = super.toIndexObject(obj);

        indexed.setDescription(obj.getDescription());
        indexed.setParent(toSolrReference(obj.getParent()));
        indexed.setCode(obj.getCode());

        return indexed;
    }

    public Dictionary findByCode(String code) {
        QDictionary qDictionary = qObject();

        Dictionary dictionary = query().select(qDictionary)
                .where(qDictionary.deleted.isNull())
                .where(qDictionary.code.eq(code))
                .fetchFirst();

        detachAll();

        return dictionary;
    }
}
