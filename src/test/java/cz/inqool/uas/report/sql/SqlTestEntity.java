package cz.inqool.uas.report.sql;

import cz.inqool.uas.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "test_sql")
public class SqlTestEntity extends DomainObject {
    private String test;
}
