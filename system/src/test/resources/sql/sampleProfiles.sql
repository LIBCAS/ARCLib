INSERT INTO public.arclib_producer(id, created, updated, name, transfer_area_path) VALUES ('aa7ddcc5-5b81-4747-bfeb-1850d952a359', current_timestamp, current_timestamp, 'Producer 1', 'prod1');
INSERT INTO public.arclib_validation_profile(id, external_id, created, updated, producer_id, "xml","name", editable) VALUES ('7916f84a-8958-4427-9c3c-6232f5326237', '90000', current_timestamp, current_timestamp,'aa7ddcc5-5b81-4747-bfeb-1850d952a359', '<?xml version="1.0"?>' ||chr(13)||chr(10)|| '<profile xmlns="http://www.arclib.lib.cas.cz/VALIDATION_PROFILE">' ||chr(13)||chr(10)||'  <rule>' ||chr(13)||chr(10)|| '    <nodeCheck>' ||chr(13)||chr(10)|| '      <filePathGlobPattern>alto/alto_7033d800-0935-11e4-beed-5ef3fc9ae867_0001.xml</filePathGlobPattern>' ||chr(13)||chr(10)|| '      <xPath>/alto/Description/MeasurementUnit</xPath>' ||chr(13)||chr(10)|| '      <value>pixel</value>' ||chr(13)||chr(10)|| '    </nodeCheck>' ||chr(13)||chr(10)|| '  </rule>' ||chr(13)||chr(10)|| '  <rule>' ||chr(13)||chr(10)|| '    <nodeCheck>' ||chr(13)||chr(10)|| '      <filePathGlobPattern>amdsec/amd_mets_7033d800-0935-11e4-beed-5ef3fc9ae867_0001.xml</filePathGlobPattern>' ||chr(13)||chr(10)|| '      <xPath>/mets/metsHdr/agent/name</xPath>' ||chr(13)||chr(10)|| '      <regex>.*001</regex>' ||chr(13)||chr(10)|| '    </nodeCheck>' ||chr(13)||chr(10)|| '  </rule>' ||chr(13)||chr(10)|| '  <rule>' ||chr(13)||chr(10)|| '    <fileExistenceCheck>' ||chr(13)||chr(10)|| '      <filePathGlobPattern>info_7033d800-0935-11e4-beed-5ef3fc9ae867.xml</filePathGlobPattern>' ||chr(13)||chr(10)|| '    </fileExistenceCheck>' ||chr(13)||chr(10)|| '  </rule>' ||chr(13)||chr(10)|| '</profile>','Validation profile 1', true);
INSERT INTO public.arclib_workflow_definition(id, external_id, created, updated, producer_id, bpmn_definition,"name", editable) VALUES ('b1b6cc1a-0581-4e35-8b42-49e293753c16', '90000', current_timestamp, current_timestamp, 'aa7ddcc5-5b81-4747-bfeb-1850d952a359', '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="2.0.3"><bpmn:process id="ingestWorkflow" isExecutable="true"><bpmn:startEvent id="init" name="init"><bpmn:outgoing>SequenceFlow_10xr812</bpmn:outgoing></bpmn:startEvent><bpmn:serviceTask id="antivirus" name="antivirus" camunda:asyncBefore="true" camunda:delegateExpression="${antivirusDelegate}"><bpmn:incoming>SequenceFlow_1qyy2bs</bpmn:incoming><bpmn:outgoing>SequenceFlow_020l49h</bpmn:outgoing></bpmn:serviceTask><bpmn:subProcess id="SubProcess_0m3ut3d" triggeredByEvent="true"><bpmn:startEvent id="bpmErrorStartEvent" name="bpm error start event"><bpmn:outgoing>SequenceFlow_14npmls</bpmn:outgoing><bpmn:errorEventDefinition errorRef="Error_16v6tah" camunda:errorCodeVariable="errorCode" camunda:errorMessageVariable="errorMessage" /></bpmn:startEvent><bpmn:serviceTask id="bpmErrorHandler" name="bpm error handler" camunda:delegateExpression="${bpmErrorHandlerDelegate}"><bpmn:incoming>SequenceFlow_14npmls</bpmn:incoming><bpmn:outgoing>SequenceFlow_0zt5now</bpmn:outgoing></bpmn:serviceTask><bpmn:endEvent id="bpmErrorEndEvent" name="bpm error end event"><bpmn:incoming>SequenceFlow_0zt5now</bpmn:incoming></bpmn:endEvent><bpmn:sequenceFlow id="SequenceFlow_14npmls" sourceRef="bpmErrorStartEvent" targetRef="bpmErrorHandler" /><bpmn:sequenceFlow id="SequenceFlow_0zt5now" sourceRef="bpmErrorHandler" targetRef="bpmErrorEndEvent" /></bpmn:subProcess><bpmn:serviceTask id="validator" name="validator" camunda:asyncBefore="true" camunda:delegateExpression="${validatorDelegate}"><bpmn:incoming>SequenceFlow_1lv9rpm</bpmn:incoming><bpmn:outgoing>SequenceFlow_0ftlkmr</bpmn:outgoing></bpmn:serviceTask><bpmn:serviceTask id="formatIdentification" name="format identifier" camunda:asyncBefore="true" camunda:delegateExpression="${formatIdentificationDelegate}"><bpmn:incoming>SequenceFlow_03u3ynk</bpmn:incoming><bpmn:outgoing>SequenceFlow_1qyy2bs</bpmn:outgoing></bpmn:serviceTask><bpmn:serviceTask id="fixityCheck" name="fixity check" camunda:asyncBefore="true" camunda:delegateExpression="${fixityCheckerDelegate}"><bpmn:incoming>SequenceFlow_020l49h</bpmn:incoming><bpmn:outgoing>SequenceFlow_1lv9rpm</bpmn:outgoing></bpmn:serviceTask><bpmn:sequenceFlow id="SequenceFlow_020l49h" sourceRef="antivirus" targetRef="fixityCheck" /><bpmn:sequenceFlow id="SequenceFlow_1lv9rpm" sourceRef="fixityCheck" targetRef="validator" /><bpmn:subProcess id="SubProcess_07ljd3i" name="finalize ingest"><bpmn:incoming>SequenceFlow_0ftlkmr</bpmn:incoming><bpmn:serviceTask id="arclibXmlGenerator" name="ArclibXml generator" camunda:asyncBefore="true" camunda:delegateExpression="${arclibXmlGeneratorDelegate}"><bpmn:incoming>SequenceFlow_1iwnh8s</bpmn:incoming><bpmn:outgoing>SequenceFlow_0n2wp4g</bpmn:outgoing></bpmn:serviceTask><bpmn:endEvent id="ingestSuccessEvent" name="ingest success event"><bpmn:extensionElements><camunda:executionListener delegateExpression="${ingestSuccessEventDelegate}" event="start" /></bpmn:extensionElements><bpmn:incoming>success</bpmn:incoming></bpmn:endEvent><bpmn:exclusiveGateway id="successVerifierResult" name="success verifier result?" camunda:asyncBefore="true" default="failedTooManyTimesOrProcessingForTooLong"><bpmn:extensionElements><camunda:properties><camunda:property /></camunda:properties></bpmn:extensionElements><bpmn:incoming>SequenceFlow_1j4hj2j</bpmn:incoming><bpmn:outgoing>failedTooManyTimesOrProcessingForTooLong</bpmn:outgoing><bpmn:outgoing>stillProcessingWait</bpmn:outgoing><bpmn:outgoing>success</bpmn:outgoing><bpmn:outgoing>failedRetry</bpmn:outgoing></bpmn:exclusiveGateway><bpmn:startEvent id="StartEvent_16sfszv"><bpmn:outgoing>SequenceFlow_0a6zqyo</bpmn:outgoing></bpmn:startEvent><bpmn:sequenceFlow id="failedTooManyTimesOrProcessingForTooLong" name="failed too many times or processing for too long" sourceRef="successVerifierResult" targetRef="ingestErrorJoinPoint" /><bpmn:sequenceFlow id="SequenceFlow_0a6zqyo" sourceRef="StartEvent_16sfszv" targetRef="fixityGenerator" /><bpmn:sequenceFlow id="stillProcessingWait" name="still processing, wait" sourceRef="successVerifierResult" targetRef="aipSavedCheckTimer"><bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${aipSavedCheckAttempts &gt; 0 &amp;&amp; archivalStorageResult == ''PROCESSING''}</bpmn:conditionExpression></bpmn:sequenceFlow><bpmn:sequenceFlow id="success" name="success" sourceRef="successVerifierResult" targetRef="ingestSuccessEvent"><bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${archivalStorageResult == ''SUCCESS''}</bpmn:conditionExpression></bpmn:sequenceFlow><bpmn:sequenceFlow id="failedRetry" name="failed, retry" sourceRef="successVerifierResult" targetRef="storeRetryJoinPoint"><bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${aipStoreAttempts &gt; 0 &amp;&amp; archivalStorageResult == ''FAIL''}</bpmn:conditionExpression></bpmn:sequenceFlow><bpmn:intermediateCatchEvent id="aipStoreTimer"><bpmn:incoming>SequenceFlow_11u84yh</bpmn:incoming><bpmn:outgoing>SequenceFlow_1589txq</bpmn:outgoing><bpmn:timerEventDefinition><bpmn:timeDuration xsi:type="bpmn:tFormalExpression">${aipStoreAttemptsInterval}</bpmn:timeDuration></bpmn:timerEventDefinition></bpmn:intermediateCatchEvent><bpmn:intermediateCatchEvent id="aipSavedCheckTimer"><bpmn:incoming>stillProcessingWait</bpmn:incoming><bpmn:outgoing>SequenceFlow_0ooeftp</bpmn:outgoing><bpmn:timerEventDefinition><bpmn:timeDuration xsi:type="bpmn:tFormalExpression">${aipSavedCheckAttemptsInterval}</bpmn:timeDuration></bpmn:timerEventDefinition></bpmn:intermediateCatchEvent><bpmn:endEvent id="ingestErrorEvent" name="ingest error event"><bpmn:incoming>SequenceFlow_08u1i35</bpmn:incoming><bpmn:errorEventDefinition errorRef="Error_1olr0r0" /></bpmn:endEvent><bpmn:serviceTask id="archivalStorage" name="archival storage" camunda:asyncBefore="true" camunda:delegateExpression="${archivalStorageDelegate}"><bpmn:incoming>SequenceFlow_0t0dvxs</bpmn:incoming><bpmn:outgoing>SequenceFlow_0oizux8</bpmn:outgoing></bpmn:serviceTask><bpmn:sequenceFlow id="SequenceFlow_1589txq" sourceRef="aipStoreTimer" targetRef="storeAttemptJoinPoint" /><bpmn:sequenceFlow id="SequenceFlow_0n2wp4g" sourceRef="arclibXmlGenerator" targetRef="storeAttemptJoinPoint" /><bpmn:serviceTask id="ServiceTask_15sn5m2" name="storage success verifier" camunda:asyncBefore="true" camunda:delegateExpression="${storageSuccessVerifierDelegate}"><bpmn:incoming>SequenceFlow_1rgeiby</bpmn:incoming><bpmn:outgoing>SequenceFlow_1j4hj2j</bpmn:outgoing></bpmn:serviceTask><bpmn:sequenceFlow id="SequenceFlow_1j4hj2j" sourceRef="ServiceTask_15sn5m2" targetRef="successVerifierResult" /><bpmn:sequenceFlow id="SequenceFlow_0oizux8" sourceRef="archivalStorage" targetRef="storageResponse" /><bpmn:sequenceFlow id="SequenceFlow_0ooeftp" sourceRef="aipSavedCheckTimer" targetRef="checkStateAttemptJoinPoint" /><bpmn:serviceTask id="arclibXmlExtractor" name="ArclibXml extractor" camunda:asyncBefore="true" camunda:delegateExpression="${arclibXmlExtractorDelegate}"><bpmn:incoming>SequenceFlow_0l27cd5</bpmn:incoming><bpmn:outgoing>SequenceFlow_1iwnh8s</bpmn:outgoing></bpmn:serviceTask><bpmn:sequenceFlow id="SequenceFlow_1iwnh8s" sourceRef="arclibXmlExtractor" targetRef="arclibXmlGenerator" /><bpmn:serviceTask id="fixityGenerator" name="fixity generator" camunda:asyncBefore="true" camunda:delegateExpression="${fixityGeneratorDelegate}"><bpmn:incoming>SequenceFlow_0a6zqyo</bpmn:incoming><bpmn:outgoing>SequenceFlow_0l27cd5</bpmn:outgoing></bpmn:serviceTask><bpmn:sequenceFlow id="SequenceFlow_0l27cd5" sourceRef="fixityGenerator" targetRef="arclibXmlExtractor" /><bpmn:exclusiveGateway id="storageResponse" name="storage response?" camunda:asyncBefore="true" default="responseOk"><bpmn:incoming>SequenceFlow_0oizux8</bpmn:incoming><bpmn:outgoing>responseOk</bpmn:outgoing><bpmn:outgoing>nonStandardResponseRetry</bpmn:outgoing><bpmn:outgoing>nonStandardResponseKill</bpmn:outgoing></bpmn:exclusiveGateway><bpmn:sequenceFlow id="responseOk" name="response OK" sourceRef="storageResponse" targetRef="checkStateAttemptJoinPoint" /><bpmn:sequenceFlow id="nonStandardResponseRetry" name="non-standard response, retry" sourceRef="storageResponse" targetRef="storeRetryJoinPoint"><bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${aipStoreAttempts &gt; 0 &amp;&amp; archivalStorageResult == ''FAIL''}</bpmn:conditionExpression></bpmn:sequenceFlow><bpmn:exclusiveGateway id="storeRetryJoinPoint" name="store retry join point" camunda:asyncBefore="true"><bpmn:incoming>nonStandardResponseRetry</bpmn:incoming><bpmn:incoming>failedRetry</bpmn:incoming><bpmn:outgoing>SequenceFlow_11u84yh</bpmn:outgoing></bpmn:exclusiveGateway><bpmn:sequenceFlow id="SequenceFlow_11u84yh" sourceRef="storeRetryJoinPoint" targetRef="aipStoreTimer" /><bpmn:sequenceFlow id="nonStandardResponseKill" name="non-standard response, kill" sourceRef="storageResponse" targetRef="ingestErrorJoinPoint"><bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${aipStoreAttempts &lt; 1 &amp;&amp; archivalStorageResult == ''FAIL''}</bpmn:conditionExpression></bpmn:sequenceFlow><bpmn:exclusiveGateway id="checkStateAttemptJoinPoint" name="check state attempt join point"><bpmn:incoming>SequenceFlow_0ooeftp</bpmn:incoming><bpmn:incoming>responseOk</bpmn:incoming><bpmn:outgoing>SequenceFlow_1rgeiby</bpmn:outgoing></bpmn:exclusiveGateway><bpmn:sequenceFlow id="SequenceFlow_1rgeiby" sourceRef="checkStateAttemptJoinPoint" targetRef="ServiceTask_15sn5m2" /><bpmn:exclusiveGateway id="ingestErrorJoinPoint" name="ingest error join point"><bpmn:incoming>nonStandardResponseKill</bpmn:incoming><bpmn:incoming>failedTooManyTimesOrProcessingForTooLong</bpmn:incoming><bpmn:outgoing>SequenceFlow_08u1i35</bpmn:outgoing></bpmn:exclusiveGateway><bpmn:sequenceFlow id="SequenceFlow_08u1i35" sourceRef="ingestErrorJoinPoint" targetRef="ingestErrorEvent" /><bpmn:exclusiveGateway id="storeAttemptJoinPoint" name="store attempt join point"><bpmn:incoming>SequenceFlow_0n2wp4g</bpmn:incoming><bpmn:incoming>SequenceFlow_1589txq</bpmn:incoming><bpmn:outgoing>SequenceFlow_0t0dvxs</bpmn:outgoing></bpmn:exclusiveGateway><bpmn:sequenceFlow id="SequenceFlow_0t0dvxs" sourceRef="storeAttemptJoinPoint" targetRef="archivalStorage" /></bpmn:subProcess><bpmn:subProcess id="SubProcess_15fffya" triggeredByEvent="true"><bpmn:startEvent id="storageErrorStartEvent" name="storage error start event"><bpmn:outgoing>SequenceFlow_0wgkgq5</bpmn:outgoing><bpmn:errorEventDefinition errorRef="Error_1olr0r0" camunda:errorCodeVariable="errorCode" camunda:errorMessageVariable="errorMessage" /></bpmn:startEvent><bpmn:serviceTask id="storageErrorHandler" name="storage error handler" camunda:delegateExpression="${bpmErrorHandlerDelegate}"><bpmn:incoming>SequenceFlow_0wgkgq5</bpmn:incoming><bpmn:outgoing>SequenceFlow_1v9rnml</bpmn:outgoing></bpmn:serviceTask><bpmn:endEvent id="storageErrorEndEvent" name="storage error end event"><bpmn:incoming>SequenceFlow_1v9rnml</bpmn:incoming></bpmn:endEvent><bpmn:sequenceFlow id="SequenceFlow_0wgkgq5" sourceRef="storageErrorStartEvent" targetRef="storageErrorHandler" /><bpmn:sequenceFlow id="SequenceFlow_1v9rnml" sourceRef="storageErrorHandler" targetRef="storageErrorEndEvent" /></bpmn:subProcess><bpmn:sequenceFlow id="SequenceFlow_1qyy2bs" sourceRef="formatIdentification" targetRef="antivirus" /><bpmn:sequenceFlow id="SequenceFlow_10xr812" sourceRef="init" targetRef="duplicateSipCheck" /><bpmn:serviceTask id="duplicateSipCheck" name="duplicate SIP check" camunda:asyncBefore="true" camunda:delegateExpression="${duplicateSipCheckDelegate}"><bpmn:incoming>SequenceFlow_10xr812</bpmn:incoming><bpmn:outgoing>SequenceFlow_03u3ynk</bpmn:outgoing></bpmn:serviceTask><bpmn:sequenceFlow id="SequenceFlow_03u3ynk" sourceRef="duplicateSipCheck" targetRef="formatIdentification" /><bpmn:sequenceFlow id="SequenceFlow_0ftlkmr" sourceRef="validator" targetRef="SubProcess_07ljd3i" /></bpmn:process><bpmn:error id="Error_1j240sk" name="runtimeError" errorCode="java.lang.RuntimeException" /><bpmn:error id="Error_16v6tah" name="bpmError" errorCode="processFailure" /><bpmn:error id="Error_1olr0r0" name="storageError" errorCode="storageFailure" /><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="ingestWorkflow"><bpmndi:BPMNShape id="StartEvent_1y3rro6_di" bpmnElement="init"><dc:Bounds x="-408" y="-349" width="36" height="36" /><bpmndi:BPMNLabel><dc:Bounds x="-398" y="-303" width="15" height="14" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape id="ServiceTask_0z6wumr_di" bpmnElement="antivirus"><dc:Bounds x="-89" y="-371" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNShape id="ServiceTask_0nhvpwh_di" bpmnElement="fixityGenerator"><dc:Bounds x="-279" y="-215" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNShape id="SubProcess_0m3ut3d_di" bpmnElement="SubProcess_0m3ut3d" isExpanded="true"><dc:Bounds x="-403" y="216" width="306" height="122" /></bpmndi:BPMNShape><bpmndi:BPMNShape id="StartEvent_0osz2mu_di" bpmnElement="bpmErrorStartEvent"><dc:Bounds x="-383" y="258" width="36" height="36" /><bpmndi:BPMNLabel><dc:Bounds x="-393" y="296" width="74" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape id="ServiceTask_1vdb3kz_di" bpmnElement="bpmErrorHandler"><dc:Bounds x="-305" y="236" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNShape id="EndEvent_0s1mmir_di" bpmnElement="bpmErrorEndEvent"><dc:Bounds x="-169" y="258" width="36" height="36" /><bpmndi:BPMNLabel><dc:Bounds x="-185" y="296" width="70" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_14npmls_di" bpmnElement="SequenceFlow_14npmls"><di:waypoint x="-347" y="276" /><di:waypoint x="-305" y="276" /><bpmndi:BPMNLabel><dc:Bounds x="-370" y="163" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_0zt5now_di" bpmnElement="SequenceFlow_0zt5now"><di:waypoint x="-205" y="276" /><di:waypoint x="-169" y="276" /><bpmndi:BPMNLabel><dc:Bounds x="-231" y="163" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNShape id="ServiceTask_1y220pq_di" bpmnElement="validator"><dc:Bounds x="170" y="-371" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNShape id="ServiceTask_07dfwlp_di" bpmnElement="formatIdentification"><dc:Bounds x="-216" y="-371" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNShape id="ServiceTask_1clbu9d_di" bpmnElement="fixityCheck"><dc:Bounds x="37" y="-371" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_020l49h_di" bpmnElement="SequenceFlow_020l49h"><di:waypoint x="11" y="-331" /><di:waypoint x="37" y="-331" /><bpmndi:BPMNLabel><dc:Bounds x="-178" y="-352" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_1lv9rpm_di" bpmnElement="SequenceFlow_1lv9rpm"><di:waypoint x="137" y="-331" /><di:waypoint x="170" y="-331" /><bpmndi:BPMNLabel><dc:Bounds x="-39" y="-352" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNShape id="SubProcess_07ljd3i_di" bpmnElement="SubProcess_07ljd3i" isExpanded="true"><dc:Bounds x="-401" y="-243" width="833" height="444" /></bpmndi:BPMNShape><bpmndi:BPMNShape id="ServiceTask_0b2g5c0_di" bpmnElement="arclibXmlGenerator"><dc:Bounds x="4" y="-215" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNShape id="EndEvent_0lrb2a4_di" bpmnElement="ingestSuccessEvent"><dc:Bounds x="-308" y="100" width="36" height="36" /><bpmndi:BPMNLabel><dc:Bounds x="-326" y="140" width="73" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape id="ExclusiveGateway_16nlbuk_di" bpmnElement="successVerifierResult" isMarkerVisible="true"><dc:Bounds x="-315" y="0" width="50" height="50" /><bpmndi:BPMNLabel><dc:Bounds x="-370" y="-5" width="77" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape id="StartEvent_16sfszv_di" bpmnElement="StartEvent_16sfszv"><dc:Bounds x="-363" y="-193" width="36" height="36" /><bpmndi:BPMNLabel><dc:Bounds x="-376" y="-148" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_1qfqjnh_di" bpmnElement="failedTooManyTimesOrProcessingForTooLong"><di:waypoint x="-281" y="41" /><di:waypoint x="-259" y="83" /><di:waypoint x="197" y="83" /><bpmndi:BPMNLabel><dc:Bounds x="-234" y="91" width="88" height="53" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_0a6zqyo_di" bpmnElement="SequenceFlow_0a6zqyo"><di:waypoint x="-327" y="-175" /><di:waypoint x="-279" y="-175" /><bpmndi:BPMNLabel><dc:Bounds x="-361" y="-135" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_15x18qm_di" bpmnElement="stillProcessingWait"><di:waypoint x="-280" y="10" /><di:waypoint x="-167" y="-13" /><bpmndi:BPMNLabel><dc:Bounds x="-277" y="-31" width="76" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_0np9etf_di" bpmnElement="success"><di:waypoint x="-290" y="50" /><di:waypoint x="-290" y="100" /><bpmndi:BPMNLabel><dc:Bounds x="-338" y="72" width="40" height="14" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_1gtsp32_di" bpmnElement="failedRetry"><di:waypoint x="-265" y="25" /><di:waypoint x="361" y="25" /><di:waypoint x="361" y="-49" /><bpmndi:BPMNLabel><dc:Bounds x="-215" y="31" width="54" height="14" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNShape id="IntermediateCatchEvent_10c18o1_di" bpmnElement="aipStoreTimer"><dc:Bounds x="343" y="-193" width="36" height="36" /><bpmndi:BPMNLabel><dc:Bounds x="-235" y="-148" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape id="IntermediateCatchEvent_0wx1ml4_di" bpmnElement="aipSavedCheckTimer"><dc:Bounds x="-168" y="-35" width="36" height="36" /><bpmndi:BPMNLabel><dc:Bounds x="-96" y="59" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape id="EndEvent_0kegd0b_di" bpmnElement="ingestErrorEvent"><dc:Bounds x="343" y="65" width="36" height="36" /><bpmndi:BPMNLabel><dc:Bounds x="317" y="111" width="87" height="14" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape id="SubProcess_15fffya_di" bpmnElement="SubProcess_15fffya" isExpanded="true"><dc:Bounds x="-34" y="216" width="306" height="122" /></bpmndi:BPMNShape><bpmndi:BPMNShape id="StartEvent_0cn1yhs_di" bpmnElement="storageErrorStartEvent"><dc:Bounds x="-14" y="258" width="36" height="36" /><bpmndi:BPMNLabel><dc:Bounds x="-31" y="296" width="89" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape id="ServiceTask_0qpoqo1_di" bpmnElement="storageErrorHandler"><dc:Bounds x="64" y="236" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNShape id="EndEvent_11w4twn_di" bpmnElement="storageErrorEndEvent"><dc:Bounds x="200" y="258" width="36" height="36" /><bpmndi:BPMNLabel><dc:Bounds x="176" y="296" width="85" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_0wgkgq5_di" bpmnElement="SequenceFlow_0wgkgq5"><di:waypoint x="22" y="276" /><di:waypoint x="64" y="276" /><bpmndi:BPMNLabel><dc:Bounds x="-1" y="163" width="0" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_1v9rnml_di" bpmnElement="SequenceFlow_1v9rnml"><di:waypoint x="164" y="276" /><di:waypoint x="200" y="276" /><bpmndi:BPMNLabel><dc:Bounds x="138" y="163" width="0" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_1qyy2bs_di" bpmnElement="SequenceFlow_1qyy2bs"><di:waypoint x="-116" y="-331" /><di:waypoint x="-89" y="-331" /><bpmndi:BPMNLabel><dc:Bounds x="-270" y="-352" width="0" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_10xr812_di" bpmnElement="SequenceFlow_10xr812"><di:waypoint x="-372" y="-331" /><di:waypoint x="-333" y="-331" /><bpmndi:BPMNLabel><dc:Bounds x="-402" y="-352" width="0" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNShape id="ServiceTask_1snmjbd_di" bpmnElement="archivalStorage"><dc:Bounds x="211" y="-215" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_1589txq_di" bpmnElement="SequenceFlow_1589txq"><di:waypoint x="361" y="-193" /><di:waypoint x="361" y="-229" /><di:waypoint x="158" y="-229" /><di:waypoint x="158" y="-200" /><bpmndi:BPMNLabel><dc:Bounds x="-220" y="-135" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_0n2wp4g_di" bpmnElement="SequenceFlow_0n2wp4g"><di:waypoint x="104" y="-175" /><di:waypoint x="133" y="-175" /><bpmndi:BPMNLabel><dc:Bounds x="-305" y="-88" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNShape id="ServiceTask_15sn5m2_di" bpmnElement="ServiceTask_15sn5m2"><dc:Bounds x="-279" y="-114" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_1j4hj2j_di" bpmnElement="SequenceFlow_1j4hj2j"><di:waypoint x="-279" y="-74" /><di:waypoint x="-290" y="-74" /><di:waypoint x="-290" y="0" /><bpmndi:BPMNLabel><dc:Bounds x="-18" y="-88" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_0oizux8_di" bpmnElement="SequenceFlow_0oizux8"><di:waypoint x="222" y="-135" /><di:waypoint x="222" y="-99" /><bpmndi:BPMNLabel><dc:Bounds x="-165" y="-88" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_0ooeftp_di" bpmnElement="SequenceFlow_0ooeftp"><di:waypoint x="-132" y="-17" /><di:waypoint x="8" y="-17" /><di:waypoint x="8" y="-49" /><bpmndi:BPMNLabel><dc:Bounds x="-81" y="-10" width="90" height="12" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNShape id="ServiceTask_0fxsc9x_di" bpmnElement="arclibXmlExtractor"><dc:Bounds x="-149" y="-215" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_1iwnh8s_di" bpmnElement="SequenceFlow_1iwnh8s"><di:waypoint x="-49" y="-175" /><di:waypoint x="4" y="-175" /></bpmndi:BPMNEdge><bpmndi:BPMNShape id="ServiceTask_0tr54un_di" bpmnElement="duplicateSipCheck"><dc:Bounds x="-333" y="-371" width="100" height="80" /></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_03u3ynk_di" bpmnElement="SequenceFlow_03u3ynk"><di:waypoint x="-233" y="-331" /><di:waypoint x="-216" y="-331" /></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_0l27cd5_di" bpmnElement="SequenceFlow_0l27cd5"><di:waypoint x="-179" y="-175" /><di:waypoint x="-149" y="-175" /></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_0ftlkmr_di" bpmnElement="SequenceFlow_0ftlkmr"><di:waypoint x="220" y="-291" /><di:waypoint x="220" y="-267" /><di:waypoint x="16" y="-267" /><di:waypoint x="16" y="-243" /></bpmndi:BPMNEdge><bpmndi:BPMNShape id="ExclusiveGateway_039qlqk_di" bpmnElement="storageResponse" isMarkerVisible="true"><dc:Bounds x="197" y="-99" width="50" height="50" /><bpmndi:BPMNLabel><dc:Bounds x="157" y="-111" width="52" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_0h43nr6_di" bpmnElement="responseOk"><di:waypoint x="197" y="-74" /><di:waypoint x="33" y="-74" /><bpmndi:BPMNLabel><dc:Bounds x="76" y="-94" width="65" height="14" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_0toj5rg_di" bpmnElement="nonStandardResponseRetry"><di:waypoint x="247" y="-74" /><di:waypoint x="336" y="-74" /><bpmndi:BPMNLabel><dc:Bounds x="250" y="-106" width="74" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNShape id="ExclusiveGateway_15fbe7w_di" bpmnElement="storeRetryJoinPoint" isMarkerVisible="true"><dc:Bounds x="336" y="-99" width="50" height="50" /><bpmndi:BPMNLabel><dc:Bounds x="364" y="-111" width="70" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_11u84yh_di" bpmnElement="SequenceFlow_11u84yh"><di:waypoint x="361" y="-99" /><di:waypoint x="361" y="-157" /></bpmndi:BPMNEdge><bpmndi:BPMNEdge id="SequenceFlow_0xkuhz8_di" bpmnElement="nonStandardResponseKill"><di:waypoint x="222" y="-49" /><di:waypoint x="222" y="58" /><bpmndi:BPMNLabel><dc:Bounds x="154" y="-40" width="66" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNShape id="ExclusiveGateway_1q7mq15_di" bpmnElement="checkStateAttemptJoinPoint" isMarkerVisible="true"><dc:Bounds x="-17" y="-99" width="50" height="50" /><bpmndi:BPMNLabel><dc:Bounds x="-35" y="-128" width="85" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_1rgeiby_di" bpmnElement="SequenceFlow_1rgeiby"><di:waypoint x="-17" y="-74" /><di:waypoint x="-179" y="-74" /></bpmndi:BPMNEdge><bpmndi:BPMNShape id="ExclusiveGateway_0fc3qax_di" bpmnElement="ingestErrorJoinPoint" isMarkerVisible="true"><dc:Bounds x="197" y="58" width="50" height="50" /><bpmndi:BPMNLabel><dc:Bounds x="183.5" y="113" width="77" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_08u1i35_di" bpmnElement="SequenceFlow_08u1i35"><di:waypoint x="247" y="83" /><di:waypoint x="343" y="83" /></bpmndi:BPMNEdge><bpmndi:BPMNShape id="ExclusiveGateway_1dmgh2f_di" bpmnElement="storeAttemptJoinPoint" isMarkerVisible="true"><dc:Bounds x="133" y="-200" width="50" height="50" /><bpmndi:BPMNLabel><dc:Bounds x="115" y="-149" width="85" height="27" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNEdge id="SequenceFlow_0t0dvxs_di" bpmnElement="SequenceFlow_0t0dvxs"><di:waypoint x="183" y="-175" /><di:waypoint x="211" y="-175" /></bpmndi:BPMNEdge></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>
', 'Workflow definition 1', true);
INSERT INTO public.arclib_sip_profile(id, external_id, name, created, updated, producer_id, editable, sip_metadata_path_regex, authorial_id_file_path_regex, authorial_id_xpath, xsl)
VALUES ('3cdc21a2-8b2a-4b56-81a8-3d2ee82ab6b4', '90000', 'Sip Profile 1', current_timestamp, current_timestamp, 'aa7ddcc5-5b81-4747-bfeb-1850d952a359', true, '[^\/]+\/mets.*\.xml', '[^\/]+\/mets.*\.xml',
'if (/mets/@TYPE = ''Monograph'') then  	if(/mets/dmdSec[starts-with(@ID,''MODSMD_VOLUME'')]//identifier[@type=''uuid'']/text()) 		then /mets/dmdSec[starts-with(@ID,''MODSMD_VOLUME'')]//identifier[@type=''uuid'']/text() 	else 		/mets/dmdSec[starts-with(@ID,''MODSMD_SUPPL'')]//identifier[@type=''uuid'']/text() else 	if(/mets/@TYPE = ''Periodical'') then 		if(/mets/dmdSec[starts-with(@ID,''MODSMD_ISSUE'')]//identifier[@type=''uuid'']/text()) 			then /mets/dmdSec[starts-with(@ID,''MODSMD_ISSUE'')]//identifier[@type=''uuid'']/text() 		else 			/mets/dmdSec[starts-with(@ID,''MODSMD_SUPPL'')]//identifier[@type=''uuid'']/text() 	else 		null',
'<xsl:stylesheet version="2.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:METS="http://www.loc.gov/METS/"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:ARCLib="http://arclib.lib.cas.cz/ARCLIB_XSD"
xmlns:premis="info:lc/xmlns/premis-v2"
xmlns:mix="http://www.loc.gov/mix/v20"
xmlns:xlink="http://www.w3.org/1999/xlink"
>
<!-- == SPECIFICATION OF INPUT FILES == -->
<!--Path to SIP passed from the Java code (with trailing / (slash))-->
<xsl:param name="pathToSip"/>
<!--Path to main XML record (METS in the root folder) of SIP passed from the Java code-->
<xsl:param name="sipMetadataPath"/>
<xsl:variable name="sipMetadata" select="document(concat(''file:///'', $sipMetadataPath))/*"/>
<!-- == SETTING THE DEFAULT DOCUMENT == -->
<xsl:template match="/">
<xsl:apply-templates select="$sipMetadata"/>
</xsl:template>
<!-- == CUSTOM TEMPLATES == -->
<xsl:template match="/METS:mets">
<xsl:variable name="amdSecs"
select="document(//METS:fileGrp[@ID=''TECHMDGRP'']/METS:file/METS:FLocat/concat(''file:///'', string-join(tokenize($sipMetadataPath, ''/'')[position() lt last()], ''/''),''/'',@xlink:href))"/>
<xsl:copy>
<xsl:attribute name="LABEL">
<xsl:value-of select="@LABEL"/>
</xsl:attribute>
<xsl:attribute name="TYPE">
<xsl:value-of select="@TYPE"/>
</xsl:attribute>
<xsl:element name="METS:metsHdr">
<xsl:copy-of select="/METS:mets/METS:metsHdr/METS:agent"/>
</xsl:element>
<xsl:copy-of select="METS:dmdSec"/>
<!--generate ARCLib:formats-->
<xsl:element name="METS:amdSec">
<xsl:element name="METS:techMD">
<xsl:attribute name="ID">ARCLIB_001</xsl:attribute>
<xsl:element name="METS:mdWrap">
<xsl:attribute name="MDTYPE">OTHER</xsl:attribute>
<xsl:element name="METS:xmlData">
<xsl:element name="ARCLib:formats">
<xsl:for-each-group
select="$amdSecs/METS:mets/METS:amdSec/METS:techMD/METS:mdWrap/
METS:xmlData/premis:object"
group-by="concat(
premis:objectCharacteristics/premis:format/
    premis:formatDesignation/premis:formatName,
    premis:objectCharacteristics/premis:format/
    premis:formatDesignation/premis:formatVersion,
    premis:objectCharacteristics/premis:format/
premis:formatRegistry/premis:formatRegistryKey,
premis:objectCharacteristics/premis:format/
premis:formatRegistry/premis:formatRegistryName,
     premis:objectCharacteristics/premis:creatingApplication/
     premis:creatingApplicationName,
     premis:objectCharacteristics/premis:creatingApplication/
     premis:creatingApplicationVersion,
     substring(premis:objectCharacteristics/premis:creatingApplication/
     premis:dateCreatedByApplication, 1, 10),
premis:preservationLevel/premis:preservationLevelValue)">
<xsl:element name="ARCLib:format">
<xsl:element name="ARCLib:fileFormat">
<xsl:value-of
    select="premis:objectCharacteristics/premis:format/
    premis:formatDesignation/premis:formatName"/>
</xsl:element>
<xsl:element name="ARCLib:formatVersion">
<xsl:value-of
    select="premis:objectCharacteristics/premis:format/
    premis:formatDesignation/premis:formatVersion"/>
</xsl:element>
<xsl:element name="ARCLib:formatRegistryKey">
<xsl:value-of
    select="premis:objectCharacteristics/premis:format/
premis:formatRegistry/premis:formatRegistryKey"/>
</xsl:element>
<xsl:element name="ARCLib:formatRegistryName">
<xsl:value-of
    select="premis:objectCharacteristics/premis:format/
premis:formatRegistry/premis:formatRegistryName"/>
</xsl:element>
<xsl:element name="ARCLib:creatingApplicationName">
<xsl:value-of
    select="premis:objectCharacteristics/premis:creatingApplication/
    premis:creatingApplicationName"/>
</xsl:element>
<xsl:element name="ARCLib:creatingApplicationVersion">
<xsl:value-of
    select="premis:objectCharacteristics/premis:creatingApplication/
    premis:creatingApplicationVersion"/>
</xsl:element>
<xsl:element name="ARCLib:dateCreatedByApplication">
<xsl:value-of
    select="substring(premis:objectCharacteristics/
    premis:creatingApplication/premis:dateCreatedByApplication, 1, 10)"/>
