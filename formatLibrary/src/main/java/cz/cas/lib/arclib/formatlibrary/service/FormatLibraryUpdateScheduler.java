package cz.cas.lib.arclib.formatlibrary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import javax.inject.Inject;

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

    @Inject
    public void setFormatLibraryUpdater(FormatLibraryUpdater formatLibraryUpdater) {
        this.formatLibraryUpdater = formatLibraryUpdater;
    }

    @Inject
    public void setFormatLibraryUpdateCron(@Value("${formatLibrary.updateCron}") String formatLibraryUpdateCron) {
        this.formatLibraryUpdateCron = formatLibraryUpdateCron;
    }

    @Inject
    public void setScheduleFomratLibraryUpdate(@Value("${formatLibrary.scheduleUpdate}")
                                                       Boolean scheduleFormatLibraryUpdate) {
        this.scheduleFormatLibraryUpdate = scheduleFormatLibraryUpdate;
    }
}
