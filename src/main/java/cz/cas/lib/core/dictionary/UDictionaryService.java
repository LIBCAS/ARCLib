package cz.cas.lib.core.dictionary;

import cz.cas.lib.core.exception.BadArgument;
import cz.cas.lib.core.rest.data.DictionaryDelegateAdapter;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Objects;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@Service
public class UDictionaryService implements DictionaryDelegateAdapter<Dictionary> {
    @Getter
    private UDictionaryStore delegate;

    /**
     * Gets Dictionary by code
     *
     * @param code code of Dictionary
     * @return Dictionary
     */
    public Dictionary findByCode(String code) {
        return delegate.findByCode(code);
    }

    /**
     * Adds tests if there is no cycle in parent hierarchy
     *
     * @param entity entity to save
     * @return saved entity
     * @throws BadArgument thrown if parent not found or will create cycle
     */
    @Override
    public Dictionary save(Dictionary entity) {
        Dictionary parentDto = entity.getParent();
        if (parentDto != null) {
            Dictionary parent = delegate.find(parentDto.getId());
            notNull(parent, () -> new BadArgument("parent"));

            boolean hasCycle = eqOrParentEq(parent, entity);
            eq(hasCycle, false, () -> new BadArgument("parent"));
        }

        return delegate.save(entity);
    }

    private boolean eqOrParentEq(Dictionary dictionary, Dictionary possibleParent) {
        if (Objects.equals(dictionary, possibleParent)) {
            return true;
        } else if (dictionary.getParent() != null) {
            return eqOrParentEq(dictionary.getParent(), possibleParent);
        } else {
            return false;
        }
    }

    @Inject
    public void setDelegate(UDictionaryStore delegate) {
        this.delegate = delegate;
    }
}
