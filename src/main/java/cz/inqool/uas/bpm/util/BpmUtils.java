package cz.inqool.uas.bpm.util;

import cz.inqool.uas.exception.BadArgument;
import org.camunda.bpm.engine.form.FormData;
import org.camunda.bpm.engine.form.FormField;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.inqool.uas.util.Utils.eq;

public class BpmUtils {
    public static Map<String, Object> filterDefinedFields(FormData formData, Map<String, Object> in) {
        Set<String> fields = formData.getFormFields().stream()
                .map(FormField::getId)
                .collect(Collectors.toSet());

        return in.entrySet().stream()
                .filter(entry -> fields.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static boolean canStart(BpmnModelInstance modelInstance, UserDetails user) {
        Collection<StartEvent> startEvents = modelInstance.getModelElementsByType(StartEvent.class);
        eq(startEvents.size(), 1, () -> new BadArgument("wrong number of start events"));

        StartEvent event = startEvents.iterator().next();

        ExtensionElements extensionElements = event.getExtensionElements();
        if (extensionElements != null) {
            List<CamundaProperties> propertiesList = extensionElements.getElementsQuery()
                    .filterByType(CamundaProperties.class)
                    .list();

            if (propertiesList.size() > 0) {
                CamundaProperties properties = propertiesList.get(0);

                for (CamundaProperty property : properties.getCamundaProperties()) {
                    if (property.getCamundaName().equals("authority")) {
                        // if no user specified and authority required, then starting is not allowed
                        if (user == null) {
                            return false;
                        }

                        String authority = property.getCamundaValue();
                        if (!hasAuthority(user, authority)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private static boolean hasAuthority(UserDetails user, String authority) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.equals(authority))
                .count() > 0;
    }
}
