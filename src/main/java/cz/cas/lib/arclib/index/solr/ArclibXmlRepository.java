package cz.cas.lib.arclib.index.solr;

import org.springframework.data.solr.repository.SolrCrudRepository;

public interface ArclibXmlRepository extends SolrCrudRepository<ArclibXmlDocument, String> {
}