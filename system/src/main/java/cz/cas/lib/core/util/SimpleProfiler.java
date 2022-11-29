package cz.cas.lib.core.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * When you want to debug how long certain operations take.
 * Put start and stop calls with equal timer name around blocks you wanna measure.
 * Dump() to get results and reset timers.
 *
 * @author Lukas Jane (inQool) 12.08.2019.
 */
@Slf4j
public class SimpleProfiler {
    private static Map<String, Long> cummulativeTimes = new HashMap<>();
    private static Map<String, Long> counts = new HashMap<>();
    private static Map<String, Long> lastStartTimes = new HashMap<>();

    public static void start(String timerName) {
        lastStartTimes.put(Thread.currentThread().getId() + timerName, System.currentTimeMillis());
    }

    public static synchronized void end(String timerName) {
        Long timeStarted = lastStartTimes.get(Thread.currentThread().getId() + timerName);
        if (timeStarted == null) {
            log.warn("WARN: Timer " + timerName + " not started.");
            return;
        }
        long timeSpent = System.currentTimeMillis() - timeStarted;
        cummulativeTimes.merge(timerName, timeSpent, Long::sum);
        counts.merge(timerName, 1L, Long::sum);
    }

    public static String dump() {
        String retVal = "SimpleProfiler dump:\n";
        for (String key : cummulativeTimes.keySet().stream().sorted().collect(Collectors.toList())) {
            retVal += "    Timer \"" + key + "\": " + counts.get(key) + "x, sum " + formatTime(cummulativeTimes.get(key)) + ", avg " + cummulativeTimes.get(key) / counts.get(key) + " ms\n";
        }
        log.info(retVal);
        wipe();
        return retVal;  //for cases when output other than stdout is desired
    }

    private static String formatTime(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        }
        long s = ms / 1000;
        if (s < 100) {
            return s + " s";
        }
        long m = s / 60;
        s = s % 60;
        if (m < 60) {
            return m + " m " + s + " s";
        }
        long h = m / 60;
        m = m % 60;
        return h + " h " + m + " m " + s + " s";
    }

    private static void wipe() {
        cummulativeTimes = new HashMap<>();
        counts = new HashMap<>();
        lastStartTimes = new HashMap<>();
    }
}
