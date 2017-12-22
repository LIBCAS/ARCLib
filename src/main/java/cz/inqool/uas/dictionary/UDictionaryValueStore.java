package cz.inqool.uas.dictionary;

import cz.inqool.uas.index.IndexedDictionaryStore;
import cz.inqool.uas.index.IndexedStore;
import org.springframework.stereotype.Repository;

import static cz.inqool.uas.util.Utils.toLabeledReference;

/**
 * Implementation of {@link IndexedStore} for storing {@link DictionaryValue} and indexing {@link IndexedDictionaryValue}.
 */
@Repository
public class UDictionaryValueStore extends IndexedDictionaryStore<DictionaryValue, QDictionaryValue, IndexedDictionaryValue> {

    public UDictionaryValueStore() {
        super(DictionaryValue.class, QDictionaryValue.class, IndexedDictionaryValue.class);
    }

    @Override
    public IndexedDictionaryValue toIndexObject(DictionaryValue o) {
        IndexedDictionaryValue indexedDictionaryValue = super.toIndexObject(o);

        indexedDictionaryValue.setDictionary(toLabeledReference(o.getDictionary()));
        indexedDictionaryValue.setParent(toLabeledReference(o.getParent()));

        indexedDictionaryValue.setDescription(o.getDescription());
        indexedDictionaryValue.setValidFrom(o.getValidFrom());
        indexedDictionaryValue.setValidTo(o.getValidTo());

        indexedDictionaryValue.setCode(o.getCode());

        return indexedDictionaryValue;
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