</xsl:element>
<xsl:element name="ARCLib:preservationLevelValue">
<xsl:value-of
    select="premis:preservationLevel/premis:preservationLevelValue"/>
</xsl:element>
<xsl:element name="ARCLib:fileCount">
<xsl:value-of select="count(current-group())"/>
</xsl:element>
<xsl:element name="ARCLib:size">
<xsl:value-of
    select="xs:decimal(sum(current-group()/premis:objectCharacteristics/premis:size/text()))"/>
</xsl:element>
</xsl:element>
</xsl:for-each-group>
</xsl:element>
</xsl:element>
</xsl:element>
</xsl:element>
</xsl:element>
<!--generate ARCLib:eventAgents-->
<xsl:element name="METS:amdSec">
<xsl:element name="METS:techMD">
<xsl:attribute name="ID">ARCLIB_002</xsl:attribute>
<xsl:element name="METS:mdWrap">
<xsl:attribute name="MDTYPE">OTHER</xsl:attribute>
<xsl:element name="METS:xmlData">
<xsl:element name="ARCLib:eventAgents">
<xsl:variable name="joinedEventAgents">
<xsl:call-template name="eventAgentsT">
<xsl:with-param name="amdSecs" select="$amdSecs"/>
</xsl:call-template>
</xsl:variable>
<xsl:for-each select="$joinedEventAgents/*">
<xsl:sort select="ARCLib:eventDate"/>
<xsl:sort select="ARCLib:agentName"/>
<xsl:copy-of select="."/>
</xsl:for-each>
</xsl:element>
</xsl:element>
</xsl:element>
</xsl:element>
</xsl:element>
<!--generate ARCLib:ImageCaptureMetadata-->
<xsl:element name="METS:amdSec">
<xsl:element name="METS:techMD">
<xsl:attribute name="ID">ARCLIB_003</xsl:attribute>
<xsl:element name="METS:mdWrap">
<xsl:attribute name="MDTYPE">OTHER</xsl:attribute>
<xsl:element name="METS:xmlData">
<xsl:element name="ARCLib:ImageCaptureInformation">
<xsl:for-each-group
select="$amdSecs/METS:mets/METS:amdSec/METS:techMD/METS:mdWrap/
METS:xmlData/mix:mix/mix:ImageCaptureMetadata"
group-by="concat(substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10),
    mix:GeneralCaptureInformation/mix:imageProducer,
    mix:ScannerCapture/mix:ScannerModel/mix:scannerModelSerialNo)">
