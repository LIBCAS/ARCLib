package cz.cas.lib.arclib.report;

import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.SerializationUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "arclib_report")
@Setter
@Getter
public class Report extends DomainObject implements Serializable {
    @Column(unique = true)
    private String name;

    @Column(columnDefinition = "LONGVARCHAR")
    private String template;
    @Column(length = 10485760)
    private byte[] compiled;
    /**
     * true if the report operates with ARCLib XML SOLR collection, false if it operates with ARCLib system database.. this is exclusive
     */
    private boolean arclibXmlDs;

    public Report() {
    }

    public Report(String id, String name, String template, byte[] compiled, boolean arclibXmlDs) {
        this.name = name;
        this.template = template;
        this.compiled = compiled;
        this.id = id;
        this.arclibXmlDs = arclibXmlDs;
    }

    public Report(String name, String template, Object compiled, boolean arclibXmlDs) {
        this.name = name;
        this.template = template;
        this.setCompiledObject(compiled);
        this.arclibXmlDs = arclibXmlDs;
    }

    public Object getCompiledObject() {
        return SerializationUtils.deserialize(compiled);
    }

    public void setCompiledObject(Object compiled) {
        this.compiled = SerializationUtils.serialize((Serializable) compiled);
    }
}
