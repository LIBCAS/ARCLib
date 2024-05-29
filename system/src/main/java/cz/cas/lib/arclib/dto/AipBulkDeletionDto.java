package cz.cas.lib.arclib.dto;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.domain.packages.AipBulkDeletionState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AipBulkDeletionDto {
    private User creator;
    private Producer producer;
    private AipBulkDeletionState state;
    private int deletedCount;
    private int size;
    private boolean deleteIfNewerVersionsDeleted;
    private Instant created;
    private Instant updated;
}
