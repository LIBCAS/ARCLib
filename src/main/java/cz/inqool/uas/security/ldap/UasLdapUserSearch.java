package cz.inqool.uas.security.ldap;

import cz.inqool.uas.exception.BadArgument;
import lombok.Getter;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.ldap.search.LdapUserSearch;

import javax.naming.directory.SearchControls;

import static cz.inqool.uas.util.Utils.notNull;

@Getter
public abstract class UasLdapUserSearch implements LdapUserSearch {
    private final ContextSource contextSource;

    /**
     * The LDAP SearchControls object used for the search. Shared between searches so
     * shouldn't be modified once the bean has been configured.
     */
    private final SearchControls searchControls = new SearchControls();

    /**
     * The filter expression used in the user search. This is an LDAP search filter (as
     * defined in 'RFC 2254') with optional arguments. See the documentation for the
     * <tt>search</tt> methods in {@link javax.naming.directory.DirContext DirContext} for
     * more information.
     *
     * <p>
     * In this case, the username is the only parameter.
     * </p>
     * Possible examples are:
     * <ul>
     * <li>(uid={0}) - this would search for a username match on the uid attribute.</li>
     * </ul>
     */
    private final String searchFilter;

    public UasLdapUserSearch(String searchFilter, BaseLdapPathContextSource contextSource) {
        notNull(contextSource, () -> new BadArgument("contextSource must not be null"));

        this.searchFilter = searchFilter;
        this.contextSource = contextSource;
    }

    public void setSearchScope(int scope) {
        searchControls.setSearchScope(scope);
    }
}
