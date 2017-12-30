package cz.inqool.uas.bpm.config.form;

import org.camunda.bpm.engine.impl.form.type.SimpleFormFieldType;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

/**
 * Java 8 Arbitrary object type representation for Camunda
 *
 */
public class ObjectFormType extends SimpleFormFieldType {
    public final static String TYPE_NAME = "object";

    /**
     * Converts the value from all supported types to the internal one
     * @param propertyValue value with one type
     * @return value with internal type
     */
    @Override
    protected TypedValue convertValue(TypedValue propertyValue) {
        if(propertyValue instanceof ObjectValue) {
            return propertyValue;
        }
        else {
            Object value = propertyValue.getValue();
            return Variables.objectValue(value).create();
        }
    }

    /**
     * Gets the name for the form type.
     */
    @Override
    public String getName() {
        return TYPE_NAME;
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
