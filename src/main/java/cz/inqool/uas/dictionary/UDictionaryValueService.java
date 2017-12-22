package cz.inqool.uas.dictionary;

import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.MissingAttribute;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.index.dto.Filter;
import cz.inqool.uas.index.dto.FilterOperation;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;

import static cz.inqool.uas.util.Utils.*;

@Service
public class UDictionaryValueService {
    private UDictionaryStore store;

    private UDictionaryValueStore valueStore;

    public DictionaryValue find(String dictionaryId, String id) {
        DictionaryValue value = valueStore.find(id);
        notNull(value, () -> new MissingObject(DictionaryValue.class, id));
        notNull(value.getDictionary(), () -> new MissingAttribute(value, "dictionary"));
        eq(value.getDictionary().getId(), dictionaryId, () -> new BadArgument("dictionaryId"));

        return value;
    }

    /**
     * Saves value of Dictionary
     *
     * Allows to save DictionaryValue with parentValue which does not belong to parent Dictionary, because
     * it might not be saved.
     *
     * @param dictionaryId Id of dictionary this value belongs to
     * @param entity Entity to save
     * @return saved value
     */
    public DictionaryValue save(String dictionaryId, DictionaryValue entity) {
        Dictionary dictionary = store.find(dictionaryId);
        notNull(dictionary, () -> new BadArgument("dictionaryId"));

        entity.setDictionary(dictionary);
        return valueStore.save(entity);
    }

    public void delete(String dictionaryId, DictionaryValue entity) {
        notNull(entity.getDictionary(), () -> new MissingAttribute(entity, "dictionary"));
        eq(entity.getDictionary().getId(), dictionaryId, () -> new BadArgument("dictionaryId"));

        valueStore.delete(entity);
    }

    public Result<DictionaryValue> findAll(String dictionaryId, Params params) {
        Filter filter = new Filter("dictionary.id", FilterOperation.EQ, dictionaryId, null);
        addPrefilter(params, filter);

        return valueStore.findAll(params);
    }

    public List<DictionaryValue> findAllPrefixed(String dictionaryId, String prefix) {
        Params params = new Params();
        params.setSort("name");
        params.setPageSize(100);
        params.setFilter(asList(validAndActiveFilter(), new Filter("name", FilterOperation.STARTWITH, prefix, null)));

        return this.findAll(dictionaryId, params).getItems();
    }

    public List<DictionaryValue> findAllContaining(String dictionaryId, String q) {
        Params params = new Params();
        params.setSort("name");
        params.setPageSize(100);
        params.setFilter(asList(validAndActiveFilter(), new Filter("name", FilterOperation.CONTAINS, q, null)));

        return this.findAll(dictionaryId, params).getItems();
    }

    public List<DictionaryValue> findAll(String dictionaryId) {
        Params params = new Params();
        params.setSort("name");
        params.setPageSize(1000);

        return this.findAll(dictionaryId, params).getItems();
    }

    /**
     * Gets Dictionary by code
     * @param code code of Dictionary
     * @return Dictionary
     */
    public DictionaryValue findByCode(String dictionaryId, String code) {
        return valueStore.findByCode(dictionaryId, code);
    }


    private Filter validAndActiveFilter() {
        String now = LocalDateTime.now().toString();

        return new Filter(null, FilterOperation.AND, null, asList(
            new Filter("active", FilterOperation.EQ, "true", null),

            new Filter(null, FilterOperation.OR, null, asList(
                    new Filter("validTo", FilterOperation.GTE, now, null),
                    new Filter("validTo", FilterOperation.IS_NULL, null, null)
            )),

            new Filter(null, FilterOperation.OR, null, asList(
                new Filter("validFrom", FilterOperation.LTE, now, null),
                new Filter("validFrom", FilterOperation.IS_NULL, null, null)
            ))
        ));
    }

    @Inject
    public void setStore(UDictionaryStore store) {
        this.store = store;
    }

    @Inject
    public void setValueStore(UDictionaryValueStore valueStore) {
        this.valueStore = valueStore;
    }
}
