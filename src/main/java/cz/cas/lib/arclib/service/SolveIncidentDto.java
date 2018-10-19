package cz.cas.lib.arclib.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SolveIncidentDto {
    String incidentId;
    String config;
}
