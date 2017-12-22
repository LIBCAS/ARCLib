package cz.inqool.uas.error;

import cz.inqool.uas.domain.DatedObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "uas_error")
public class Error extends DatedObject {
    @Lob
    private String message;

    @Lob
    private String stackTrace;

    private Boolean clientSide;

    private String userId;

    private String ip;

    private String url;

    private String userAgent;
}
