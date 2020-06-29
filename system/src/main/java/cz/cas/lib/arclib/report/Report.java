package cz.cas.lib.arclib.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.arclib.domainbase.store.InstantGenerator;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.SerializationUtils;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "arclib_report")
@Setter
@Getter
public class Report extends DomainObject implements Serializable {

    @Column(updatable = false)
    @GeneratorType(type = InstantGenerator.class, when = GenerationTime.INSERT)
    protected Instant created;

    @GeneratorType(type = InstantGenerator.class, when = GenerationTime.ALWAYS)
    protected Instant updated;

    @Column(unique = true)
    private String name;

    @Column(columnDefinition = "LONGVARCHAR")
    private String template;
    @Column(length = 10485760)
    @JsonIgnore
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

    @JsonIgnore
    public Object getCompiledObject() {
        return SerializationUtils.deserialize(compiled);
    }

    public void setCompiledObject(Object compiled) {
        this.compiled = SerializationUtils.serialize((Serializable) compiled);
    }
}
