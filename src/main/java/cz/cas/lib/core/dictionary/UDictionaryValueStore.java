package cz.cas.lib.core.dictionary;

import cz.cas.lib.core.index.solr.SolrDictionaryStore;
import org.springframework.stereotype.Repository;

import static cz.cas.lib.core.util.Utils.toSolrReference;


/**
 * Implementation of {@link cz.cas.lib.core.index.solr.SolrStore} for storing {@link DictionaryValue} and indexing {@link SolrDictionaryValue}.
 */
@Repository
public class UDictionaryValueStore extends SolrDictionaryStore<DictionaryValue, QDictionaryValue, SolrDictionaryValue> {

    public UDictionaryValueStore() {
        super(DictionaryValue.class, QDictionaryValue.class, SolrDictionaryValue.class);
    }

    @Override
    public SolrDictionaryValue toIndexObject(DictionaryValue o) {
        SolrDictionaryValue solrDictionaryValue = super.toIndexObject(o);

        solrDictionaryValue.setDictionary(toSolrReference(o.getDictionary()));
        solrDictionaryValue.setParent(toSolrReference(o.getParent()));

        solrDictionaryValue.setDescription(o.getDescription());
        solrDictionaryValue.setValidFrom(o.getValidFrom());
        solrDictionaryValue.setValidTo(o.getValidTo());

        solrDictionaryValue.setCode(o.getCode());

        return solrDictionaryValue;
    }

    public DictionaryValue findByCode(String dictionaryId, String code) {
        QDictionaryValue qDictionaryValue = qObject();

        DictionaryValue value = query().select(qDictionaryValue)
                .where(qDictionaryValue.deleted.isNull())
                .where(qDictionaryValue.dictionary.id.eq(dictionaryId))
                .where(qDictionaryValue.code.eq(code))
                .fetchFirst();

        detachAll();

        return value;
    }
}
