package cz.cas.lib.arclib.domain;

import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlDocument;
import cz.cas.lib.core.domain.NamedObject;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * Vyhľadávací dotaz nad ARCLib XML spolu s výsledkami dotazu.
 */
@Getter
@Setter
@Entity
@Table(name = "arclib_aip_query")
@NoArgsConstructor
public class AipQuery extends NamedObject {
    public AipQuery(String id) {
        setId(id);
    }

    public AipQuery(User user, Result<SolrArclibXmlDocument> result, Params query, String queryName) {
        this.name=queryName;
        this.user = user;
        this.result = result;
        this.query = query;
    }

    /**
     * Užívateľ
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    /**
     * Zoznam nájdených výsledkov
     */
    @Column(length = 100 * 1024 * 1024)
    private Result<SolrArclibXmlDocument> result;

    /**
     * Vyhľadávací dotaz
     */
    @Column(length = 100 * 1024 * 1024)
    private Params query;
}
