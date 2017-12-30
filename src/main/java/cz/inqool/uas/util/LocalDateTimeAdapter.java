package cz.inqool.uas.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;

public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

    @Override
    public LocalDateTime unmarshal(String inputDate) throws Exception {
        return LocalDateTime.parse(inputDate);
    }

    @Override
    public String marshal(LocalDateTime inputDate) throws Exception {
        return inputDate.toString();
    }

}
