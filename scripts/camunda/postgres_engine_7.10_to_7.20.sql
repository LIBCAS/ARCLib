-- https://app.camunda.com/jira/browse/CAM-10275
create index ACT_IDX_HI_IDENT_LNK_TIMESTAMP on ACT_HI_IDENTITYLINK(TIMESTAMP_);

-- https://app.camunda.com/jira/browse/CAM-10616
create index ACT_IDX_HI_JOB_LOG_JOB_CONF on ACT_HI_JOB_LOG(JOB_DEF_CONFIGURATION_);

-- https://app.camunda.com/jira/browse/CAM-11117
drop index ACT_IDX_HI_ACT_INST_START;
create index ACT_IDX_HI_ACT_INST_START_END on ACT_HI_ACTINST(START_TIME_, END_TIME_);

-- https://app.camunda.com/jira/browse/CAM-9920
ALTER TABLE ACT_HI_OP_LOG
  ADD CATEGORY_ varchar(64);

ALTER TABLE ACT_HI_OP_LOG
  ADD EXTERNAL_TASK_ID_ varchar(64);

create table ACT_GE_SCHEMA_LOG (
    ID_ varchar(64),
    TIMESTAMP_ timestamp,
    VERSION_ varchar(255),
    primary key (ID_)
);

insert into ACT_GE_SCHEMA_LOG
values ('0', CURRENT_TIMESTAMP, '7.11.0');

-- https://app.camunda.com/jira/browse/CAM-10129
create index ACT_IDX_HI_OP_LOG_USER_ID on ACT_HI_OP_LOG(USER_ID_);
create index ACT_IDX_HI_OP_LOG_OP_TYPE on ACT_HI_OP_LOG(OPERATION_TYPE_);
create index ACT_IDX_HI_OP_LOG_ENTITY_TYPE on ACT_HI_OP_LOG(ENTITY_TYPE_);

insert into ACT_GE_SCHEMA_LOG
values ('1', CURRENT_TIMESTAMP, '7.11.3');

-- https://app.camunda.com/jira/browse/CAM-10616
create index ACT_IDX_HI_JOB_LOG_JOB_CONF on ACT_HI_JOB_LOG(JOB_DEF_CONFIGURATION_);
¨
insert into ACT_GE_SCHEMA_LOG
values ('2', CURRENT_TIMESTAMP, '7.11.8');

-- https://app.camunda.com/jira/browse/CAM-11117
drop index ACT_IDX_HI_ACT_INST_START;
create index ACT_IDX_HI_ACT_INST_START_END on ACT_HI_ACTINST(START_TIME_, END_TIME_);

insert into ACT_GE_SCHEMA_LOG
values ('3', CURRENT_TIMESTAMP, '7.11.19');

-- insert telemetry.lock in property table - https://jira.camunda.com/browse/CAM-12023  --
insert into ACT_GE_PROPERTY
values ('telemetry.lock', '0', 1);

-- insert installationId.lock in property table - https://jira.camunda.com/browse/CAM-12031  --
insert into ACT_GE_PROPERTY
values ('installationId.lock', '0', 1);

insert into ACT_GE_SCHEMA_LOG
values ('100', CURRENT_TIMESTAMP, '7.12.0');

-- https://app.camunda.com/jira/browse/CAM-10665
ALTER TABLE ACT_HI_OP_LOG
  ADD ANNOTATION_ varchar(4000);

-- https://app.camunda.com/jira/browse/CAM-9855
ALTER TABLE ACT_RU_JOB
  ADD REPEAT_OFFSET_ bigint default 0;

-- https://app.camunda.com/jira/browse/CAM-10672
ALTER TABLE ACT_HI_INCIDENT
  ADD HISTORY_CONFIGURATION_ varchar(255);

-- https://app.camunda.com/jira/browse/CAM-10600
create index ACT_IDX_HI_DETAIL_VAR_INST_ID on ACT_HI_DETAIL(VAR_INST_ID_);

insert into ACT_GE_SCHEMA_LOG
values ('101', CURRENT_TIMESTAMP, '7.12.1');

