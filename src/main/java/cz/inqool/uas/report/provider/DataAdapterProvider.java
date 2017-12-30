package cz.inqool.uas.report.provider;

import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.index.dto.Result;
import cz.inqool.uas.report.exception.GeneratingException;
import cz.inqool.uas.rest.GeneralApi;
import cz.inqool.uas.rest.data.DataAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.Map;

import static cz.inqool.uas.util.Utils.asMap;

/**
 * Report provider working with {@link Params} object supplied from user.
 *
 * <p>
 *     This {@link ReportProvider} is mainly used when generating report of entries shown through
 *     {@link GeneralApi#list(Params)} or when the entries can be specified through {@link Params}.
 * </p>
 */
@Slf4j
@Service
public class DataAdapterProvider extends BaseProvider<DataAdapterProvider.Input> implements ApplicationContextAware {
    private ApplicationContext context;

    public DataAdapterProvider() {
        super(Input.class);
    }

    @Override
    public String getName() {
        return "Data adapter provider";
    }

    @Override
    public Map<String, Object> provide(Input input) {
        try {
            if (input.getAdapter() != null && input.getParams() != null) {
                Class adapterClass = Class.forName(input.adapter);

                if (DataAdapter.class.isAssignableFrom(adapterClass)) {
                    DataAdapter adapter = (DataAdapter) context.getBean(adapterClass);

                    Result result = adapter.findAll(input.params);
                    return asMap("result", result);
                } else {
                    log.error("Specified class '{}' is not DataAdapter.", input.adapter);
                }
            } else {
                log.error("Store or params not provided.");
            }

        } catch (ClassNotFoundException e) {
            throw new GeneratingException("Failed to parse parameters.", e);
        }

        throw new GeneratingException("Bad store specified.");
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Getter
    @Setter
    public static class Input {
        private String adapter;
        private Params params;
    }
}
