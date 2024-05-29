package cz.cas.lib.arclib.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AipBulkDeletionCreateDto {
    private String aipIds;
    private boolean deleteIfNewerVersionsDeleted;
}
