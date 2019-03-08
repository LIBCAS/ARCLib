package cz.cas.lib.arclib.store;

import cz.cas.lib.arclib.domain.preservationPlanning.FormatDefinition;
import cz.cas.lib.arclib.domain.preservationPlanning.QFormatDefinition;
import cz.cas.lib.arclib.index.solr.entity.SolrFormatDefinition;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.index.dto.Result;
import cz.cas.lib.core.index.solr.SolrDatedStore;
import org.springframework.stereotype.Repository;

import java.util.List;

import static cz.cas.lib.core.util.Utils.asList;

@Repository
public class FormatDefinitionStore
        extends SolrDatedStore<FormatDefinition, QFormatDefinition, SolrFormatDefinition> {
    public FormatDefinitionStore() {
        super(FormatDefinition.class, QFormatDefinition.class, SolrFormatDefinition.class);
    }

    public List<FormatDefinition> findByFormatId(Integer formatId, boolean localDefinition) {
        Params params = new Params();
        params.setPageSize(1000);

        Filter formatIdFilter = new Filter();
        formatIdFilter.setField("formatId");
        formatIdFilter.setOperation(FilterOperation.EQ);
        formatIdFilter.setValue(String.valueOf(formatId));

        Filter localDefinitionFilter = new Filter();
        localDefinitionFilter.setField("localDefinition");
        localDefinitionFilter.setOperation(FilterOperation.EQ);
        localDefinitionFilter.setValue(String.valueOf(localDefinition));

        params.setFilter(asList(formatIdFilter, localDefinitionFilter));

        Result<FormatDefinition> all = findAll(params);

        return all.getItems();
    }

    public FormatDefinition findPreferredByFormatId(Integer formatId) {
        Params params = new Params();
        params.setPageSize(1000);

        Filter formatIdFilter = new Filter();
        formatIdFilter.setField("formatId");
        formatIdFilter.setOperation(FilterOperation.EQ);
        formatIdFilter.setValue(String.valueOf(formatId));

        Filter preferredFilter = new Filter();
        preferredFilter.setField("preferred");
        preferredFilter.setOperation(FilterOperation.EQ);
        preferredFilter.setValue(String.valueOf(true));

        params.setFilter(asList(formatIdFilter, preferredFilter));

        Result<FormatDefinition> all = findAll(params);

        List<FormatDefinition> items = all.getItems();
        FormatDefinition format = null;

        if (items.size() > 0) {
            format = items.get(0);
        }
        return format;
    }

    public FormatDefinition findPreferredByFormatPuid(String puid) {
        Params params = new Params();
        params.setPageSize(1000);

        Filter puidFilter = new Filter();
        puidFilter.setField("puid");
        puidFilter.setOperation(FilterOperation.EQ);
        puidFilter.setValue(String.valueOf(puid));

        Filter preferredFilter = new Filter();
        preferredFilter.setField("preferred");
        preferredFilter.setOperation(FilterOperation.EQ);
        preferredFilter.setValue(String.valueOf(true));

        params.setFilter(asList(puidFilter, preferredFilter));

        Result<FormatDefinition> all = findAll(params);

        List<FormatDefinition> items = all.getItems();
        FormatDefinition format = null;

        if (items.size() > 0) {
            format = items.get(0);
        }
        return format;
    }

    @Override
    public SolrFormatDefinition toIndexObject(FormatDefinition obj) {
        SolrFormatDefinition indexObject = super.toIndexObject(obj);


        String formatVersion = obj.getFormatVersion();
        if (formatVersion != null) {
            indexObject.setFormatVersion(formatVersion);
        }

        Integer internalVersionNumber = obj.getInternalVersionNumber();
        if (internalVersionNumber != null) {
            indexObject.setInternalVersionNumber(internalVersionNumber);
        }

        Boolean internalInformationFilled = obj.isInternalInformationFilled();
        if (internalInformationFilled != null) {
            indexObject.setInternalInformationFilled(internalInformationFilled);
        }

        Boolean localDefinition = obj.isLocalDefinition();
        if (localDefinition != null) {
            indexObject.setLocalDefinition(localDefinition);
        }

        Boolean preferred = obj.isPreferred();
        if (preferred != null) {
            indexObject.setPreferred(preferred);
        }

        Integer formatId = obj.getFormat().getFormatId();
        if (formatId != null) {
            indexObject.setFormatId(formatId);
        }

        String puid = obj.getFormat().getPuid();
        if (puid != null) {
            indexObject.setPuid(puid);
        }
        return indexObject;
    }
}
