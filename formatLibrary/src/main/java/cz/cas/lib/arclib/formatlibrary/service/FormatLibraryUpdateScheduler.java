package cz.cas.lib.arclib.formatlibrary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;



@Configuration
@Slf4j
public class FormatLibraryUpdateScheduler implements SchedulingConfigurer {

    private String formatLibraryUpdateCron;
    private FormatLibraryUpdater formatLibraryUpdater;
    private Boolean scheduleFormatLibraryUpdate;


    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (scheduleFormatLibraryUpdate) {
            taskRegistrar.addTriggerTask(formatLibraryUpdater::updateFormatsFromExternal, new CronTrigger(formatLibraryUpdateCron));
        }
    }

    @Autowired
    public void setFormatLibraryUpdater(FormatLibraryUpdater formatLibraryUpdater) {
        this.formatLibraryUpdater = formatLibraryUpdater;
    }

    @Autowired
    public void setFormatLibraryUpdateCron(@Value("${formatLibrary.updateCron}") String formatLibraryUpdateCron) {
        this.formatLibraryUpdateCron = formatLibraryUpdateCron;
    }

    @Autowired
    public void setScheduleFomratLibraryUpdate(@Value("${formatLibrary.scheduleUpdate}")
                                                       Boolean scheduleFormatLibraryUpdate) {
        this.scheduleFormatLibraryUpdate = scheduleFormatLibraryUpdate;
    }
}
