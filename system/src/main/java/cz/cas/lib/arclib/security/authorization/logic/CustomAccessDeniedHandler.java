package cz.cas.lib.arclib.security.authorization.logic;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private DispatcherServlet dispatcherServlet;

    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException,
            ServletException {
        if (!response.isCommitted()) {
            List<HandlerMapping> handlerMappings = dispatcherServlet.getHandlerMappings();
            if (handlerMappings != null) {
                HandlerExecutionChain handler = null;
                for (HandlerMapping handlerMapping : handlerMappings) {
                    try {
                        handler = handlerMapping.getHandler(request);
                    } catch (Exception ignored) {}
                    if (handler != null)
                        break;
                }
                if (handler != null && handler.getHandler() instanceof HandlerMethod) {
                    HandlerMethod method = (HandlerMethod) handler.getHandler();
                    PreAuthorize methodAnnotation = method.getMethodAnnotation(PreAuthorize.class);
                    if (methodAnnotation != null) {
                        response.sendError(HttpStatus.FORBIDDEN.value(),
                                "Authorization condition not met: " + methodAnnotation.value());
                        return;
                    }
                }
            }
            response.sendError(HttpStatus.FORBIDDEN.value(),
                    HttpStatus.FORBIDDEN.getReasonPhrase());
        }
    }

    @Inject
    public void setDispatcherServlet(DispatcherServlet dispatcherServlet) {
        this.dispatcherServlet = dispatcherServlet;
    }
}
