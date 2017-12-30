package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.index.IndexStore;
import cz.inqool.uas.index.dto.FilterOperation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.List;

@RestController
@RequestMapping("/api")
public class IndexApi {

    private IndexStore indexStore;

    /**
     * Query Solr for documents with specified filters.
     * <p>
     *     Filters are specified in query parameters in ModelAttribute way eg.
     *     ?filter[0].field=event_type&filter[0].operation=EQ&filter[0].value=validation
     * </p>
     * <p>
     *     See {@link FilterOperation} for all supported operations, {@link cz.cas.lib.arclib.index.solr.SolrQueryBuilder} for operations meaning and tests for the usage.
     * </p>
     * @param filter
     * @return  list of IDs of documents
     */
    @RequestMapping(value="/list", method = RequestMethod.GET)
    public List<String> listDocumentIds(FilterWrapper filter){
        return indexStore.findAll(filter.getFilter());
    }

    @Inject
    public void setIndexStore(IndexStore indexStore){
        this.indexStore=indexStore;
    }
}
