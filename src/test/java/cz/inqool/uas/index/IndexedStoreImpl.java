package cz.inqool.uas.index;

import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static cz.inqool.uas.util.Utils.toLabeledReference;
import static java.util.Collections.emptySet;

public class IndexedStoreImpl extends IndexedDatedStore<TestEntity, QTestEntity, IndexedTestEntity> {
    public IndexedStoreImpl() {
        super(TestEntity.class, QTestEntity.class, IndexedTestEntity.class);
    }

    @Override
    public IndexedTestEntity toIndexObject(TestEntity obj) {
        IndexedTestEntity indexObject = super.toIndexObject(obj);

        indexObject.setStringAttribute(obj.getStringAttribute());
        indexObject.setFoldingAttribute(obj.getStringAttribute());

        indexObject.setIntAttribute(obj.getIntAttribute());
        indexObject.setDoubleAttribute(obj.getDoubleAttribute());
        indexObject.setLocalDateAttribute(obj.getLocalDateAttribute());
        indexObject.setInstantAttribute(obj.getInstantAttribute());
        indexObject.setDependent(toLabeledReference(obj.getDependent(), DependentEntity::getName));

        if (obj.getDependents() != null) {
            Set<LabeledReference> references = obj.getDependents().stream()
                                                  .map(d -> toLabeledReference(d, DependentEntity::getName))
                                                  .collect(Collectors.toSet());
            indexObject.setDependents(references);
        }

        return indexObject;
    }

    public List<String> findEmptyInFilter() {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withTypes(getIndexType())
                .withFilter(inQuery("stringAttribute", emptySet()))
                .withFields("id")
                .build();

        return template.query(query, response -> {
            return StreamSupport.stream(response.getHits().spliterator(), true)
                                            .map(hit -> hit.field("id").<String>getValue())
                                            .collect(Collectors.toList());
        });
    }

    public List<String> findEmptyNotInFilter() {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withTypes(getIndexType())
                .withFilter(notInQuery("stringAttribute", emptySet()))
                .withFields("id")
                .build();

        return template.query(query, response -> {
            return StreamSupport.stream(response.getHits().spliterator(), true)
                                .map(hit -> hit.field("id").<String>getValue())
                                .collect(Collectors.toList());
        });
    }
}
