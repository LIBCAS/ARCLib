package cz.inqool.uas.store;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.EnumPath;
import cz.inqool.uas.domain.CodedObject;
import cz.inqool.uas.index.Labeled;

/**
 * {@link DictionaryStore} specialized implementation. Exists for orthogonal purpose.
 *
 * @param <T> Type of entity to hold
 * @param <Q> Type of query object
 */
public abstract class CodedStore<T extends CodedObject<V>, Q extends EntityPathBase<T>, V extends Enum<V> & Labeled> extends DictionaryStore<T, Q> {
    private Class<V> cType;

    public CodedStore(Class<T> type, Class<Q> qType, Class<V> cType) {
        super(type, qType);
        this.cType = cType;
    }

    /**
     * Finds the instance by specified code value
     * @param code Code value
     * @return Found instance
     */
    public T findByCode(V code) {
        Q qObject = qObject();

        T type = query().select(qObject)
                                   .where(findWhereExpression())
                                   .where(codeExpression(code))
                                   .fetchFirst();

        detachAll();

        return type;
    }

    protected BooleanExpression codeExpression(V code) {
        EnumPath<V> codePath = propertyPathEnum("code", cType);
        return codePath.eq(code);
    }
}
