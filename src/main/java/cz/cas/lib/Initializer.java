package cz.cas.lib;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "cz.cas.lib")
@EntityScan(basePackages = "cz.cas.lib")
@EnableJms
@EnableAsync
@EnableScheduling
@EnableProcessApplication("arclib")
public class Initializer {

    public static void main(String[] args) {
        System.setProperty("javax.xml.transform.TransformerFactory", "cz.cas.lib.arclib.config.ArclibTransformerFactory");
        SpringApplication.run(Initializer.class, args);
    }
}
