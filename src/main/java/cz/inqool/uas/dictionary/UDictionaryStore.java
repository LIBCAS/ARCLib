package cz.inqool.uas.dictionary;

import cz.inqool.uas.index.IndexedDictionaryStore;
import cz.inqool.uas.index.IndexedStore;
import org.springframework.stereotype.Repository;

import static cz.inqool.uas.util.Utils.toLabeledReference;

/**
 * Implementation of {@link IndexedStore} for storing {@link Dictionary} and indexing {@link IndexedDictionary}.
 */
@Repository
public class UDictionaryStore extends IndexedDictionaryStore<Dictionary, QDictionary, IndexedDictionary> {

    public UDictionaryStore() {
        super(Dictionary.class, QDictionary.class, IndexedDictionary.class);
    }

    @Override
    public IndexedDictionary toIndexObject(Dictionary obj) {
        IndexedDictionary indexed = super.toIndexObject(obj);

        indexed.setDescription(obj.getDescription());
        indexed.setParent(toLabeledReference(obj.getParent()));
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
