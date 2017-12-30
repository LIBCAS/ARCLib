package cz.inqool.uas.ws.log;

import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;

import java.io.ByteArrayOutputStream;

@Slf4j
public class MessageInterceptor extends ClientInterceptorAdapter {
    private SoapMessageService messageService;

    private String serviceName;

    public MessageInterceptor(SoapMessageService store, String serviceName) {
        this.messageService = store;
        this.serviceName = serviceName;
    }

    @Override
    public void afterCompletion(MessageContext context, Exception ex) throws WebServiceClientException {

        WebServiceMessage request = context.getRequest();
        WebServiceMessage response = context.getResponse();

        SoapMessage message = new SoapMessage();
        message.setService(serviceName);
        message.setRequest(formatMessage(request));
        message.setResponse(formatMessage(response));
        messageService.save(message);
    }

    private String formatMessage(WebServiceMessage message) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            message.writeTo(stream);

            return stream.toString(Charsets.UTF_8.name());
        } catch (Exception ex) {
            log.error("Failed to log SOAP message.", ex);
            return "error";
        }
    }
}
