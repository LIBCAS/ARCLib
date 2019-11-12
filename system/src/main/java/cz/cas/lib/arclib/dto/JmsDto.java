package cz.cas.lib.arclib.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO used in the JMS communication between instances of <code>Coordinator</code>, <code>Worker</code> and other classes
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JmsDto {
    private String id;
    private String userId;
}
