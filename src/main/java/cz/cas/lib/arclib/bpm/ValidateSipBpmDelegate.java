package cz.cas.lib.arclib.bpm;

import cz.cas.lib.arclib.validation.ValidationService;
import cz.inqool.uas.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

@Slf4j
@Component
public class ValidateSipBpmDelegate implements JavaDelegate {

    protected ValidationService service;

    /**
     * Executes the validation process for the given SIP:
     * 1. copies SIP to workspace
     * 2. validates SIP
     * 3. deletes SIP from workspace
     *
     * @param execution parameter containing the SIP id
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    @Transactional
    @Override
    public void execute(DelegateExecution execution) throws IOException, ParserConfigurationException,
            SAXException, XPathExpressionException {
        String validationProfileId = (String) execution.getVariable("validationProfileId");
        String pathToSip = (String) execution.getVariable("pathToSip");

        log.info("BPM process for SIP at path " + pathToSip + " started.");

        service.validateSip(pathToSip.substring(pathToSip.lastIndexOf("/") + 1, pathToSip.length()),
                pathToSip, validationProfileId);
    }

    @Inject
    public void setService(ValidationService service) {
        this.service = service;
    }
}
