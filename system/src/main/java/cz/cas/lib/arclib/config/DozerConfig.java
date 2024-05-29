package cz.cas.lib.arclib.config;

import cz.cas.lib.arclib.domain.AipQuery;
import cz.cas.lib.arclib.domain.Batch;
import cz.cas.lib.arclib.domain.IngestRoutine;
import cz.cas.lib.arclib.domain.export.ExportRoutine;
import cz.cas.lib.arclib.domain.ingestWorkflow.IngestWorkflow;
import cz.cas.lib.arclib.domain.ingestWorkflow.WorkflowDefinition;
import cz.cas.lib.arclib.domain.packages.AipBulkDeletion;
import cz.cas.lib.arclib.domain.packages.AipDeletionRequest;
import cz.cas.lib.arclib.domain.profiles.ProducerProfile;
import cz.cas.lib.arclib.domain.profiles.SipProfile;
import cz.cas.lib.arclib.domain.profiles.ValidationProfile;
import cz.cas.lib.arclib.dto.*;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.dozer.loader.api.BeanMappingBuilder;
import org.dozer.loader.api.FieldsMappingOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DozerConfig {
    public static class DozerCustomConfig extends BeanMappingBuilder {
        @Override
        protected void configure() {
            mapping(BatchDto.class, Batch.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference())
                    .fields("producer", "producerProfile.producer", FieldsMappingOptions.copyByReference());
            mapping(BatchDetailDto.class, Batch.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference());
            mapping(BatchDetailIngestWorkflowDto.class, IngestWorkflow.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference())
                    .fields("sipAuthorialId", "sip.authorialPackage.authorialId", FieldsMappingOptions.copyByReference());
            mapping(ProducerProfileDto.class, ProducerProfile.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference())
                    .fields("producer", "producer", FieldsMappingOptions.copyByReference())
                    .fields("sipProfileName", "sipProfile.name", FieldsMappingOptions.copyByReference())
                    .fields("validationProfileName", "validationProfile.name", FieldsMappingOptions.copyByReference())
                    .fields("workflowDefinitionName", "workflowDefinition.name", FieldsMappingOptions.copyByReference());
            mapping(IngestRoutineDto.class, IngestRoutine.class)
                    .fields("producer", "producerProfile.producer", FieldsMappingOptions.copyByReference())
                    .fields("producerProfileName", "producerProfile.name", FieldsMappingOptions.copyByReference())
                    .fields("cronExpression", "job.timing", FieldsMappingOptions.copyByReference())
                    .fields("active", "job.active", FieldsMappingOptions.copyByReference());
            mapping(ValidationProfileDto.class, ValidationProfile.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference())
                    .fields("producer", "producer", FieldsMappingOptions.copyByReference());

            mapping(SipProfileDto.class, SipProfile.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference())
                    .fields("producer", "producer", FieldsMappingOptions.copyByReference());
            mapping(WorkflowDefinitionDto.class, WorkflowDefinition.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference())
                    .fields("producer", "producer", FieldsMappingOptions.copyByReference());
            mapping(AipDeletionRequestDto.class, AipDeletionRequest.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference())
                    .fields("deleted", "deleted", FieldsMappingOptions.copyByReference())
                    .fields("requester", "requester", FieldsMappingOptions.copyByReference())
                    .fields("confirmer1", "confirmer1", FieldsMappingOptions.copyByReference())
                    .fields("confirmer2", "confirmer2", FieldsMappingOptions.copyByReference())
                    .fields("rejectedBy", "rejectedBy", FieldsMappingOptions.copyByReference());
            mapping(AipQueryDto.class, AipQuery.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference());
            mapping(AipQueryDetailDto.class, AipQuery.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference())
                    .fields("query", "query", FieldsMappingOptions.copyByReference())
                    .fields("result", "result", FieldsMappingOptions.copyByReference());
            mapping(AipQueryDetailExportRoutineDto.class, ExportRoutine.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference())
                    .fields("exportTime", "exportTime", FieldsMappingOptions.copyByReference())
                    .fields("config", "config", FieldsMappingOptions.copyByReference());
            mapping(AipBulkDeletionDto.class, AipBulkDeletion.class)
                    .fields("created", "created", FieldsMappingOptions.copyByReference())
                    .fields("updated", "updated", FieldsMappingOptions.copyByReference())
                    .fields("producer", "producer", FieldsMappingOptions.copyByReference())
                    .fields("creator", "creator", FieldsMappingOptions.copyByReference());
        }
    }

    @Bean
    public Mapper dozer() {
        DozerBeanMapper dozer = new DozerBeanMapper();
        dozer.addMapping(new DozerCustomConfig());
        return dozer;
    }
}
