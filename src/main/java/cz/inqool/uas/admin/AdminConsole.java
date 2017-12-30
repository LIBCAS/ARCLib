package cz.inqool.uas.admin;

import cz.inqool.uas.script.ScriptExecutor;
import cz.inqool.uas.script.ScriptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.AbstractNamedMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.ManagementServletContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import javax.inject.Inject;

/**
 * Management endpoint for admin script execution. Provides own HTML interface.
 */
@ConditionalOnProperty(prefix = "admin.console", name = "enabled", havingValue = "true")
@Component
public class AdminConsole extends AbstractNamedMvcEndpoint {
    private static final String LOCATION = "classpath:/console/";

    private final ManagementServletContext managementServletContext;

    private ScriptExecutor scriptExecutor;

    private TransactionTemplate transactionTemplate;

    public AdminConsole(ManagementServletContext managementServletContext) {
        super("console", "/console", true);
        this.managementServletContext = managementServletContext;
    }

    @RequestMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String browse() {
        return "forward:" + this.managementServletContext.getContextPath() + getPath()
                + "/index.html";
    }

    @ResponseBody
    @RequestMapping(value = "/execute", method = RequestMethod.POST)
    public String execute(@RequestParam("script") String script, @RequestParam("type") ScriptType type,
                          @RequestParam(name = "transaction", required=false) boolean transaction) {
        try {
            if (transaction && transactionTemplate != null) {
                return transactionTemplate.execute(transactionStatus -> executeScript(type, script));
            } else {
                return executeScript(type, script);
            }

        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private String executeScript(ScriptType type, String script) {
        Object result = scriptExecutor.executeScript(type, script);
        return result != null ? result.toString() : null;
    }


    @RequestMapping(value = "", produces = MediaType.TEXT_HTML_VALUE)
    public String redirect() {
        return "redirect:" + this.managementServletContext.getContextPath() + getPath()
                + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(
                this.managementServletContext.getContextPath() + getPath() + "/**")
                .addResourceLocations(LOCATION);
    }

    @Inject
    public void setScriptExecutor(ScriptExecutor scriptExecutor) {
        this.scriptExecutor = scriptExecutor;
    }

    @Autowired(required = false)
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }
}
