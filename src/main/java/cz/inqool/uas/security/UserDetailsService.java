package cz.inqool.uas.security;

import java.util.List;

public interface UserDetailsService extends org.springframework.security.core.userdetails.UserDetailsService {
    UserDetails loadUserById(String id);

    default List<UserDetails> loadUsersWithPermission(String permission) {
        throw new UnsupportedOperationException();
    }
}
