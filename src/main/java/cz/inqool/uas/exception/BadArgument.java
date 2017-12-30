package cz.inqool.uas.exception;

import cz.inqool.uas.domain.DomainObject;

public class BadArgument extends GeneralException {
    private Object argument;

    public BadArgument() {
        super();
    }

    public BadArgument(Object argument) {
        super();
        this.argument = argument;
    }

    public BadArgument(Class<?> clazz, String objectId) {
        super();
        try {
            this.argument = clazz.newInstance();

            if (DomainObject.class.isAssignableFrom(clazz)) {
                ((DomainObject)this.argument).setId(objectId);
            }

        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String toString() {
        if (argument != null) {
            return "BadArgument{" +
                    "argument=" + argument +
                    '}';
        } else {
            return "BadArgument{argument not specified}";
        }
    }

    public Object getArgument() {
        return argument;
    }
}
