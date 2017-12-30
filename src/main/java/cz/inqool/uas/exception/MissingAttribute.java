package cz.inqool.uas.exception;

import cz.inqool.uas.domain.DomainObject;

public class MissingAttribute extends GeneralException {
    private Object object;

    private String attribute;

    public MissingAttribute(Object object, String attribute) {
        super();
        this.object = object;
        this.attribute = attribute;
    }

    public MissingAttribute(Class<?> clazz, String objectId, String attribute) {
        super();
        try {
            this.object = clazz.newInstance();

            if (DomainObject.class.isAssignableFrom(clazz)) {
                ((DomainObject)this.object).setId(objectId);
            }

        } catch (Exception e) {
            // ignore
        }


        this.attribute = attribute;
    }

    @Override
    public String toString() {
        return "MissingAttribute{" +
                "object=" + object +
                ", attribute='" + attribute + '\'' +
                '}';
    }

    public Object getObject() {
        return object;
    }

    public String getAttribute() {
        return attribute;
    }
}
