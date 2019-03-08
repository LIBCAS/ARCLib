package cz.cas.lib.arclib.service;

import cz.cas.lib.arclib.dto.JmsDto;

/**
 * workaround to create a proxy
 */
public interface WorkerServiceI {
    void solveIncident(SolveIncidentDto incidentDto);

    void startProcessingOfIngestWorkflow(JmsDto dto);
}
