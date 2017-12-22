package cz.inqool.uas.exception;

public class MissingObject extends GeneralException {
    private String type;

    private String id;

    public MissingObject(Class clazz, String id) {
        super(makeMessage(clazz.getTypeName(), id));
        this.type = clazz.getTypeName();
        this.id = id;
    }

    public MissingObject(String type, String id) {
        super(makeMessage(type, id));
        this.type = type;
        this.id = id;
    }

    @Override
    public String toString() {
        return makeMessage(type, id);
    }

    private static String makeMessage(String type, String id) {
        return "MissingObject{" +
                "type=" + type +
                ", id='" + id + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }
}
