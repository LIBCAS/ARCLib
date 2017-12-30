package cz.inqool.uas.ws.log;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "uas_soap_conversation")
public class SoapMessage extends DatedObject {
    private String service;

    @Lob
    private String request;

    @Lob
    private String response;
}
