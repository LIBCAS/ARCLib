package cz.inqool.uas.ws.log;

import cz.inqool.uas.store.TransactionalNew;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class SoapMessageService {
    private SoapMessageStore store;

    @TransactionalNew
    public SoapMessage save(SoapMessage message) {
        return store.save(message);
    }

    @Inject
    public void setStore(SoapMessageStore store) {
        this.store = store;
    }
}
