package cz.cas.lib.arclib.security.user;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import cz.cas.lib.arclib.security.authorization.data.UserRole;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserDetailsImpl implements UserDetails {

    @NotNull
    private User user;

    // Basic YXJjLWJvYjphdXJhMjRncm9ncw==
    // TODO: vytvorit permissions pre vsetky moduly, priradit permissions nad API, upravit testy
    // TODO: odstranit stare Roles, assignedrole, servicy, endpointy atd
    // TODO zmenit tu metodu ktora sa odkazuje na Roles.SUPER_ADMIN atd na Permissions
    public UserDetailsImpl(@NonNull User user, Collection<? extends GrantedAuthority> ignored) {
        this.user = user;
    }

    public UserDetailsImpl(@NonNull User user) {
        this.user = user;
    }


    /**
     * Method returns collection of authorities which the annotation
     * {@code @PreAuthorize("hasAuthority('" + MY_DECLARED_PERMISSION + "')")} validates against.
     *
     * User roles and permissions are stored in structure like this:
     * <pre>
     *     User --- Role1 --- Permission A, Permission B, Permission C
     *          \-- Role2 --- Permission B
     *          \-- Role3 --- Permission C, Permission D
     *
     *     Resulting permissions: A, B, C, D
     * </pre>
     *
     * @return Collection of Authorities, in ARClib used mainly by @PreAuthorize annotation
     * @see User#getRoles()
     * @see UserRole#getPermissions()
     */
    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return user.jointPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public String getProducerId() {
        Producer p = user.getProducer();
        if (p == null)
            return null;
        if (p instanceof HibernateProxy)
            return (String) ((HibernateProxy) p).getHibernateLazyInitializer().getIdentifier();
        return p.getId();
    }

    @Override
    public String getId() {
        return user.getId();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

}
