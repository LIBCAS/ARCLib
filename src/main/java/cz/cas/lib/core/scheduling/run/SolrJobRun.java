package cz.cas.lib.core.scheduling.run;

import cz.cas.lib.core.index.solr.SolrDatedObject;
import cz.cas.lib.core.index.solr.SolrReference;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

/**
 * Indexed representation of {@link JobRun}.
 */
@Getter
@Setter
@SolrDocument(solrCoreName = "jobRun")
public class SolrJobRun extends SolrDatedObject {
    @Field(child = true)
    @Indexed(readonly = true)
    protected SolrReference job;

    @Field
    @Indexed
    protected String result;

    @Field
    @Indexed
    protected Boolean success;

}