<xsl:element name="ARCLib:ImageCaptureMetadata">
<xsl:element name="ARCLib:dateCreated">
<xsl:value-of
    select="substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10)"/>
</xsl:element>
<xsl:element name="ARCLib:imageProducer">
<xsl:value-of
    select="mix:GeneralCaptureInformation/mix:imageProducer"/>
</xsl:element>
<xsl:element name="ARCLib:scannerModelSerialNo">
<xsl:value-of
    select="mix:ScannerCapture/mix:ScannerModel/mix:scannerModelSerialNo"/>
</xsl:element>
<xsl:element name="ARCLib:eventCount">
<xsl:value-of select="count(current-group())"/>
</xsl:element>
</xsl:element>
</xsl:for-each-group>
</xsl:element>
</xsl:element>
</xsl:element>
</xsl:element>
</xsl:element>
<!--generate ARCLib:creatingApplication-->
<xsl:element name="METS:amdSec">
<xsl:element name="METS:techMD">
<xsl:attribute name="ID">ARCLIB_004</xsl:attribute>
<xsl:element name="METS:mdWrap">
<xsl:attribute name="MDTYPE">OTHER</xsl:attribute>
<xsl:element name="METS:xmlData">
<xsl:element name="ARCLib:creatingApplications">
<xsl:for-each-group
select="$amdSecs/METS:mets/METS:amdSec/METS:techMD/METS:mdWrap/
METS:xmlData/premis:object"
group-by="concat(premis:objectCharacteristics/premis:creatingApplication/
premis:creatingApplicationName,
    premis:objectCharacteristics/premis:creatingApplication/
    premis:creatingApplicationVersion,
    substring(premis:objectCharacteristics/premis:creatingApplication/
    premis:dateCreatedByApplication, 1, 10))">
