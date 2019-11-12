package cz.cas.lib.core.index.nested;

import cz.cas.lib.core.index.solr.IndexedDomainStore;
import lombok.Getter;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ParentEntityStore extends IndexedDomainStore<ParentEntity, QParentEntity, IndexedParentEntity> {

    public ParentEntityStore() {
        super(ParentEntity.class, QParentEntity.class, IndexedParentEntity.class);
    }

    @Getter
    private final String indexType = "testParentEntity";

    @Override
    public boolean isParentStore() {
        return true;
    }

    @Override
    public ParentEntity index(ParentEntity obj) {
        IndexedParentEntity indexedParent = super.toIndexObject(obj);
        indexedParent.setAttribute(obj.getAttr());
        List<IndexedChildEntity> children = new ArrayList<>();
        for (ChildEntity child : obj.getChildren()) {
            IndexedChildEntity iChild = new IndexedChildEntity();
            iChild.setAttribute(child.getAttr());
            iChild.setType(ChildEntityStore.INDEX_TYPE);
            iChild.setId(obj.getId());
            children.add(iChild);
        }
        SolrInputDocument parentInputDocument = getTemplate().convertBeanToSolrInputDocument(indexedParent);
        children.stream().map(d -> getTemplate().convertBeanToSolrInputDocument(d)).forEach(parentInputDocument::addChildDocument);
        getTemplate().saveDocument(getIndexCollection(), parentInputDocument);
        getTemplate().commit(getIndexCollection());
        return obj;
    }
}
