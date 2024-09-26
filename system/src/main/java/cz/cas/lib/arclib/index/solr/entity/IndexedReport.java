package cz.cas.lib.arclib.index.solr.entity;

import cz.cas.lib.core.index.SolrDocument;
import cz.cas.lib.core.index.solr.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SolrDocument(collection = "arclibDomainC")
public class IndexedReport extends IndexedDatedObject {
    // intentionally empty, all fields needed for indexing, at the time, are declared by superclasses
}
