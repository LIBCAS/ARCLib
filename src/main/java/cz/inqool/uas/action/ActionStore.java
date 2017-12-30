package cz.inqool.uas.action;

import cz.inqool.uas.index.IndexedDictionaryStore;
import cz.inqool.uas.index.IndexedStore;
import org.springframework.stereotype.Repository;

import static cz.inqool.uas.util.Utils.toLabeledReference;

/**
 * Implementation of {@link IndexedStore} for storing {@link Action} and indexing {@link IndexedAction}.
 */
@Repository
public class ActionStore extends IndexedDictionaryStore<Action, QAction, IndexedAction> {

    public ActionStore() {
        super(Action.class, QAction.class, IndexedAction.class);
    }

    @Override
    public IndexedAction toIndexObject(Action o) {
        IndexedAction indexed = super.toIndexObject(o);

        indexed.setCode(o.getCode());
        indexed.setScriptType(toLabeledReference(o.getScriptType()));

        return indexed;
    }

    public Action findByCode(String code) {
        QAction qAction = qObject();

        Action action = query().select(qAction)
                .where(qAction.deleted.isNull())
                .where(qAction.code.eq(code))
                .fetchFirst();

        detachAll();

        return action;
    }
}
