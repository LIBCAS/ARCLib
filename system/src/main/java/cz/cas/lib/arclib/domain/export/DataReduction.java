package cz.cas.lib.arclib.domain.export;

import cz.cas.lib.arclib.domainbase.util.ArrayJsonConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DataReduction {

    @Convert(converter = ArrayJsonConverter.class)
    @Column(name = "data_reduction_regexes")
    @NotEmpty
    private List<String> regexes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "data_reduction_mode")
    @NotNull
    private DataReductionMode mode;
}
