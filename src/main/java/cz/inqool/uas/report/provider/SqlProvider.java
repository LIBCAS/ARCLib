package cz.inqool.uas.report.provider;

import cz.inqool.uas.report.exception.GeneratingException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;

import static cz.inqool.uas.util.Utils.asMap;

/**
 * Report provider working with SQL statement and params supplied from user.
 *
 * <p>
 *     This {@link ReportProvider} is used when generating complex report based on SQL data.
 * </p>
 */
@Slf4j
@Service
public class SqlProvider extends BaseProvider<SqlProvider.Input> {
    private EntityManager em;

    public SqlProvider() {
        super(Input.class);
    }

    @Override
    public String getName() {
        return "SQL provider";
    }

    @Override
    public Map<String, Object> provide(Input input) {

        if (input.getSql() != null) {
            Query query = em.createNativeQuery(input.getSql());

            List<Object> params = input.getParams();
            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    query.setParameter(i+1, params.get(i));
                }
            }

            List result = query.getResultList();

            return asMap("result", result);
        } else {
            log.error("Sql query not provided.");
            throw new GeneratingException("Bad sql specified.");
        }
    }

    @Inject
    public void setEm(EntityManager em) {
        this.em = em;
    }

    @Getter
    @Setter
    public static class Input {
        private String sql;
        private List<Object> params;
    }
}
