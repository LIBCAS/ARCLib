package cz.inqool.uas.bpm.config.notify;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.util.xml.Element;

/**
 * Camunda global notification listener.
 */
public class NotifyEventParseListener extends AbstractBpmnParseListener {
    private NotifyTaskListener notifyTaskListener;
    private NotifyPoolTaskListener notifyPoolTaskListener;

    public NotifyEventParseListener(NotifyTaskListener notifyTaskListener, NotifyPoolTaskListener notifyPoolTaskListener) {
        this.notifyTaskListener = notifyTaskListener;
        this.notifyPoolTaskListener = notifyPoolTaskListener;
    }

    /**
     * Hooks listeners to assignment and creation of tasks.
     * @param userTaskElement task to hook to
     * @param scope a BPMN scope
     * @param activity an Activity scope
     */
    @Override
    public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
        UserTaskActivityBehavior activityBehavior = (UserTaskActivityBehavior) activity.getActivityBehavior();
        TaskDefinition taskDefinition = activityBehavior.getTaskDefinition();
        addTaskAssignmentListeners(taskDefinition);
    }

    protected void addTaskAssignmentListeners(TaskDefinition taskDefinition) {
        taskDefinition.addTaskListener(TaskListener.EVENTNAME_ASSIGNMENT, notifyTaskListener);
        taskDefinition.addTaskListener(TaskListener.EVENTNAME_CREATE, notifyPoolTaskListener);
    }
}
