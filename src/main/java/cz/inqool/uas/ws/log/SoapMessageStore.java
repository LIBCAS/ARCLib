package cz.inqool.uas.ws.log;

import cz.inqool.uas.store.DatedStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * Implementation of {@link DatedStore} for storing {@link SoapMessage}.
 *
 */
@Slf4j
@Repository
public class SoapMessageStore extends DatedStore<SoapMessage, QSoapMessage> {

    public SoapMessageStore() {
        super(SoapMessage.class, QSoapMessage.class);
    }
}
