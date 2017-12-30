package cz.cas.lib.arclib.api;

import cz.cas.lib.arclib.domain.SipProfile;
import cz.cas.lib.arclib.store.SipProfileStore;
import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.exception.BadArgument;
import cz.inqool.uas.exception.MissingObject;
import cz.inqool.uas.store.Transactional;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Collection;

import static cz.inqool.uas.util.Utils.eq;
import static cz.inqool.uas.util.Utils.notNull;

@RestController
@RequestMapping("/api/sip_profile")
public class SipProfileApi {
    @Getter
    private SipProfileStore store;

    /**
     * Saves an instance.
     *
     * <p>
     *     Specified id should correspond to {@link DomainObject#id} otherwise exception is thrown.
     * </p>
     * @param id Id of the instance
     * @param request Single instance
     * @return Single instance (possibly with computed attributes)
     * @throws BadArgument if specified id does not correspond to {@link DomainObject#id}
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Transactional
    SipProfile save(@PathVariable("id") String id, @RequestBody SipProfile request) {
        eq(id, request.getId(), () -> new BadArgument("id"));

        return store.save(request);
    }

    /**
     * Deletes an instance.
     *
     * @param id Id of the instance
     * @throws MissingObject if specified instance is not found
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Transactional
    void delete(@PathVariable("id") String id) {
        SipProfile entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        store.delete(entity);
    }

    /**
     * Gets one instance specified by id.
     *
     * @param id Id of the instance
     * @return Single instance
     * @throws MissingObject if instance does not exists
     */

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Transactional
    SipProfile get(@PathVariable("id") String id) {
        SipProfile entity = store.find(id);
        notNull(entity, () -> new MissingObject(store.getType(), id));

        return entity;
    }

    /**
     * Gets all instances
     *
     * @return All instances
     */
    @RequestMapping(method = RequestMethod.GET)
    @Transactional
    Collection<SipProfile> list() {
        return store.findAll();
    }

    @Inject
    public void setStore(SipProfileStore store) {
        this.store = store;
    }
}
