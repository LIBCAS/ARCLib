package cz.cas.lib.arclib.init;

import cz.cas.lib.core.index.Indexed;
import cz.cas.lib.core.index.SolrDocument;
import cz.cas.lib.core.index.solr.IndexField;
import cz.cas.lib.core.util.SetUtils;
import cz.cas.lib.core.util.Utils;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(0)
public class ManagedSchemasInitializer implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private SolrClient solrClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("managed solr schema initializer started");

        List<Class<?>> solrDocumentClasses;
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages("cz.cas.lib.arclib.index.solr.entity") // Package to scan
                .enableClassInfo() // Enable class scanning
                .enableAnnotationInfo() // Enable annotation scanning
                .scan()) {
            solrDocumentClasses = scanResult.getClassesWithAnnotation(SolrDocument.class.getName())
                    .stream().map(ClassInfo::loadClass).collect(Collectors.toList());
        }

        //gathers metadata of index fields from source code, also asserts there is no conflict in field types across classes
        //key of first map = name of collection
        //key of second map = name of field
        //value of second map = <fieldType, copyTo set (set of fields to which value is copied)>
        Map<String, Map<String, Pair<String, Set<String>>>> collectionsToFieldsMap = new HashMap<>();
        solrDocumentClasses.forEach(clazz -> {
            String collection = clazz.getAnnotation(SolrDocument.class).collection();
            Map<String, Pair<String, Set<String>>> fieldsOfCollection = collectionsToFieldsMap.computeIfAbsent(collection, k -> new HashMap<>());
            for (Field field : FieldUtils.getFieldsWithAnnotation(clazz, Indexed.class)) {
                IndexField indexField = new IndexField(field);
                fieldsOfCollection.merge(indexField.getFieldName(),
                        Pair.of(indexField.getFieldType(), indexField.getManagedSchemaCopyTo()),
                        (fst, snd) -> {
                            Utils.eq(fst.getLeft(), snd.getLeft(), () -> new IllegalStateException("Source code contains multiple @Indexed fields of name " + indexField.getFieldName() + " with different types: " + fst.getLeft() + ", " + snd.getLeft()));
                            return Pair.of(fst.getLeft(), SetUtils.union(fst.getRight(), snd.getRight()));
                        }
                );
            }
        });

        for (String collection : collectionsToFieldsMap.keySet()) {
            log.debug("resolving changes in collection: {}", collection);

            Map<String, Pair<String, Set<String>>> codeFieldMap = collectionsToFieldsMap.get(collection);

            SchemaResponse.FieldsResponse solrFieldsResponse = sendSolrReq(new SchemaRequest.Fields(), collection);
            Map<String, String> solrFieldsTypes = solrFieldsResponse.getFields().stream().collect(Collectors.toMap(f -> (String) f.get("name"), f -> (String) f.get("type")));

            SchemaResponse.CopyFieldsResponse solrCopyFieldsResponse = sendSolrReq(new SchemaRequest.CopyFields(), collection);
            Map<String, Set<String>> solrCpyFields = solrCopyFieldsResponse.getCopyFields().stream().collect(Collectors.toMap(f -> (String) f.get("source"), f -> Set.of((String) f.get("dest")), SetUtils::union));

            //adds new fields fields in solr, also validates that type of existing fields does not differ from type in source code
            for (String codeFieldName : codeFieldMap.keySet()) {
                Pair<String, Set<String>> codeTypeAndCpyMeta = codeFieldMap.get(codeFieldName);
                String codeFieldType = codeTypeAndCpyMeta.getLeft();
                if (solrFieldsTypes.containsKey(codeFieldName)) {
                    String solrFieldType = solrFieldsTypes.get(codeFieldName);
                    if (!solrFieldType.equals(codeFieldType)) {
                        throw new IllegalStateException("Solr collection " + collection + " contains field " + codeFieldName + " of type " + solrFieldType + " but source code works with type " + codeFieldType);
                    }
                } else {
                    sendSolrReq(new SchemaRequest.AddField(Map.of("name", codeFieldName, "type", codeFieldType)), collection);
                }
            }

            //add new and updates existing copy fields in solr
            for (String codeFieldName : codeFieldMap.keySet()) {
                Pair<String, Set<String>> codeTypeAndCpyMeta = codeFieldMap.get(codeFieldName);
                if (solrCpyFields.containsKey(codeFieldName)) {
                    Set<String> destFieldsNoLongerInCode = SetUtils.difference(solrCpyFields.get(codeFieldName), codeTypeAndCpyMeta.getRight());
                    if (!destFieldsNoLongerInCode.isEmpty()) {
                        sendSolrReq(new SchemaRequest.DeleteCopyField(codeFieldName, new ArrayList<>(destFieldsNoLongerInCode)), collection);
                    }
                    Set<String> destFieldsNewInCode = SetUtils.difference(codeTypeAndCpyMeta.getRight(), solrCpyFields.get(codeFieldName));
                    if (!destFieldsNewInCode.isEmpty()) {
                        sendSolrReq(new SchemaRequest.AddCopyField(codeFieldName, new ArrayList<>(destFieldsNewInCode)), collection);
                    }
                } else {
                    if (!codeTypeAndCpyMeta.getRight().isEmpty()) {
                        sendSolrReq(new SchemaRequest.AddCopyField(codeFieldName, new ArrayList<>(codeTypeAndCpyMeta.getRight())), collection);
                    }
                }
            }

            //remove old copy fields from solr
            solrCpyFields.entrySet().stream().filter(e -> !codeFieldMap.containsKey(e.getKey())).forEach(
                    e -> sendSolrReq(new SchemaRequest.DeleteCopyField(e.getKey(), new ArrayList<>(e.getValue())), collection)
            );

            //removes old fields from solr
            solrFieldsTypes.entrySet().stream().filter(e -> !codeFieldMap.containsKey(e.getKey()) && !e.getKey().startsWith("_")).forEach(
                    e -> sendSolrReq(new SchemaRequest.DeleteField(e.getKey()), collection)
            );
            log.debug("resolved changes in collection: {}", collection);
        }
        log.info("managed solr schema initializer finished");
    }

    private <T extends SolrResponse> T sendSolrReq(SolrRequest<T> req, String collection) {
        try {
            return req.process(solrClient, collection);
        } catch (SolrServerException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
