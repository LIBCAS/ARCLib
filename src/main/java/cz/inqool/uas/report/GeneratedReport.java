package cz.inqool.uas.report;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class GeneratedReport {
    protected byte[] content;
    protected String contentType;
    protected String name;
}
