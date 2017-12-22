package cz.inqool.uas.exception;

import cz.inqool.uas.domain.DomainObject;

public class ForbiddenObject extends GeneralException {
    private Object object;

    public ForbiddenObject(Object object) {
        super();
        this.object = object;
    }

    public ForbiddenObject(Class<?> clazz, String objectId) {
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
        return "ForbiddenObject{" +
                "object=" + object +
                '}';
    }

    public Object getObject() {
        return object;
    }
}
