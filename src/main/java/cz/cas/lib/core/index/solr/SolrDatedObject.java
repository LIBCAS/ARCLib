package cz.cas.lib.core.index.solr;

import cz.cas.lib.core.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.solr.core.mapping.Indexed;

import java.util.Date;

/**
 * Building block for Solr entities, which want to track creation and update.
 * <p>
 * <p>
 * Provides attributes {@link SolrDatedObject#created} and {@link SolrDatedObject#updated}.
 * </p>
 * <p>
 * <p>
 * Unlike {@link DatedObject} there is no deleted attribute. Deleted entites are always removed from Solr.
 * Also unlike {@link DatedObject} the attributes are of {@link Date} type, because Solr does not
 * understand Java 8 Time classes.
 * </p>
 */

@Getter
@Setter
public abstract class SolrDatedObject extends SolrDomainObject {

    @Field
    @Indexed(type = FieldType.DATE)
    protected Date created;

    @Field
    @Indexed(type = FieldType.DATE)
    protected Date updated;
}