<xsl:element name="ARCLib:creatingApplication">
<xsl:element name="ARCLib:creatingApplicationName">
<xsl:value-of
    select="premis:objectCharacteristics/premis:creatingApplication/
    premis:creatingApplicationName"/>
</xsl:element>
<xsl:element name="ARCLib:creatingApplicationVersion">
<xsl:value-of
    select="premis:objectCharacteristics/premis:creatingApplication/
    premis:creatingApplicationVersion"/>
</xsl:element>
<xsl:element name="ARCLib:dateCreatedByApplication">
<xsl:value-of
    select="substring(premis:objectCharacteristics/
    premis:creatingApplication/premis:dateCreatedByApplication, 1, 10)"/>
</xsl:element>
<xsl:element name="ARCLib:eventCount">
<xsl:value-of select="count(current-group())"/>
</xsl:element>
</xsl:element>
</xsl:for-each-group>
</xsl:element>
</xsl:element>
</xsl:element>
</xsl:element>
</xsl:element>
</xsl:copy>
</xsl:template>

<xsl:template name="eventAgentsT">
<xsl:param name="amdSecs"/>
<xsl:for-each-group select="$amdSecs/METS:mets/METS:amdSec/METS:digiprovMD/METS:mdWrap/
    METS:xmlData/premis:agent"
