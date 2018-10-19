package cz.cas.lib.arclib.bpm.delegate;

import cz.cas.lib.arclib.bpm.ArclibDelegate;
import cz.cas.lib.core.store.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestDelegate extends ArclibDelegate implements JavaDelegate {
    @Transactional
    @Override
    public void execute(DelegateExecution execution) throws InterruptedException {
        //simulate the process of ingestion
        log.info("Waiting for two seconds to simulate the process of SIP ingestion...");
        Thread.sleep(2000);
    }
}
