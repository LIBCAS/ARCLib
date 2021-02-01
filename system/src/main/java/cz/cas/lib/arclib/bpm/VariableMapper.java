package cz.cas.lib.arclib.bpm;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;

import java.util.List;

public interface VariableMapper {
    default Boolean getBooleanVariable(DelegateTask task, String name) {
        return (Boolean) task.getVariable(name);
    }

    default String getStringVariable(DelegateTask task, String name) {
        return (String) task.getVariable(name);
    }

    default List<String> getStringListVariable(DelegateTask task, String name) {
        return (List<String>) task.getVariable(name);
    }

    default String getStringVariable(DelegateExecution execution, String name) {
        return (String) execution.getVariable(name);
    }

    default Long getLongVariable(DelegateExecution execution, String name) {
        return (Long) execution.getVariable(name);
    }

    default Boolean getBooleanVariable(DelegateExecution execution, String name) {
        return (Boolean) execution.getVariable(name);
    }

    default List<String> getStringListVariable(DelegateExecution execution, String name) {
        return (List<String>) execution.getVariable(name);
    }
}
