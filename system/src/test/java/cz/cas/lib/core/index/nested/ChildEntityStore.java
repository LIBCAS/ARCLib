package cz.cas.lib.core.index.nested;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.core.index.solr.IndexedDomainStore;
import cz.cas.lib.core.index.solr.IndexedStore;
import lombok.Getter;
import org.springframework.stereotype.Repository;

/**
 * Nested indexation when using {@link IndexedStore} is quite limited, customized way of handling of nested indicies, e.g. through
 * {@link org.apache.solr.common.SolrInputDocument} is advised.
 * <br>
 * If {@link IndexedStore} has to be used it has to be used under certain conditions:
 * <ul>
 * <li>child store overrides {@link IndexedStore#isChildStore()}</li>
 * <li>parent store overrides {@link IndexedStore#isParentStore()}</li>
 * <li>child is always indexed during indexation of parent in custom methods: parent store must override {@link IndexedStore#index(DomainObject)}</li>
 * <li>child can not exist without parent</li>
 * </ul>
 */
@Repository
public class ChildEntityStore extends IndexedDomainStore<ChildEntity, QChildEntity, IndexedChildEntity> {

    public ChildEntityStore() {
        super(ChildEntity.class, QChildEntity.class, IndexedChildEntity.class);
    }

    @Override
    public boolean isChildStore() {
        return true;
    }

    public static final String INDEX_TYPE = "testChildEntity";

    @Getter
    private final String indexType = INDEX_TYPE;
}
