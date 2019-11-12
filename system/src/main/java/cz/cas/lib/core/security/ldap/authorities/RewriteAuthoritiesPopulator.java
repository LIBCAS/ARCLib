package cz.cas.lib.core.security.ldap.authorities;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import cz.cas.lib.arclib.domainbase.exception.MissingObject;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.notNull;

/**
 * Adapter rewriting LDAP authorities according to specified lookup table.
 */
public class RewriteAuthoritiesPopulator implements UasAuthoritiesPopulator {
    private UasAuthoritiesPopulator delegate;

    //These two are inverse of each other and must remain in sync
    private ImmutableMap<String, String> authorityToPermissionMap;
    private ImmutableMultimap<String, String> permissionToAuthoritiesMap;

    public RewriteAuthoritiesPopulator(UasAuthoritiesPopulator delegate, Map<String, String> permissions) {
        this.delegate = delegate;
        this.authorityToPermissionMap = ImmutableMap.copyOf(permissions);

        ImmutableMultimap.Builder<String, String> multimapBuilder = ImmutableMultimap.builder();
        for (Map.Entry<String, String> entry : permissions.entrySet()) {
            multimapBuilder.put(entry.getValue(), entry.getKey());
        }
        this.permissionToAuthoritiesMap = multimapBuilder.build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
        Collection<? extends GrantedAuthority> authorities = delegate.getGrantedAuthorities(userData, username);

        if (authorities != null) {
            return authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> authorityToPermissionMap.get(a))
                    .filter(Objects::nonNull)
                    .filter(a -> !a.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        } else {
            return null;
        }
    }

    @Override
    public Set<String> getUserNamesWithAuthority(String authority) {
        Set<String> userNamesWithAuthority = new HashSet<>();
        ImmutableCollection<String> mappedAuthorities = permissionToAuthoritiesMap.get(authority);
        for (String mappedAuthority : mappedAuthorities) {
            notNull(mappedAuthority, () -> new MissingObject(GrantedAuthority.class, mappedAuthority));
            userNamesWithAuthority.addAll(delegate.getUserNamesWithAuthority(mappedAuthority));
        }
        return userNamesWithAuthority;
    }
}
