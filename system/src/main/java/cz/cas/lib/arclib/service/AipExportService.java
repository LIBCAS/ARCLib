package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.export.ExportConfig;
import cz.cas.lib.arclib.domain.export.ExportScope;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.packages.Sip;
import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.arclib.index.IndexedArclibXmlStore;
import cz.cas.lib.arclib.index.SimpleIndexFilter;
import cz.cas.lib.arclib.index.SimpleIndexFilterOperation;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedAipState;
import cz.cas.lib.arclib.index.solr.arclibxml.IndexedArclibXmlDocument;
import cz.cas.lib.arclib.index.solr.arclibxml.SolrArclibXmlStore;
import cz.cas.lib.arclib.security.authorization.permission.Permissions;
import cz.cas.lib.arclib.security.user.UserDetails;
import cz.cas.lib.arclib.security.user.UserDetailsImpl;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageException;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageResponseExtractor;
import cz.cas.lib.arclib.service.archivalStorage.ArchivalStorageService;
import cz.cas.lib.arclib.service.export.DcExportMetadataKey;
import cz.cas.lib.arclib.service.export.DcExportService;
import cz.cas.lib.arclib.store.SipStore;
import cz.cas.lib.arclib.utils.ArclibUtils;
import cz.cas.lib.core.index.dto.Filter;
import cz.cas.lib.core.index.dto.FilterOperation;
import cz.cas.lib.core.index.dto.Params;
import cz.cas.lib.core.util.SetUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static cz.cas.lib.arclib.utils.ArclibUtils.hasRole;

@Component
@Slf4j
public class AipExportService {

    private IndexedArclibXmlStore indexedArclibXmlStore;
    private ArchivalStorageService archivalStorageService;
    private ArchivalStorageResponseExtractor archivalStorageResponseExtractor;
    private UserDetails userDetails;
    private ExportInfoFileService exportInfoFileService;
    private SipStore sipStore;
    private DcExportService dcExportService;

