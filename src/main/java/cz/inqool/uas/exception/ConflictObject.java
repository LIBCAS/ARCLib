package cz.inqool.uas.exception;

import cz.inqool.uas.domain.DomainObject;

public class ConflictObject extends GeneralException {
    private Object object;

    public ConflictObject() {
        super();
    }

    public ConflictObject(Object object) {
        super();
        this.object = object;
    }

    public ConflictObject(Class<?> clazz, String objectId) {
        super();
        try {
            this.object = clazz.newInstance();

            if (DomainObject.class.isAssignableFrom(clazz)) {
                ((DomainObject)this.object).setId(objectId);
            }

        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String toString() {
        if (object != null) {
            return "ConflictObject{" +
                    "object=" + object +
                    '}';
        } else {
            return "ConflictObject{id not specified}";
        }
    }

    public Object getObject() {
        return object;
    }
}
