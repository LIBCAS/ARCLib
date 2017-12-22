package cz.inqool.uas.exception;

import cz.inqool.uas.domain.DomainObject;

public class InvalidAttribute extends GeneralException {
    private DomainObject object;

    private String attribute;

    private Object value;

    public InvalidAttribute(DomainObject object, String attribute, Object value) {
        super();
        this.object = object;
        this.attribute = attribute;
        this.value = value;
    }

    @Override
    public String toString() {
        return "InvalidAttribute{" +
                "object=" + object +
                ", attribute='" + attribute + '\'' +
                ", value=" + value +
                '}';
    }

    public DomainObject getObject() {
        return object;
    }

    public String getAttribute() {
        return attribute;
    }

    public Object getValue() {
        return value;
    }
}
