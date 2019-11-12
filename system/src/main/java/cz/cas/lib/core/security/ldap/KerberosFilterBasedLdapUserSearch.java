package cz.cas.lib.core.security.ldap;

import cz.cas.lib.arclib.domainbase.exception.BadArgument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cz.cas.lib.core.util.Utils.eq;
import static cz.cas.lib.core.util.Utils.notNull;

@Slf4j
public class KerberosFilterBasedLdapUserSearch extends UasLdapUserSearch {

    public KerberosFilterBasedLdapUserSearch(String searchFilter, BaseLdapPathContextSource contextSource) {
        super(searchFilter, contextSource);
        notNull(searchFilter, () -> new BadArgument("searchFilter must not be null."));
    }

    /**
     * Return the DirContextOperations containing the user's information
     *
     * @param username the kerberos username to search for.
     * @return An DirContextOperations object containing the details of the located user's
     * directory entry
     * @throws UsernameNotFoundException if no matching entry is found.
     */
    public DirContextOperations searchForUser(String username) {
        String[] data = username.split("@");
        eq(data.length, 2, () -> new UsernameNotFoundException("Wrong username format for " + username));

        String accountName = data[0];
        String domainName = data[1];

        String[] domainData = domainName.split("\\.");

        String searchBase = Stream.of(domainData)
                .map(part -> "dc=" + part)
                .collect(Collectors.joining(","));

        log.debug("Searching for user '{}' in '{}', with user search {}.", accountName, searchBase, this);


        SpringSecurityLdapTemplate template = new SpringSecurityLdapTemplate(getContextSource());

        template.setSearchControls(getSearchControls());

        try {
            return template.searchForSingleEntry(searchBase, getSearchFilter(), new String[]{accountName});

        } catch (IncorrectResultSizeDataAccessException ex) {
            if (ex.getActualSize() == 0) {
                throw new UsernameNotFoundException("User " + username + " not found in directory.");
            }
            // Search should never return multiple results if properly configured, so just rethrow
            throw ex;
        }
    }
}
