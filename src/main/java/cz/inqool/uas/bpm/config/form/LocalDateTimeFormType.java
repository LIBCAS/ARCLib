package cz.inqool.uas.bpm.config.form;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.form.type.SimpleFormFieldType;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.TypedValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;


/**
 * Java 8 LocalDateTime type representation for Camunda
 *
 */
public class LocalDateTimeFormType extends SimpleFormFieldType {

    public final static String TYPE_NAME = "localDateTime";


    /**
     * Gets the name for the form type.
     */
    public String getName() {
        return TYPE_NAME;
    }

    /**
     * Converts the value from all supported types to the internal one
     * @param propertyValue value with one type
     * @return value with internal type
     */
    protected TypedValue convertValue(TypedValue propertyValue) {
        Object value = propertyValue.getValue();
        if(value == null) {
            return Variables.objectValue(null).create();
        }
        else if(value instanceof LocalDateTime) {
            return Variables.objectValue(value).create();
        }
        else if(value instanceof String) {
            try {
                return Variables.objectValue(LocalDateTime.parse((String) value)).create();
            } catch (DateTimeParseException e) {
                throw new ProcessEngineException("Could not parse value '"+value+"' as LocalDateTime.");
            }
        }
        else {
            throw new ProcessEngineException("Value '"+value+"' cannot be transformed into a LocalDateTime.");
        }
    }

    @Deprecated
    @Override
    public Object convertFormValueToModelValue(Object propertyValue) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public String convertModelValueToFormValue(Object modelValue) {
        throw new UnsupportedOperationException();
    }

}
