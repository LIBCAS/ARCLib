package cz.inqool.uas.api;

import cz.inqool.uas.rest.GeneralApi;
import cz.inqool.uas.rest.data.DataAdapter;
import lombok.Getter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/general")
public class GeneralApiImpl implements GeneralApi<GeneralEntity> {

    @Getter
    private DataAdapter<GeneralEntity> adapter;

    public void setAdapter(DataAdapter<GeneralEntity> adapter) {
        this.adapter = adapter;
    }
}
