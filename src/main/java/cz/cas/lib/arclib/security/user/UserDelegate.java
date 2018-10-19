package cz.cas.lib.arclib.security.user;

import cz.cas.lib.arclib.domain.Producer;
import cz.cas.lib.arclib.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
public class UserDelegate implements UserDetails {
    @Getter
    @Setter
    private User user;
    @Setter
    private Set<GrantedAuthority> authorities = new HashSet<>();

    public UserDelegate(User user, Collection<? extends GrantedAuthority> additionalAuthorities) {
        this.user = user;
        if (additionalAuthorities != null) {
            authorities.addAll(additionalAuthorities);
        }
    }

    public UserDelegate(User user) {
        this.user = user;
    }

    @Override
    public String getProducerId() {
        if (this.user != null) {
            Producer p = user.getProducer();
            if (p == null)
                return null;
            if (p instanceof HibernateProxy)
                return (String) ((HibernateProxy) p).getHibernateLazyInitializer().getIdentifier();
            return p.getId();
        }
        return null;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getId() {
        return user.getId();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
