package cz.inqool.uas.error;

import cz.inqool.uas.store.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

@Service
public class ErrorService {
    private UserDetails user;

    private HttpServletRequest request;

    private ErrorStore store;

    @Transactional
    public void logError(String message, String stackTrace, String url, String userAgent) {
        String userId = null;
        if (user != null) {
            userId = user.getUsername();
        }

        Error error = new Error();
        error.setMessage(message);
        error.setStackTrace(stackTrace);
        error.setClientSide(true);
        error.setUserId(userId);

        if (request != null) {
            error.setUrl(url);
            error.setIp(request.getRemoteAddr());
            error.setUserAgent(userAgent);
        }

        store.save(error);
    }

    @Autowired(required = false)
    public void setUser(UserDetails user) {
        this.user = user;
    }

    @Autowired(required = false)
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Inject
    public void setStore(ErrorStore store) {
        this.store = store;
    }
}
