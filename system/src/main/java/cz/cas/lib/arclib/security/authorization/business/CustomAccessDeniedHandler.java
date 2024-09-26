package cz.cas.lib.arclib.security.authorization.business;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

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

    @Autowired
    public void setDispatcherServlet(DispatcherServlet dispatcherServlet) {
        this.dispatcherServlet = dispatcherServlet;
    }
}
