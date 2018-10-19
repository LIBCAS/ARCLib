package cz.cas.lib.core.version;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@RequestMapping("/api/version")
public class VersionApi {
    private String version;

    private String date;

    @RequestMapping(method = RequestMethod.GET)
    public String getVersion() {
        if (date != null) {
            return version + " (" + date + ")";
        } else {
            return version;
        }
    }

    @Inject
    public void setVersion(@Value("${build.version:'undefined'}") String version) {
        this.version = version;
    }

    @Inject
    public void setDate(@Value("${build.date:null}") String date) {
        this.date = date;
    }
}
