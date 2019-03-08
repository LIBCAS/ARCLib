package cz.cas.lib.arclib.index;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class IndexCollectionConfig {
    private String rootXpath;
    private String collectionName;
    private Set<IndexFieldConfig> IndexedFieldConfig = new HashSet<>();

    public IndexCollectionConfig(String rootXpath, String collectionName) {
        this.rootXpath = rootXpath;
        this.collectionName = collectionName;
    }
}
