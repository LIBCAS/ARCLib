package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.preservationPlanning.Format;
import cz.cas.lib.arclib.domain.preservationPlanning.QFormat;
import cz.cas.lib.arclib.index.solr.entity.SolrFormat;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.index.solr.SolrDatedStore;
import org.springframework.stereotype.Repository;

import java.util.List;

import static cz.cas.lib.core.util.Utils.asList;

@Repository
public class FormatStore
        extends SolrDatedStore<Format, QFormat, SolrFormat> {
    public FormatStore() {
        super(Format.class, QFormat.class, SolrFormat.class);
    }

    public Format findByFormatId(Integer formatId) {
        Params params = new Params();
        params.setPageSize(null);

        Filter filter = new Filter();
        filter.setField("formatId");
        filter.setOperation(FilterOperation.EQ);
        filter.setValue(String.valueOf(formatId));

        params.setFilter(asList(filter));

        Result<Format> all = findAll(params);
        List<Format> items = all.getItems();

        Format format = null;

        if (items.size() > 0) {
            format = items.get(0);
        }
        return format;
    }

    @Override
    public SolrFormat toIndexObject(Format obj) {
        SolrFormat indexObject = super.toIndexObject(obj);

        Integer formatId = obj.getFormatId();
        if (formatId != null) {
            indexObject.setFormatId(formatId);
        }

        String puid = obj.getPuid();
        if (puid != null) {
            indexObject.setPuid(puid);
        }

        String formatName = obj.getFormatName();
        if (formatName != null) {
            indexObject.setFormatName(formatName);
        }
        return indexObject;
    }
}