group-by="premis:agentName">
<xsl:variable name="agentName" select="premis:agentName"/>
<xsl:variable name="agentId" select="premis:agentIdentifier/premis:agentIdentifierValue"/>
<xsl:for-each-group select="$amdSecs/METS:mets/METS:amdSec/METS:digiprovMD/METS:mdWrap/
    METS:xmlData/premis:event[premis:linkingAgentIdentifier/
    premis:linkingAgentIdentifierValue/text() =
    $agentId]"
group-by="concat(premis:eventType,substring(premis:eventDateTime,1,10))">
<xsl:element name="ARCLib:eventAgent">
<xsl:element name="ARCLib:eventType">
<xsl:value-of
select="premis:eventType"/>
</xsl:element>
<xsl:element name="ARCLib:agentName">
<xsl:value-of
select="$agentName"/>
</xsl:element>
<xsl:element name="ARCLib:eventDate">
<xsl:value-of
select="substring(premis:eventDateTime,1,10)"/>
</xsl:element>
<xsl:element name="ARCLib:fileCount">
<xsl:value-of
select="count(current-group()/premis:linkingObjectIdentifier)"/>
</xsl:element>
</xsl:element>
</xsl:for-each-group>
</xsl:for-each-group>
<xsl:for-each-group
select="$amdSecs/METS:mets/METS:amdSec/METS:techMD/
METS:mdWrap/METS:xmlData/mix:mix/mix:ImageCaptureMetadata"
group-by="concat(mix:ScannerCapture/mix:ScannerModel/mix:scannerModelName,
    mix:ScannerCapture/mix:ScannerModel/mix:scannerModelSerialNo,
    substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10))">
