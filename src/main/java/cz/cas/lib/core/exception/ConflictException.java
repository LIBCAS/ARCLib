package cz.cas.lib.core.exception;

import cz.cas.lib.core.domain.DomainObject;

public class ConflictException extends GeneralException {
    private Object object;
    private String msg;

    public ConflictException() {
        super();
    }

    public ConflictException(Object object) {
        super();
        this.object = object;
    }

    public ConflictException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public ConflictException(Class<?> clazz, String objectId) {
        super();
        try {
            this.object = clazz.newInstance();

            if (DomainObject.class.isAssignableFrom(clazz)) {
                ((DomainObject) this.object).setId(objectId);
            }

        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String toString() {
        if (msg != null)
            return msg;
        if (object != null) {
            return "ConflictException{" +
                    "object=" + object +
                    '}';
        } else {
            return "ConflictException{id not specified}";
        }
    }

    public Object getObject() {
        return object;
    }
}
