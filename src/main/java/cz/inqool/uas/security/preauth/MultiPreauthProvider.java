package cz.inqool.uas.security.preauth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.List;
import java.util.function.Predicate;

/**
 * Groups multiple Preauth providers together and use testing methods to select the proper one
 * during loadUserDetails
 */
public class MultiPreauthProvider implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
    private List<Mapper> mappers;

    public MultiPreauthProvider(List<Mapper> mappers) {
        this.mappers = mappers;
    }

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        Mapper mapper = mappers.stream()
                               .filter(p -> p.getTester().test(token))
                               .findFirst()
                               .orElse(null);

        if (mapper != null) {
            String username = (String) token.getPrincipal();
            return mapper.getDetailsService().loadUserByUsername(username);
        } else {
            return null;
        }
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class Mapper {
        private Predicate<PreAuthenticatedAuthenticationToken> tester;
        private UserDetailsService detailsService;
    }
}
