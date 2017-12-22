package cz.inqool.uas.action;

import cz.inqool.uas.rest.data.DictionaryDelegateAdapter;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Action management service
 */
@Service
public class ActionService implements DictionaryDelegateAdapter<Action> {
    @Getter
    private ActionStore delegate;

    @Inject
    public void setDelegate(ActionStore delegate) {
        this.delegate = delegate;
    }

    /**
     * Gets Action by code
     * @param code code of action
     * @return Action
     */
    public Action findByCode(String code) {
        return delegate.findByCode(code);
    }
}
