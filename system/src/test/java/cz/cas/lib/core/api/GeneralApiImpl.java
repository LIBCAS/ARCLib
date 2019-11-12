package cz.cas.lib.core.api;

import cz.cas.lib.core.rest.GeneralApi;
import cz.cas.lib.core.rest.data.DataAdapter;
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
