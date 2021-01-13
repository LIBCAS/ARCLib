package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.core.index.solr.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.solr.core.mapping.SolrDocument;

@Getter
@Setter
@SolrDocument(collection = "arclibDomainC")
public class IndexedReport extends IndexedDatedObject {
    // intentionally empty, all fields needed for indexing, at the time, are declared by superclasses
}
