package cz.inqool.uas.bpm.security;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.impl.cfg.multitenancy.TenantIdProvider;
import org.camunda.bpm.engine.impl.cfg.multitenancy.TenantIdProviderCaseInstanceContext;
import org.camunda.bpm.engine.impl.cfg.multitenancy.TenantIdProviderHistoricDecisionInstanceContext;
import org.camunda.bpm.engine.impl.cfg.multitenancy.TenantIdProviderProcessInstanceContext;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.identity.Authentication;
import org.camunda.bpm.engine.variable.VariableMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provides tenantId for newly started processes.
 *
 * First it searches for tenantId variable in provided variables, then it checks currently signed user.
 *
 * No tenant id might also be found and it is not an error.
 */
@ConditionalOnProperty(prefix = "bpm", name = "enabled", havingValue = "true")
@Service
public class UasTenantIdProvider implements TenantIdProvider {
    @Override
    public String provideTenantIdForProcessInstance(TenantIdProviderProcessInstanceContext ctx) {
        return getTenantId(ctx.getVariables());
    }

    @Override
    public String provideTenantIdForCaseInstance(TenantIdProviderCaseInstanceContext ctx) {
        return getTenantId(ctx.getVariables());
    }

    @Override
    public String provideTenantIdForHistoricDecisionInstance(TenantIdProviderHistoricDecisionInstanceContext ctx) {
        return getTenantId(ctx.getExecution().getVariablesTyped());
    }

    protected String getTenantId(VariableMap variables) {
        String tenantId = getTenantIdFromVariableMap(variables);
        if (tenantId == null) {
            return getTenantIdOfCurrentAuthentication();
        } else {
            return tenantId;
        }
    }

    protected String getTenantIdFromVariableMap(VariableMap variables) {
        return variables.getValue("tenantId", String.class);
    }

    protected String getTenantIdOfCurrentAuthentication() {
        IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();
        Authentication currentAuthentication = identityService.getCurrentAuthentication();

        if (currentAuthentication != null) {

            List<String> tenantIds = currentAuthentication.getTenantIds();
            if (tenantIds.size() == 1) {
                return tenantIds.get(0);

            } else if (tenantIds.isEmpty()) {
                return null;
            } else {
                throw new IllegalStateException("more than one authenticated tenant");
            }
        } else {
            return null;
        }
    }
}