-- https://app.camunda.com/jira/browse/CAM-11117
drop index ACT_IDX_HI_ACT_INST_START;
create index ACT_IDX_HI_ACT_INST_START_END on ACT_HI_ACTINST(START_TIME_, END_TIME_);

insert into ACT_GE_SCHEMA_LOG
values ('102', CURRENT_TIMESTAMP, '7.12.11');

-- https://jira.camunda.com/browse/CAM-12383
create index ACT_IDX_HI_INCIDENT_CREATE_TIME on ACT_HI_INCIDENT(CREATE_TIME_);
create index ACT_IDX_HI_INCIDENT_END_TIME on ACT_HI_INCIDENT(END_TIME_);

insert into ACT_GE_SCHEMA_LOG
values ('103', CURRENT_TIMESTAMP, '7.12.12');


-- insert telemetry.lock in property table - https://jira.camunda.com/browse/CAM-12023  --
insert into ACT_GE_PROPERTY
values ('telemetry.lock', '0', 1);

-- insert installationId.lock in property table - https://jira.camunda.com/browse/CAM-12031  --
insert into ACT_GE_PROPERTY
values ('installationId.lock', '0', 1);

insert into ACT_GE_SCHEMA_LOG
values ('200', CURRENT_TIMESTAMP, '7.13.0');

-- https://jira.camunda.com/browse/CAM-10953
create index ACT_IDX_HI_VAR_PI_NAME_TYPE on ACT_HI_VARINST(PROC_INST_ID_, NAME_, VAR_TYPE_);


-- https://app.camunda.com/jira/browse/CAM-10784
ALTER TABLE ACT_HI_JOB_LOG
  ADD HOSTNAME_ varchar(255) default null;

-- https://jira.camunda.com/browse/CAM-10378
ALTER TABLE ACT_RU_JOB
  ADD FAILED_ACT_ID_ varchar(255);

ALTER TABLE ACT_HI_JOB_LOG
  ADD FAILED_ACT_ID_ varchar(255);

ALTER TABLE ACT_RU_INCIDENT
  ADD FAILED_ACTIVITY_ID_ varchar(255);

ALTER TABLE ACT_HI_INCIDENT
  ADD FAILED_ACTIVITY_ID_ varchar(255);

-- https://jira.camunda.com/browse/CAM-11616
ALTER TABLE ACT_RU_AUTHORIZATION
  ADD REMOVAL_TIME_ timestamp;
create index ACT_IDX_AUTH_RM_TIME on ACT_RU_AUTHORIZATION(REMOVAL_TIME_);

-- https://jira.camunda.com/browse/CAM-11616
ALTER TABLE ACT_RU_AUTHORIZATION
  ADD ROOT_PROC_INST_ID_ varchar(64);
create index ACT_IDX_AUTH_ROOT_PI on ACT_RU_AUTHORIZATION(ROOT_PROC_INST_ID_);

-- https://jira.camunda.com/browse/CAM-11188
ALTER TABLE ACT_RU_JOBDEF
  ADD DEPLOYMENT_ID_ varchar(64);


-- https://jira.camunda.com/browse/CAM-10978

ALTER TABLE ACT_RU_VARIABLE
  ADD PROC_DEF_ID_ varchar(64);

ALTER TABLE ACT_HI_DETAIL
  ADD INITIAL_ boolean;

insert into ACT_GE_SCHEMA_LOG
values ('201', CURRENT_TIMESTAMP, '7.13.5_1');

-- https://jira.camunda.com/browse/CAM-4441
create index ACT_IDX_TASK_OWNER on ACT_RU_TASK(OWNER_);

insert into ACT_GE_SCHEMA_LOG
values ('202', CURRENT_TIMESTAMP, '7.13.5_2');

-- https://jira.camunda.com/browse/CAM-12383
create index ACT_IDX_HI_INCIDENT_CREATE_TIME on ACT_HI_INCIDENT(CREATE_TIME_);
create index ACT_IDX_HI_INCIDENT_END_TIME on ACT_HI_INCIDENT(END_TIME_);

insert into ACT_GE_SCHEMA_LOG
values ('203', CURRENT_TIMESTAMP, '7.13.6');


-- insert telemetry.lock in property table - https://jira.camunda.com/browse/CAM-12023  --
insert into ACT_GE_PROPERTY
values ('telemetry.lock', '0', 1);

