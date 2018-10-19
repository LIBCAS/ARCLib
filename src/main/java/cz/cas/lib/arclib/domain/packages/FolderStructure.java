package cz.cas.lib.arclib.domain.packages;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cas.lib.core.util.Utils.asSet;

/**
 * Stromová štruktúra súborového priečinku
 */
public class FolderStructure implements Serializable {

    /**
     * Názov zložky resp. súboru
     */
    private String caption;

    /**
     * Zoznam podzložiek resp. súborov
     */
    private Collection<FolderStructure> children;

    public FolderStructure(Collection<FolderStructure> children, String caption) {
        super();
        this.children = children;
        this.caption = caption;
    }

    public Collection<FolderStructure> getChildren() {
        return children;
    }

    public void setChildren(Collection<FolderStructure> children) {
        this.children = children;
    }

    /**
     * Add child to the collection of children.
     * <p>
     * If a child with the same caption already exists, no new child is created.
     * Otherwise a new child is added  to the collection of existing children.
     *
     * @param childToAdd child to add
     * @return the added child
     */
    public FolderStructure addChildIfNotExists(FolderStructure childToAdd) {
        if (children == null) {
            children = asSet(childToAdd);
            return childToAdd;
        }

        List<FolderStructure> sameCaptionChildren = children.stream()
                .filter(child -> child.getCaption().equals(childToAdd.getCaption()))
                .collect(Collectors.toList());

        if (sameCaptionChildren.isEmpty()) {
            children.add(childToAdd);
            return childToAdd;
        }

        return sameCaptionChildren.get(0);
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}