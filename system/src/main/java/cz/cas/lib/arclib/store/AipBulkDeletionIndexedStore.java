//package cz.cas.lib.arclib.store;
//
//import cz.cas.lib.arclib.domain.packages.AipBulkDeletion;
//import cz.cas.lib.arclib.domain.packages.QAipBulkDeletion;
//import cz.cas.lib.arclib.index.solr.entity.IndexedAipBulkDeletion;
//import cz.cas.lib.core.index.solr.IndexedDatedStore;
//import lombok.Getter;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public class AipBulkDeletionIndexedStore extends IndexedDatedStore<AipBulkDeletion, QAipBulkDeletion, IndexedAipBulkDeletion> {
//
//    public AipBulkDeletionIndexedStore() {
//        super(AipBulkDeletion.class, QAipBulkDeletion.class, IndexedAipBulkDeletion.class);
//    }
//
//    @Getter
//    private final String indexType = "aipBulkDeletion";
//
//    @Override
//    public IndexedAipBulkDeletion toIndexObject(AipBulkDeletion obj) {
//        IndexedAipBulkDeletion indexObject = super.toIndexObject(obj);
//        indexObject.setState(obj.getState());
//        indexObject.setDeleteIfNewerVersionsDeleted(obj.isDeleteIfNewerVersionsDeleted());
//        indexObject.setUserName(obj.getCreator().getUsername());
//        if (obj.getProducer() != null) {
//            indexObject.setProducerName(obj.getProducer().getName());
//        }
//        return indexObject;
//    }
//}
