package cz.inqool.uas.report.index;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "test_report_indexed")
public class ReportTestEntity extends DatedObject {
    private String stringAttribute;
}
