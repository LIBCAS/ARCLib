package cz.inqool.uas.report.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inqool.uas.report.exception.GeneratingException;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

/**
 * Base implementation for all report providers.
 *
 * <p>
 *     Main goals of this class:
 * </p>
 * <ul>
 *     <li>Abstract user from implementing parameters deserialization from JSON</li>
 *     <li>Combining input and output parameters</li>
 * </ul>
 * @param <T> type of input data
 */
@Slf4j
public abstract class BaseProvider<T> implements ReportProvider {

    private ObjectMapper objectMapper;

    private Class<T> inputClass;

    public BaseProvider(Class<T> inputClass) {
        this.inputClass = inputClass;
    }

    /**
     * Provides data acorrding to input parameters.
     *
     * @param input Converted input parameters
     * @return Output parameters
     */
    protected abstract Map<String, Object> provide(T input);

    @Override
    public Map<String, Object> provide(Map<String, Object> inputMap) {
        try {
            T input = objectMapper.convertValue(inputMap, inputClass);

            if (input != null) {
                Map<String, Object> outputMap = provide(input);

                Map<String, Object> result = new HashMap<>();
                result.putAll(inputMap);
                result.putAll(outputMap);

                return result;
            } else {
                log.error("Insufficient parameters provided.");
            }

        } catch (IllegalArgumentException e) {
            throw new GeneratingException("Failed to parse parameters.", e);
        }

        throw new GeneratingException("Bad store specified.");
    }

    @Inject
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
