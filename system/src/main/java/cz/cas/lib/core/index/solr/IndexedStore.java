package cz.cas.lib.core.index.solr;

import cz.cas.lib.arclib.domainbase.domain.DatedObject;
import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.arclib.domainbase.exception.GeneralException;
import cz.cas.lib.arclib.domainbase.store.DatedStore;
import cz.cas.lib.arclib.domainbase.store.DomainStore;
import cz.cas.lib.arclib.index.autocomplete.AutoCompleteAware;
import cz.cas.lib.arclib.index.autocomplete.AutoCompleteItem;
import cz.cas.lib.arclib.index.solr.IndexQueryUtils;
import cz.cas.lib.core.index.Indexed;
import cz.cas.lib.core.index.SolrDocument;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.arclib.index.solr.IndexQueryUtils.*;

/**
 * {@link DatedStore} with automatic Solr indexing and filtering.
 * <p>
 * <p>
 * First purpose of this extension is to hook into {@link DomainStore#save(DomainObject)} method and using defined
 * {@link IndexedStore#toIndexObject(DomainObject)} method automatically construct Solr entity from
 * JPA entity and sending it into Solr.
 * </p>
 * <p>
 * Second purpose is retrieval of instances based on complex {@link Params} which encompass filtering, sorting and
 * paging.
 * </p>
 * <p>
 * {@link IndexedStore} works only on entities extending {@link DatedObject}.
 * </p>
 *
 * @param <T> type of JPA entity
 * @param <U> type of corresponding Solr entity
 */
@SuppressWarnings("WeakerAccess")
public interface IndexedStore<T extends DomainObject, U extends IndexedDomainObject> {
    Logger indexedStoreLogger = LoggerFactory.getLogger(IndexedStore.class);

    Collection<T> findAll();

    List<T> findAllInList(List<String> ids);

    SolrClient getSolrClient();

    Class<T> getType();

    Class<U> getUType();

    U toIndexObject(T obj);

    default T save(T entity) {
        if (isParentStore()) {
            removeIndex(entity);
        }
        return index(entity);
    }

    default Collection<? extends T> save(Collection<? extends T> entities) {
        if (isParentStore()) {
            entities.forEach(this::removeIndex);
        }
        return index(entities);
    }

    default void delete(T entity) {
        removeIndex(entity);
    }

    /**
     * Reindexes all entities from JPA to Solr.
     * <p>
     * Also creates the mapping for type.
     * </p>
     * <p>
     * This method should be used only if the index was previously deleted and recreated. Does not remove old
     * mapping and instances from Solr.
     * </p>
     */
    default void reindex() {
        String coreLogId = "type: " + this.getIndexType() + ", core: " + getIndexCollection();
        if (isChildStore())
            return;
        Collection<T> instances = findAll();
        if (instances.isEmpty()) {
            indexedStoreLogger.trace(coreLogId + " - this store has no records to index");
            return;
        }
        indexedStoreLogger.debug(coreLogId + " - reindexing " + instances.size() + " records");
        int counter = 0;
        for (T instance : instances) {
            index(instance);
            counter++;
            if (counter % 20 == 0 || counter == instances.size()) {
                indexedStoreLogger.debug(coreLogId + " - reindexed " + counter + " records");
            }
        }
        indexedStoreLogger.trace(coreLogId + " - reindexed all " + instances.size() + " records");
        instances.forEach(this::index);
    }

    /**
     * Deletes all documents from SOLR and reindexes all records from DB
     */
    default void dropReindex() {
        indexedStoreLogger.debug("drop-reindexing records of type: {} from core: {}", this.getIndexType(), getIndexCollection());
        removeAllIndexes();
        reindex();
    }