<xsl:element name="ARCLib:eventAgent">
<xsl:element name="ARCLib:eventType">
<xsl:text>capture</xsl:text>
</xsl:element>
<xsl:element name="ARCLib:agentName">
<xsl:value-of
select="mix:ScannerCapture/mix:ScannerModel/mix:scannerModelName"/>
</xsl:element>
<xsl:element name="ARCLib:scannerModelSerialNo">
<xsl:value-of
select="mix:ScannerCapture/mix:ScannerModel/mix:scannerModelSerialNo"/>
</xsl:element>
<xsl:element name="ARCLib:eventDate">
<xsl:value-of
select="substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10)"/>
</xsl:element>
<xsl:element name="ARCLib:fileCount">
<xsl:value-of select="count(current-group())"/>
</xsl:element>
</xsl:element>
</xsl:for-each-group>
<xsl:for-each-group
select="$amdSecs/METS:mets/METS:amdSec/METS:techMD/
METS:mdWrap/METS:xmlData/mix:mix/mix:ImageCaptureMetadata"
group-by="concat(mix:ScannerCapture/mix:ScanningSystemSoftware/mix:scanningSoftwareName,
    substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10))">
<xsl:element name="ARCLib:eventAgent">
<xsl:element name="ARCLib:eventType">
<xsl:text>capture</xsl:text>
</xsl:element>
<xsl:element name="ARCLib:agentName">
<xsl:value-of
select="mix:ScannerCapture/mix:ScanningSystemSoftware/mix:scanningSoftwareName"/>
</xsl:element>
<xsl:element name="ARCLib:eventDate">
<xsl:value-of
select="substring(mix:GeneralCaptureInformation/mix:dateTimeCreated, 1, 10)"/>
</xsl:element>
<xsl:element name="ARCLib:fileCount">
<xsl:value-of select="count(current-group())"/>
</xsl:element>
</xsl:element>
</xsl:for-each-group>
</xsl:template>

<!-- == GENERIC TEMPLATES: == -->
<!-- Copy the children of the current node. -->
<xsl:template name="copy-children">
<xsl:copy-of select="./*"/>
</xsl:template>
<!-- Generic identity template -->
<xsl:template match="node()|@*">
<xsl:copy>
<xsl:apply-templates select="@*|node()"/>
</xsl:copy>
</xsl:template>
</xsl:stylesheet>
');
INSERT INTO public.arclib_producer_profile(id, external_id, created, updated, producer_id, validation_profile_id, sip_profile_id, workflow_config, workflow_definition_id, name, debugging_mode_active)
VALUES ('b0384aeb-5169-459a-b5f4-483e6ad7b949', '901', current_timestamp, current_timestamp, 'aa7ddcc5-5b81-4747-bfeb-1850d952a359', '7916f84a-8958-4427-9c3c-6232f5326237', '3cdc21a2-8b2a-4b56-81a8-3d2ee82ab6b4', '{}', 'b1b6cc1a-0581-4e35-8b42-49e293753c16','Producer profile 1', true);