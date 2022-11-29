package cz.cas.lib.arclib.service.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Component
public class DcExportService {

    private final Map<DcExportPackageType, List<@Valid DcExportMetadataItemConfig>> dcExportCfg;

    public DcExportService(ObjectMapper objectMapper, @Value("${arclib.dcExportCfg}") Resource dcExportCfg) throws IOException {
        DcExportMetadataItemConfig[] cfgs = objectMapper.readValue(dcExportCfg.getInputStream(), DcExportMetadataItemConfig[].class);
        this.dcExportCfg = Arrays.stream(cfgs).peek(DcExportMetadataItemConfig::compileRegexes).collect(Collectors.groupingBy(DcExportMetadataItemConfig::getPackageType));
    }

    public Map<IndexedArclibXmlDocument, Map<DcExportMetadataKey, List<String>>> exportDcItems(Collection<IndexedArclibXmlDocument> docs,
                                                                                               List<String> metadataSelection) {
        Set<DcExportMetadataKey> dcExportItems = metadataSelection.stream().map(m -> EnumUtils.getEnum(DcExportMetadataKey.class, m)).filter(Objects::nonNull).collect(Collectors.toSet());
        if (dcExportItems.isEmpty()) {
            return Map.of();
        }
        Map<IndexedArclibXmlDocument, Map<DcExportMetadataKey, List<String>>> resultMap = new HashMap<>();
        for (IndexedArclibXmlDocument doc : docs) {
            System.out.println(doc.getFields());
            Set<DcExportPackageType> typesMatchedWithTheDoc = new HashSet<>();
            //generalization of the following block would be useful
            switch (doc.getType()) {
                case "Periodical":
                    typesMatchedWithTheDoc.add(DcExportPackageType.NDK_PERIODICAL);
                    break;
                case "Monograph":
                    List<Map<String, Collection<Object>>> dcChildren = getDcChildren(doc);
                    boolean dcContainsTitleLevel = dcChildren.stream().flatMap(c -> c.get("dublin_core_id").stream()).anyMatch(dcId -> ((String) dcId).contains("DCMD_TITLE"));
                    if (dcContainsTitleLevel) {
                        typesMatchedWithTheDoc.add(DcExportPackageType.NDK_MULTI_VOLUME);
                    } else {
                        typesMatchedWithTheDoc.add(DcExportPackageType.NDK_SINGLE_VOLUME);
                    }
                    break;
            }
            //end of not general block
            Set<@Valid DcExportMetadataItemConfig> configsToExport = dcExportCfg.entrySet().stream().filter(e -> typesMatchedWithTheDoc.contains(e.getKey())).flatMap(e -> e.getValue().stream()).filter(c -> dcExportItems.contains(c.getMetadataKey())).collect(Collectors.toSet());
            Map<DcExportMetadataKey, List<String>> extractedMetadata = new HashMap<>();
            for (DcExportMetadataItemConfig cfg : configsToExport) {
                List<String> meta = extractMetadata(doc, cfg);
                extractedMetadata.computeIfAbsent(cfg.getMetadataKey(), k -> new ArrayList<>()).addAll(meta);
            }
            resultMap.put(doc, extractedMetadata);
        }
        return resultMap;
    }

    private List<String> extractMetadata(IndexedArclibXmlDocument doc, DcExportMetadataItemConfig cfg) {
        List<String> values = new ArrayList<>();
        List<Map<String, Collection<Object>>> dublinCoreBlocks = getDcChildren(doc);
        for (String matchingDmdsecId : cfg.getDmdSecIdLike()) {
            dublinCoreBlocks.stream()
                    .filter(dc -> matchingDmdsecId.contains((String) dc.get("dublin_core_id").iterator().next())
                            && dc.containsKey(cfg.getIndexField()))
                    .flatMap(dc -> dc.get(cfg.getIndexField()).stream()).forEach(dcItem -> {
                                String dcItemString = (String) dcItem;
                                if (cfg.getInvalidValueRegex() != null && cfg.getCompiledInvalidValueRegex().matcher(dcItemString).matches()) {
                                    return;
                                }
                                if (cfg.getValueRegex() != null) {
                                    Matcher matcher = cfg.getCompiledValueRegex().matcher(dcItemString);
                                    if (matcher.matches()) {
                                        values.add(matcher.group(1));
                                    }
                                    return;
                                }
                                values.add(dcItemString);
                            }
                    );
        }
        return values;
    }

    private List<Map<String, Collection<Object>>> getDcChildren(IndexedArclibXmlDocument doc) {
        List<Map<String, Collection<Object>>> dublin_core = doc.getChildren().get("dublin_core");
        if (dublin_core == null) {
            dublin_core = List.of();
        }
        return dublin_core;
    }
}
