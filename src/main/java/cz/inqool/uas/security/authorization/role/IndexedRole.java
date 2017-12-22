package cz.inqool.uas.security.authorization.role;

import cz.inqool.uas.index.IndexedDatedObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

@Getter
@Setter
@Setting(settingPath = "/es_settings.json")
@Document( indexName = "uas" , type = "role")
public class IndexedRole extends IndexedDatedObject {

    @Field(type = FieldType.String, analyzer = "folding")
    private String name;
}