    /**
     * @param ids          ARClib XML IDs ({@link IndexedArclibXmlDocument#ID} i.e. {@link IngestWorkflow#externalId})
     * @param exportConfig config
     * @param outputStream output stream to which the result is written
     * @throws IOException
     */
    public void download(Collection<String> ids, ExportConfig exportConfig, OutputStream outputStream) throws IOException {
        Set<ExportScope> exportScopes = new HashSet<>(exportConfig.getScope());
        if (exportScopes.isEmpty())
            return;

        if (exportScopes.contains(ExportScope.IDS)) {
            if (outputStream instanceof ZipOutputStream) {
                ((ZipOutputStream) outputStream).putNextEntry(new ZipEntry(ExportScope.IDS.getFsName()));
            }
            IOUtils.write(String.join(",", ids), outputStream, StandardCharsets.UTF_8);
            if (exportScopes.size() == 1) {
                return;
            }
        }

        List<IndexedArclibXmlDocument> docsFromIndex = findDocsForExport(exportScopes, ids, userDetails);
        Set<String> metadataNotFoundIds = SetUtils.difference(new HashSet<>(ids), docsFromIndex.stream().map(IndexedArclibXmlDocument::getId).collect(Collectors.toSet()));

        if (exportScopes.contains(ExportScope.AIP_XML)) {
            ZipOutputStream zipOut = (ZipOutputStream) outputStream;

            if (!metadataNotFoundIds.isEmpty()) {
                zipOut.putNextEntry(new ZipEntry("metadataNotFoundIds.csv"));
                IOUtils.write(String.join(",", metadataNotFoundIds), outputStream, StandardCharsets.UTF_8);
            }

            HashMap<String, List<Integer>> aipIdsAndVersions = new HashMap<>();
            for (IndexedArclibXmlDocument item : docsFromIndex) {
                String sipId = item.getSipId();
                Integer xmlVersionNumber = item.getXmlVersionNumber();

                List<Integer> versions = aipIdsAndVersions.get(sipId);
                if (versions == null) versions = new ArrayList<>();

                versions.add(xmlVersionNumber);
                aipIdsAndVersions.put(sipId, versions);
            }
            for (Map.Entry<String, List<Integer>> aipAndVersionId : aipIdsAndVersions.entrySet()) {
                String aipId = aipAndVersionId.getKey();
                List<Integer> versions = aipAndVersionId.getValue();
                for (Integer version : versions) {
                    try {
                        InputStream response = archivalStorageService.exportSingleXml(aipId, version);
                        zipOut.putNextEntry(new ZipEntry(ExportScope.AIP_XML.getFsName() + "/" + ArclibUtils.getXmlExportName(aipId, version)));
                        IOUtils.copy(response, outputStream);
                        log.info("XML of AIP with ID " + aipId + " has been exported from archival storage.");
                    } catch (ArchivalStorageException e) {
                        log.error("error during export of XML of AIP with ID" + aipId, e);
                    }
                }
            }
        }

        if (exportScopes.contains(ExportScope.METADATA)) {
            if (outputStream instanceof ZipOutputStream) {
                ((ZipOutputStream) outputStream).putNextEntry(new ZipEntry(ExportScope.METADATA.getFsName()));
            }
            CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(outputStream), CSVFormat.DEFAULT);
            List<IndexedArclibXmlDocument> docs = new ArrayList<>(docsFromIndex);
            if (!metadataNotFoundIds.isEmpty()) {
                docs.addAll(metadataNotFoundIds.stream().map(id -> new IndexedArclibXmlDocument(Map.of(IndexedArclibXmlDocument.ID, id), Map.of())).collect(Collectors.toSet()));
            }
            exportMetadata(csvPrinter, exportConfig, docs);
        }
    }

    public Path initiateExport(Collection<String> ids, ExportConfig exportConfig, boolean async, User user) throws IOException {
        Path exportFolder = Path.of(exportConfig.getExportFolder()).resolve(DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
        synchronized (this) {
            if (exportFolder.toFile().exists()) {
                exportFolder = exportFolder.resolveSibling("_" + UUID.randomUUID());
            }
            Files.createDirectories(exportFolder);
        }
        exportFolder.resolve("export.inprogress").toFile().createNewFile();
        Path finalExportFolder = exportFolder;
        if (async) {
            CompletableFuture.runAsync(() -> export(ids, exportConfig, finalExportFolder, user)).exceptionally(e -> {
                log.error("error during export to: " + finalExportFolder, e);
                return null;
            });
        } else {
            export(ids, exportConfig, finalExportFolder, user);
        }
        return finalExportFolder;
    }

    private void export(Collection<String> ids, ExportConfig exportConfig, Path exportFolder, User user) {
        Set<ExportScope> exportScopes = new HashSet<>(exportConfig.getScope());

        try {
            List<IndexedArclibXmlDocument> docsFromIndex = List.of();
            if (exportConfig.getScope().size() > 1 || !exportConfig.getScope().contains(ExportScope.IDS)) {
                docsFromIndex = findDocsForExport(exportScopes, ids, new UserDetailsImpl(user));
                Set<String> metadataNotFoundIds = SetUtils.difference(new HashSet<>(ids), docsFromIndex.stream().map(IndexedArclibXmlDocument::getId).collect(Collectors.toSet()));
                if (!metadataNotFoundIds.isEmpty()) {
                    Files.writeString(exportFolder.resolve("metadataNotFoundIds.csv"), String.join(",", metadataNotFoundIds));
                }
                Set<String> deletedDocsIds = docsFromIndex.stream().filter(d -> d.getAipState() == IndexedAipState.DELETED).map(IndexedArclibXmlDocument::getId).collect(Collectors.toSet());
                if (!deletedDocsIds.isEmpty()) {
                    Files.writeString(exportFolder.resolve("deletedDocsIds.csv"), String.join(",", deletedDocsIds));
                }
            }
            for (ExportScope exportScope : exportScopes) {
                Path scopeFolder = exportFolder.resolve(exportScope.getFsName());
                switch (exportScope) {
                    case IDS:
                        Files.writeString(exportFolder.resolve(exportScope.getFsName()), String.join(",", ids));
                        break;
                    case METADATA:
                        List<String> selectedMetadata = exportConfig.getListOfMetadataToExport();
                        if (selectedMetadata.isEmpty()) {
                            break;
                        }
                        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(exportFolder.resolve(exportScope.getFsName()).toFile()), CSVFormat.DEFAULT)) {
                            exportMetadata(csvPrinter, exportConfig, docsFromIndex);
                        }
                        break;
                    case AIP_XML:
                        Files.createDirectories(scopeFolder);
                        HashMap<String, List<Integer>> aipIdsAndVersions = new HashMap<>();
                        for (IndexedArclibXmlDocument item : docsFromIndex) {
                            String sipId = item.getSipId();
                            Integer xmlVersionNumber = item.getXmlVersionNumber();

                            List<Integer> versions = aipIdsAndVersions.get(sipId);
                            if (versions == null) versions = new ArrayList<>();

                            versions.add(xmlVersionNumber);
                            aipIdsAndVersions.put(sipId, versions);
                        }
                        for (Map.Entry<String, List<Integer>> aipAndVersionId : aipIdsAndVersions.entrySet()) {
                            String aipId = aipAndVersionId.getKey();
                            List<Integer> versions = aipAndVersionId.getValue();
                            for (Integer version : versions) {
                                try {
                                    InputStream response = archivalStorageService.exportSingleXml(aipId, version);
                                    FileUtils.copyInputStreamToFile(response, scopeFolder.resolve(ArclibUtils.getXmlExportName(aipId, version)).toFile());
                                    log.info("XML of AIP with ID " + aipId + " has been exported from archival storage.");
                                } catch (ArchivalStorageException e) {
                                    log.error("error during export of XML of AIP with ID" + aipId, e);
                                }
                            }
                        }
                        break;
                    case DATA_AND_LAST_XML:
                    case DATA_AND_ALL_XMLS:
                        Files.createDirectories(scopeFolder);
                        Map<String, Sip> sipsByTheirIds = sipStore.findAllInList(docsFromIndex.stream().map(IndexedArclibXmlDocument::getSipId).distinct().collect(Collectors.toList()))
                                .stream().collect(Collectors.toMap(DomainObject::getId, s -> s));
                        for (IndexedArclibXmlDocument doc : docsFromIndex) {
                            String aipId = doc.getSipId();
                            if (doc.getAipState() == IndexedAipState.DELETED) {
                                log.debug("skipping export of AIP: {} since it is deleted", aipId);
                                continue;
                            }
                            try {
                                InputStream response = archivalStorageService.exportSingleAip(aipId, exportScope == ExportScope.DATA_AND_ALL_XMLS, exportConfig.getDataReduction());
                                Path aipDataDir;
                                try (ZipInputStream zis = new ZipInputStream(response)) {
                                    aipDataDir = archivalStorageResponseExtractor.extractAipAsFolderWithXmlsBySide(zis, aipId, scopeFolder, sipsByTheirIds.get(doc.getSipId()).getFolderStructure().getCaption());
                                }
                                if (exportConfig.isGenerateInfoFile()) {
                                    Sip sipEntity = sipStore.find(aipId);
                                    exportInfoFileService.write(aipDataDir.resolve(ExportInfoFileService.EXPORT_INFO_FILE_NAME), Map.of(ExportInfoFileService.KEY_AUTHORIAL_PACKAGE_UUID, sipEntity.getAuthorialPackage().getId()));
                                }
                                log.info("AIP " + aipId + " has been exported from archival storage.");
                            } catch (ArchivalStorageException e) {
                                log.error("error during export of AIP: " + aipId, e);
                            }
                        }
                        break;
                }
            }
            exportFolder.resolve("export.done").toFile().createNewFile();
            Files.deleteIfExists(exportFolder.resolve("export.inprogress"));
        } catch (Exception e) {
            try {
                Files.writeString(exportFolder.resolve("export.failed"), ExceptionUtils.getStackTrace(e));
                Files.deleteIfExists(exportFolder.resolve("export.inprogress"));
            } catch (IOException ee) {
                throw new UncheckedIOException(ee);
            }
            throw new RuntimeException(e);
        }
    }

    private void exportMetadata(CSVPrinter csvPrinter, ExportConfig exportConfig, List<IndexedArclibXmlDocument> docsFromIndex) throws IOException {
        List<String> selectedMetadata = exportConfig.getMetadataSelection();
        Map<IndexedArclibXmlDocument, Map<DcExportMetadataKey, List<String>>> dcExport = dcExportService.exportDcItems(docsFromIndex, selectedMetadata);

        ArrayList<String> header = new ArrayList<>();
        header.add("id");
        header.addAll(selectedMetadata);
        csvPrinter.printRecord(header);
        for (IndexedArclibXmlDocument doc : docsFromIndex) {
            List<Object> values = new ArrayList<>();
            values.add(doc.getId());
            Map<DcExportMetadataKey, List<String>> dcExportOfThisDoc = dcExport.get(doc);
            for (String key : selectedMetadata) {
                DcExportMetadataKey mbyDcExportItem = EnumUtils.getEnum(DcExportMetadataKey.class, key);
                if (mbyDcExportItem != null) {
                    if (dcExportOfThisDoc != null) {
                        List<String> dcExportValues = dcExportOfThisDoc.get(mbyDcExportItem);
                        if (dcExportValues != null) {
                            values.add(String.join(";", dcExportValues));
                        } else {
                            values.add(null);
                        }
                    } else {
                        values.add(null);
                    }
                } else {
                    Object value = doc.getFields().get(key);
                    if (value == null) {
                        value = doc.getChildren().get(key);
                    }
                    if (value instanceof Collection && ((Collection) value).size() == 1) {
                        values.add(((Collection) value).iterator().next());
                    } else {
                        values.add(value);
                    }
                }
            }
            csvPrinter.printRecord(values);
        }
        csvPrinter.flush();
    }

    private List<IndexedArclibXmlDocument> findDocsForExport(Set<ExportScope> scopes, Collection<String> ids, UserDetails userDetails) {
        List<SimpleIndexFilter> simpleFilters = new ArrayList<>();
        if (!hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE)) {
            simpleFilters.add(new SimpleIndexFilter(IndexedArclibXmlDocument.PRODUCER_ID, SimpleIndexFilterOperation.EQ, userDetails.getProducerId()));
        }
        if (!hasRole(userDetails, Permissions.LOGICAL_FILE_RENEW)) {
            simpleFilters.add(new SimpleIndexFilter(IndexedArclibXmlDocument.AIP_STATE, SimpleIndexFilterOperation.NEQ, IndexedAipState.REMOVED.toString()));
        }
        if (!(hasRole(userDetails, Permissions.ADMIN_PRIVILEGE) || hasRole(userDetails, Permissions.SUPER_ADMIN_PRIVILEGE))) {
            simpleFilters.add(new SimpleIndexFilter(IndexedArclibXmlDocument.AIP_STATE, SimpleIndexFilterOperation.NEQ, IndexedAipState.DELETED.toString()));
        }
        List<IndexedArclibXmlDocument> docsFromIndex;
        if (scopes.contains(ExportScope.METADATA)) {
            docsFromIndex = indexedArclibXmlStore.findWithChildren(ids, simpleFilters);
        } else {
            Params params = new Params();
            for (SimpleIndexFilter d : simpleFilters) {
                Filter f = new Filter();
                switch (d.getOperation()) {
                    case NEQ:
                        f.setOperation(FilterOperation.NEQ);
                        break;
                    case EQ:
                        f.setOperation(FilterOperation.EQ);
                        break;
                    default:
                        throw new UnsupportedOperationException("unsupported operation: " + d.getOperation());
                }
                f.setField(d.getField());
                f.setValue(d.getValue());
                params.addFilter(f);
            }
            params.addFilter(new Filter(IndexedArclibXmlDocument.ID, FilterOperation.IN, String.join(",", ids), List.of()));
            docsFromIndex = indexedArclibXmlStore.findAllIgnorePagination(params).getItems();
        }
        return docsFromIndex;
    }

    @Inject
    public void setArchivalStorageService(ArchivalStorageService archivalStorageService) {
        this.archivalStorageService = archivalStorageService;
    }

    @Inject
    public void setindexedArclibXmlStore(SolrArclibXmlStore indexedArclibXmlStore) {
        this.indexedArclibXmlStore = indexedArclibXmlStore;
    }

    @Inject
    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Inject
    public void setExportInfoFileService(ExportInfoFileService exportInfoFileService) {
        this.exportInfoFileService = exportInfoFileService;
    }

    @Inject
    public void setArchivalStorageResponseExtractor(ArchivalStorageResponseExtractor archivalStorageResponseExtractor) {
        this.archivalStorageResponseExtractor = archivalStorageResponseExtractor;
    }

    @Inject
    public void setSipStore(SipStore sipStore) {
        this.sipStore = sipStore;
    }

    @Inject
    public void setDcExportService(DcExportService dcExportService) {
        this.dcExportService = dcExportService;
    }
}
