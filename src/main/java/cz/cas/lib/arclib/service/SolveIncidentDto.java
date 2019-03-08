package cz.cas.lib.arclib.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SolveIncidentDto {
    private String incidentId;
    private String config;
}
