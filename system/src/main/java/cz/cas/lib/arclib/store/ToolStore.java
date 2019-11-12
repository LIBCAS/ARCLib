package cz.cas.lib.arclib.store;

import com.querydsl.jpa.impl.JPAQuery;
import cz.cas.lib.arclib.domain.preservationPlanning.QTool;
import cz.cas.lib.arclib.domain.preservationPlanning.Tool;
import cz.cas.lib.arclib.domainbase.store.NamedStore;
import org.springframework.stereotype.Repository;

@Repository
public class ToolStore
        extends NamedStore<Tool, QTool> {
    public ToolStore() {
        super(Tool.class, QTool.class);
    }

    /**
     * Finds the instance with provided name and version.
     *
     * @param name    Name of instance to find
     * @param version version of the instance
     * @return Single instance or null if not found
     */
    public Tool findByNameAndVersion(String name, String version) {
        QTool qTool = qObject();
        JPAQuery<Tool> query = query().select(qObject()).where(qTool.name.eq(name));
        if (version != null)
            query.where(qTool.version.eq(version));
        else
            query.where(qTool.version.isNull());
        Tool tool = query.fetchFirst();
        detachAll();
        return tool;
    }

    public Tool findLatestToolByName(String toolName) {
        QTool qTool = qObject();
        Tool entity = query().select(qTool).where(qTool.name.eq(toolName)).orderBy(qTool.created.desc()).fetchFirst();
        detachAll();
        return entity;
    }
}