-- insert installationId.lock in property table - https://jira.camunda.com/browse/CAM-12031  --
insert into ACT_GE_PROPERTY
values ('installationId.lock', '0', 1);

insert into ACT_GE_SCHEMA_LOG
values ('300', CURRENT_TIMESTAMP, '7.14.0');

-- https://jira.camunda.com/browse/CAM-12304
ALTER TABLE ACT_RU_VARIABLE
  ADD BATCH_ID_ varchar(64);
CREATE INDEX ACT_IDX_BATCH_ID ON ACT_RU_VARIABLE(BATCH_ID_);
ALTER TABLE ACT_RU_VARIABLE
    ADD CONSTRAINT ACT_FK_VAR_BATCH
    FOREIGN KEY (BATCH_ID_)
    REFERENCES ACT_RU_BATCH (ID_);

-- https://jira.camunda.com/browse/CAM-12411
create index ACT_IDX_VARIABLE_TASK_NAME_TYPE on ACT_RU_VARIABLE(TASK_ID_, NAME_, TYPE_);

insert into ACT_GE_SCHEMA_LOG
values ('400', CURRENT_TIMESTAMP, '7.15.0');

-- https://jira.camunda.com/browse/CAM-13013

create table ACT_RU_TASK_METER_LOG (
  ID_ varchar(64) not null,
  ASSIGNEE_HASH_ bigint,
  TIMESTAMP_ timestamp,
  primary key (ID_)
);

create index ACT_IDX_TASK_METER_LOG_TIME on ACT_RU_TASK_METER_LOG(TIMESTAMP_);

-- https://jira.camunda.com/browse/CAM-13060
ALTER TABLE ACT_RU_INCIDENT
  ADD ANNOTATION_ varchar(4000);

ALTER TABLE ACT_HI_INCIDENT
  ADD ANNOTATION_ varchar(4000);

insert into ACT_GE_SCHEMA_LOG
values ('500', CURRENT_TIMESTAMP, '7.16.0');

create table ACT_RE_CAMFORMDEF (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    KEY_ varchar(255) NOT NULL,
    VERSION_ integer NOT NULL,
    DEPLOYMENT_ID_ varchar(64),
    RESOURCE_NAME_ varchar(4000),
    TENANT_ID_ varchar(64),
    primary key (ID_)
);

insert into ACT_GE_SCHEMA_LOG
values ('600', CURRENT_TIMESTAMP, '7.17.0');

-- https://jira.camunda.com/browse/CAM-14006 --
ALTER TABLE ACT_RU_JOB
  ADD COLUMN LAST_FAILURE_LOG_ID_ varchar(64);

ALTER TABLE ACT_RU_EXT_TASK
  ADD COLUMN LAST_FAILURE_LOG_ID_ varchar(64);

create index ACT_IDX_HI_VARINST_NAME on ACT_HI_VARINST(NAME_);
create index ACT_IDX_HI_VARINST_ACT_INST_ID on ACT_HI_VARINST(ACT_INST_ID_);

insert into ACT_GE_SCHEMA_LOG
values ('700', CURRENT_TIMESTAMP, '7.18.0');

-- https://jira.camunda.com/browse/CAM-14303 --
ALTER TABLE ACT_RU_TASK
  ADD COLUMN LAST_UPDATED_ timestamp;
create index ACT_IDX_TASK_LAST_UPDATED on ACT_RU_TASK(LAST_UPDATED_);

-- https://jira.camunda.com/browse/CAM-14721
ALTER TABLE ACT_RU_BATCH
    ADD COLUMN START_TIME_ timestamp;

-- https://jira.camunda.com/browse/CAM-14722
ALTER TABLE ACT_RU_BATCH
    ADD COLUMN EXEC_START_TIME_ timestamp;
ALTER TABLE ACT_HI_BATCH
    ADD COLUMN EXEC_START_TIME_ timestamp;

insert into ACT_GE_SCHEMA_LOG
values ('800', CURRENT_TIMESTAMP, '7.19.0');

insert into ACT_GE_SCHEMA_LOG
values ('900', CURRENT_TIMESTAMP, '7.20.0');