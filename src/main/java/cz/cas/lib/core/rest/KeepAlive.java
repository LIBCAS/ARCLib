package cz.cas.lib.core.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Keepalive functionality for client.
 *
 * <p>
 * This Api is intended for authorized usage for automatic token renewal. Should not be used for loadbalancing
 * health check.
 * </p>
 */
@RestController
@RequestMapping("/api/keepalive")
public class KeepAlive {
    /**
     * Simply return true if called.
     *
     * @return true
     */
    @RequestMapping(name = "/", method = RequestMethod.GET)
    public boolean get() {
        return true;
    }
}
