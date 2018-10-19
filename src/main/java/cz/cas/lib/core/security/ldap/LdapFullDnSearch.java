package cz.cas.lib.core.security.ldap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;

@Slf4j
public class LdapFullDnSearch extends UasLdapUserSearch {

    public LdapFullDnSearch(BaseLdapPathContextSource contextSource) {
        super(null, contextSource);
    }

    /**
     * Return the DirContextOperations containing the user's information
     *
     * @param dn full DN of the user to search for.
     * @return An DirContextOperations object containing the details of the located user's
     * directory entry
     * @throws UsernameNotFoundException if no matching entry is found.
     */
    @Override
    public DirContextOperations searchForUser(String dn) {
        log.debug("Searching for dn '{}'.", dn);

        SpringSecurityLdapTemplate template = new SpringSecurityLdapTemplate(getContextSource());

        template.setSearchControls(getSearchControls());

        try {
            return template.retrieveEntry(dn, null);
        } catch (IncorrectResultSizeDataAccessException ex) {
            if (ex.getActualSize() == 0) {
                throw new UsernameNotFoundException("User " + dn + " not found in directory.");
            }
            // Search should never return multiple results if properly configured, so just rethrow
            throw ex;
        }
    }
}
