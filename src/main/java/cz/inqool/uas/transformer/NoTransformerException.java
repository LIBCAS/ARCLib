package cz.inqool.uas.transformer;

import cz.inqool.uas.exception.GeneralException;

public class NoTransformerException extends GeneralException {

    public NoTransformerException(String inType, String outType) {
        super("No transformer from '{1}' to '{2}'".replace("{1}", inType).replace("{2}", outType));
    }

}
