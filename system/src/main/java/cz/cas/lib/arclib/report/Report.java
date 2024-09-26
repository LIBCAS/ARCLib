package cz.cas.lib.arclib.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.arclib.domainbase.domain.DomainObject;
import cz.cas.lib.arclib.index.autocomplete.AutoCompleteAware;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.SerializationUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "arclib_report")
@Setter
@Getter
public class Report extends DomainObject implements Serializable, AutoCompleteAware {

    @Column(updatable = false)
    @CreationTimestamp
    protected Instant created;

    @UpdateTimestamp
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

    @Override
    public String getAutoCompleteLabel() {
        return name;
    }
}
