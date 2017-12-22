package cz.inqool.uas.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@Configuration
public class SchedulingConfiguration {
    @Bean(destroyMethod="shutdown")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(42);
        taskScheduler.setRemoveOnCancelPolicy(true);
        return taskScheduler;
    }
}
