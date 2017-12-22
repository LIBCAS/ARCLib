package cz.inqool.uas.report.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

/**
 * Report provider which only passes the specified input parameters
 *
 * <p>
 *     This {@link ReportProvider} is used when no data from server needs to be gathered.
 * </p>
 */
@Slf4j
@Service
public class PassthroughProvider extends BaseProvider<PassthroughProvider.Input> {
    public PassthroughProvider() {
        super(Input.class);
    }

    @Override
    public String getName() {
        return "Pass-through provider";
    }

    @Override
    public Map<String, Object> provide(Input input) {
        return Collections.emptyMap();
    }

    @Getter
    @Setter
    public static class Input {
    }
}
