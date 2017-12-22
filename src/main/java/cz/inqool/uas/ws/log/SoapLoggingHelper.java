package cz.inqool.uas.ws.log;

import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import javax.inject.Inject;

import static cz.inqool.uas.util.Utils.asArray;

/**
 * Helps setup logging of soap messages
 *
 */
@Service
public class SoapLoggingHelper {
    private SoapMessageService service;

    /**
     * Adds the message interceptor for soap message logging.
     * @param ws Webservice class
     * @param serviceName name of the service
     */
    public void apply(WebServiceGatewaySupport ws, String serviceName) {
        MessageInterceptor interceptor = new MessageInterceptor(service, serviceName);

        ws.setInterceptors(asArray(interceptor));
    }

    @Inject
    public void setService(SoapMessageService service) {
        this.service = service;
    }
}