    /**
     * Finds all instances that respect the selected {@link Params}.
     * <p>
     * <p>
     * Though {@link Params} one could specify filtering, sorting and paging. For further explanation
     * see {@link Params}.
     * </p>
     * <p>
     * Returning also the total number of instances passed through the filtering phase.
     * </p>
     *
     * @param params Parameters to comply with
     * @return Sorted {@link List} of instances with total number
     */
    default Result<T> findAll(Params params) {
        SolrQuery query = new SolrQuery("*:*");
        Map<String, IndexField> indexedFields = IndexQueryUtils.INDEXED_FIELDS_MAP.get(getIndexType());
        initializeQuery(query, params, indexedFields);

        query.addField("id");
        query.addFilterQuery(typeCriteria());
        query.addFilterQuery(buildFilters(params, getIndexType(), indexedFields));
        Result<T> result = new Result<>();
        QueryResponse queryResponse;
        try {
            queryResponse = getSolrClient().query(getIndexCollection(), query);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        List<U> beans = queryResponse.getBeans(getUType());
        List<String> ids = beans.stream().map(IndexedDomainObject::getId).collect(Collectors.toList());
        List<T> sorted = findAllInList(ids);
        result.setItems(sorted);
        result.setCount(queryResponse.getResults().getNumFound());
        return result;
    }

    default Result<T> findAllIgnorePagination(Params passedParams) {
        int page = 0;
        Params params = passedParams.copy();
        params.setPageSize(solrMaxRows);
        Result<T> allDocsResult = new Result<>(new LinkedList<>(), 0L);
        Result<T> allDocsSubResult;
        do {
            params.setPage(page);
            allDocsSubResult = findAll(params);
            allDocsResult.setCount(allDocsResult.getCount() + allDocsSubResult.getItems().size());
            allDocsResult.getItems().addAll(allDocsSubResult.getItems());
            page++;
        } while (allDocsSubResult.getItems().size() == solrMaxRows);
        return allDocsResult;
    }

    /**
     * Finds all instances that respect the selected {@link Params} and converts them to autocomplete entries.
     * <p>
     * The type {@link T} must implement interface {@link AutoCompleteAware}
     * or override generic AutoComplete logic (more at JavaDoc of {@link #isAutoCompleteSearchAllowed()}),
     * otherwise an exception is thrown.
     *
     * @param params parameters to comply with
     * @return autocomplete items wrapped by the {@link Result} class
     * @throws UnsupportedOperationException if {@link #isAutoCompleteSearchAllowed} check fails
     * @implSpec Only index is queried and not database, therefore it is a faster search than {@link
     * #findAll(Params)}
     */
    default Result<AutoCompleteItem> listAutoComplete(@NonNull Params params) throws UnsupportedOperationException {
        if (!isAutoCompleteSearchAllowed()) {
            throw new UnsupportedOperationException("Type class '" + getType() + "' must implement interface '" + AutoCompleteAware.class.getName() + "'.");
        }

        SolrQuery query = new SolrQuery("*:*");
        Map<String, IndexField> indexedFields = IndexQueryUtils.INDEXED_FIELDS_MAP.get(getIndexType());
        initializeQuery(query, params, indexedFields);

        query.addField("id");
        query.addField(AUTOCOMPLETE_FIELD_NAME);
        query.addFilterQuery(typeCriteria());
        query.addFilterQuery(buildFilters(params, getIndexType(), indexedFields));

        QueryResponse queryResponse;
        try {
            queryResponse = getSolrClient().query(getIndexCollection(), query);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        List<U> beans = queryResponse.getBeans(getUType());

        List<AutoCompleteItem> queriedAutoCompleteData = beans.stream().map(u -> {
            AutoCompleteItem item = new AutoCompleteItem();
            item.setId(u.getId());
            if (u.getAutoCompleteLabel() != null) {
                item.setAutoCompleteLabel(u.getAutoCompleteLabel());
            }
            return item;
        }).collect(Collectors.toList());

        Result<AutoCompleteItem> result = new Result<>();
        result.setItems(queriedAutoCompleteData);
        result.setCount(queryResponse.getResults().getNumFound());

        return result;
    }

    default String typeCriteria() {
        return IndexQueryUtils.TYPE_FIELD + ":" + getIndexType();
    }

    /**
     * Gets index type of the object.
     * <p>
     * One index collection may contain objects of multiple types. The type attribute is used to distinguish between these objects.
     * </p>
     *
     * @return Name of Solr type
     */
    String getIndexType();

    /**
     * Gets Solr collection
     *
     * @return Name of Solr collection
     */
    default String getIndexCollection() {
        SolrDocument document = getUType().getAnnotation(SolrDocument.class);

        if (document != null && StringUtils.isNotBlank(document.collection())) {
            return document.collection();
        } else {
            throw new GeneralException("Missing Solr @SolrDocument.collection for " + getUType().getSimpleName());
        }
    }

    default void removeIndex(T obj) {
        try {
            getSolrClient().deleteById(getIndexCollection(), obj.getId());
            getSolrClient().commit(getIndexCollection());
            if (isParentStore()) {
                SolrClient client = getSolrClient();
                client.deleteByQuery(getIndexCollection(), "_root_:" + obj.getId());
                client.commit(getIndexCollection());
            }
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    default void removeAllIndexes() {
        indexedStoreLogger.trace("removing all records of type: {} from core: {} ", this.getIndexType(), getIndexCollection());
        SolrClient client = getSolrClient();
        try {
            client.deleteByQuery(getIndexCollection(), IndexQueryUtils.TYPE_FIELD + ":" + this.getIndexType());
            client.commit(getIndexCollection());
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
        indexedStoreLogger.trace("successfully removed all records of type: {} from core: {}", this.getIndexType(), getIndexCollection());
    }

    default T index(T obj) {
        if (isChildStore())
            return obj;
        try {
            getSolrClient().addBean(getIndexCollection(), this.toIndexObject(obj));
            getSolrClient().commit(getIndexCollection());
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    default Collection<? extends T> index(Collection<? extends T> objects) {
        if (objects.isEmpty() || isChildStore()) {
            return objects;
        }
        //no batch processing
        if (isParentStore()) {
            for (T object : objects) {
                index(object);
            }
            return objects;
        }
        List<U> indexObjects = objects.stream()
                .map(this::toIndexObject)
                .collect(Collectors.toList());
        try {
            getSolrClient().addBeans(getIndexCollection(), indexObjects);
            getSolrClient().commit(getIndexCollection());
        } catch (IOException | SolrServerException e) {
            throw new RuntimeException(e);
        }

        return objects;
    }

    /**
     * Returns populated @Field annotation object for attribute
     * going deeper to nested objects if needed and also to generics (Set, List, ...)
     * <p>
     * fixme: add support for arrays
     *
     * @param fieldName name of the field
     * @return populated annotation object
     */
    default Field getFieldAnnotation(Class clazz, String fieldName) {
        try {
            if (fieldName.contains(".")) {
                String[] data = fieldName.split("\\.", 2);
                Class<?> nestedType = clazz.getDeclaredField(data[0]).getType();

                Type genericType = clazz.getDeclaredField(data[0]).getGenericType();
                if (genericType instanceof ParameterizedType) {
                    Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
                    if (typeArguments.length == 1 && typeArguments[0] instanceof Class) {
                        nestedType = (Class<?>) typeArguments[0];
                    }
                }

                return getFieldAnnotation(nestedType, data[1]);
            } else {
                return clazz.getDeclaredField(fieldName).getAnnotation(Field.class);
            }
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return getFieldAnnotation(clazz.getSuperclass(), fieldName);
            } else {
                return null;
            }
        }
    }

    /**
     * This method must call {@link #analyzeIndexedClass()} after bean initialization to compute analyzed fields.
     */
    @PostConstruct
    void init();

    /**
     * Analyze indexed object class provided by {@code #getUType} method.
     * map.
     */
    default void analyzeIndexedClass() {
        Map<String, IndexField> indexedFields = new HashMap<>();
        for (java.lang.reflect.Field field : FieldUtils.getFieldsWithAnnotation(getUType(), Indexed.class)) {
            IndexField solrField = new IndexField(field);
            indexedFields.put(solrField.getFieldName(), solrField);
        }
        IndexQueryUtils.INDEXED_FIELDS_MAP.put(getIndexType(), indexedFields);
    }

    @SneakyThrows
    default U getIndexObjectInstance() {
        return getUType().getDeclaredConstructor().newInstance();
    }

    /**
     * nested index support
     * <p>
     * all child docs are removed during parent removal and also during parent indexation,
     * therefore <b>child objects</b> MUST always be indexed
     * during parent object indexation in custom {@link #index(DomainObject)} method
     */
    default boolean isParentStore() {
        return false;
    }

    /**
     * nested index support
     * <br>
     * <b>child objects are not indexed during update operations - reindex MUST be done manually on parent object</b>
     */
    default boolean isChildStore() {
        return false;
    }

    /**
     * Method to override by classes that wants to access AutoComplete logic
     * and do not implement AutoCompleteAware interface.
     *
     * @implNote {@link #toIndexObject} must be overridden to set value of {@link IndexedDatedObject#autoCompleteLabel}
     */
    default boolean isAutoCompleteSearchAllowed() {
        return AutoCompleteAware.class.isAssignableFrom(getType());
    }
}