package ru.runa.wfe.audit.dao;

import com.google.common.base.Strings;
import com.querydsl.jpa.JPAExpressions;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.val;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.runa.wfe.audit.CreateTimerLog;
import ru.runa.wfe.audit.NodeEnterLog;
import ru.runa.wfe.audit.NodeLeaveLog;
import ru.runa.wfe.audit.ProcessLogVisitor;
import ru.runa.wfe.audit.ReceiveMessageLog;
import ru.runa.wfe.audit.TaskAssignLog;
import ru.runa.wfe.audit.TaskCancelledLog;
import ru.runa.wfe.audit.TaskCreateLog;
import ru.runa.wfe.audit.TaskEndByAdminLog;
import ru.runa.wfe.audit.TaskEndBySubstitutorLog;
import ru.runa.wfe.audit.TaskEndLog;
import ru.runa.wfe.audit.aggregated.QSignalListenerAggregatedLog;
import ru.runa.wfe.audit.aggregated.QTaskAggregatedLog;
import ru.runa.wfe.audit.aggregated.QTaskAssignmentAggregatedLog;
import ru.runa.wfe.audit.aggregated.QTimerAggregatedLog;
import ru.runa.wfe.audit.aggregated.SignalListenerAggregatedLog;
import ru.runa.wfe.audit.aggregated.TaskAggregatedLog;
import ru.runa.wfe.audit.aggregated.TaskAssignmentAggregatedLog;
import ru.runa.wfe.audit.aggregated.TaskEndReason;
import ru.runa.wfe.audit.aggregated.TimerAggregatedLog;
import ru.runa.wfe.commons.querydsl.HibernateQueryFactory;
import ru.runa.wfe.lang.BaseReceiveMessageNode;
import ru.runa.wfe.lang.NodeType;

@Component
public class AggregatedProcessLogVisitor extends ProcessLogVisitor {

    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private HibernateQueryFactory queryFactory;

    @Override
    public void onNodeEnterLog(NodeEnterLog nodeEnterLog) {
        if (nodeEnterLog.getNode() instanceof BaseReceiveMessageNode) {
            sessionFactory.getCurrentSession().save(
                    new SignalListenerAggregatedLog(nodeEnterLog, ((BaseReceiveMessageNode) nodeEnterLog.getNode()).getEventType()));
        }
    }
    
    @Override
    public void onNodeLeaveLog(NodeLeaveLog nodeLeaveLog) {
        if (nodeLeaveLog.getNodeType() == NodeType.TIMER) {
            QTimerAggregatedLog l = QTimerAggregatedLog.timerAggregatedLog;
            TimerAggregatedLog logEntry = queryFactory.selectFrom(l)
                    .where(l.processId.eq(nodeLeaveLog.getProcessId()).and(l.nodeId.eq(nodeLeaveLog.getNodeId()))).orderBy(l.id.desc()).fetchFirst();
            if (logEntry == null) {
                return;
            }
            logEntry.setEndDate(nodeLeaveLog.getCreateDate());
            sessionFactory.getCurrentSession().merge(logEntry);
        }
    }
    
    @Override
    public void onReceiveMessageLog(ReceiveMessageLog receiveMessageLog) {
        QSignalListenerAggregatedLog l = QSignalListenerAggregatedLog.signalListenerAggregatedLog;
        SignalListenerAggregatedLog logEntry = queryFactory.selectFrom(l)
                .where(l.processId.eq(receiveMessageLog.getProcessId()).and(l.nodeId.eq(receiveMessageLog.getNodeId()))).orderBy(l.id.desc())
                .fetchFirst();
        if (logEntry == null) {
            return;
        }
        logEntry.setExecuteDate(receiveMessageLog.getCreateDate());
        sessionFactory.getCurrentSession().merge(logEntry);
    }

    @Override
    public void onCreateTimerLog(CreateTimerLog createTimerLog) {
        sessionFactory.getCurrentSession().save(new TimerAggregatedLog(createTimerLog));
    }

    @Override
    public void onTaskCreateLog(TaskCreateLog l) {
        if (getTaskLog(l.getTaskId()) != null) {
            return;
        }
        TaskAggregatedLog tal = new TaskAggregatedLog();
        tal.setTaskId(l.getTaskId());
        tal.setProcessId(l.getProcessId());
        tal.setCreateDate(l.getCreateDate());
        tal.setDeadlineDate(l.getDeadlineDate());
        tal.setTokenId(l.getTokenId());
        tal.setNodeId(l.getNodeId());
        tal.setTaskName(l.getTaskName());
        tal.setTaskIndex(l.getTaskIndex());
        tal.setSwimlaneName(l.getSwimlaneName());
        tal.setEndReason(TaskEndReason.PROCESSING);
        sessionFactory.getCurrentSession().save(tal);
    }

    @Override
    public void onTaskAssignLog(TaskAssignLog taskAssignLog) {
        TaskAggregatedLog l = getTaskLog(taskAssignLog.getTaskId());
        if (l == null) {
            return;
        }

        saveAssignment(l, taskAssignLog.getCreateDate(), taskAssignLog.getNewExecutorName());
        if (Strings.isNullOrEmpty(l.getCompleteActorName())) {
            l.setInitialActorName(taskAssignLog.getNewExecutorName());
        }

        sessionFactory.getCurrentSession().merge(l);
    }

    @Override
    public void onTaskEndLog(TaskEndLog taskEndLog) {
        onTaskEnd(taskEndLog, TaskEndReason.COMPLETED);
    }

    @Override
    public void onTaskEndBySubstitutorLog(TaskEndBySubstitutorLog taskEndBySubstitutorLog) {
        onTaskEnd(taskEndBySubstitutorLog, TaskEndReason.SUBSTITUTOR_END);
    }

    @Override
    public void onTaskEndByAdminLog(TaskEndByAdminLog taskEndByAdminLog) {
        onTaskEnd(taskEndByAdminLog, TaskEndReason.ADMIN_END);
    }

    @Override
    public void onTaskCancelledLog(TaskCancelledLog taskCancelledLog) {
        onTaskEnd(taskCancelledLog, taskCancelledLog.getActorName() == null ? TaskEndReason.CANCELLED : TaskEndReason.COMPLETED);
    }

    private TaskAggregatedLog getTaskLog(long taskId) {
        val l = QTaskAggregatedLog.taskAggregatedLog;
        return queryFactory.selectFrom(l).where(l.taskId.eq(taskId)).fetchFirst();
    }

    private void onTaskEnd(TaskEndLog taskEndLog, TaskEndReason endReason) {
        TaskAggregatedLog l = getTaskLog(taskEndLog.getTaskId());
        if (l == null) {
            return;
        }

        saveAssignment(l, taskEndLog.getCreateDate(), taskEndLog.getActorName());
        l.setEndDate(taskEndLog.getCreateDate());
        l.setCompleteActorName(taskEndLog.getActorName());
        l.setEndReason(endReason);
        l.setTransitionName(taskEndLog.getTransitionName());

        sessionFactory.getCurrentSession().merge(l);
    }

    private void saveAssignment(TaskAggregatedLog tal, Date assignmentDate, String newExecutorName) {
        newExecutorName = Strings.nullToEmpty(newExecutorName);
        // Insert new record if last executor name is different from newExecutorName,
        // and if same record does not already exists (this check is for import operation: assignment may already be saved before import).
        // Instead of loading all detail rows via Hibernate collections, here I check both conditions using single optimized SQL query.
        val l = new QTaskAssignmentAggregatedLog("l");
        val l2 = new QTaskAssignmentAggregatedLog("l2");
        List<String> rows = queryFactory.select(l.newExecutorName)
                .from(l)
                .where(l.id.eq(JPAExpressions.select(l2.id.max()).from(l2).where(l2.log.eq(tal))).or(
                        l.assignDate.eq(assignmentDate).and(l.newExecutorName.eq(newExecutorName))
                ))
                .orderBy(l.id.desc())
                .fetch();

        // If rows.size() == 0, no detail rows exist for given TaskAggregatedLog ==> insert.
        // If rows.size() >  1, we got both last AND existing row ==> DON'T insert.
        // If rows.size() == 1, have to check returned newExecutorName.
        val oldExecutorName = rows.size() == 1 ? rows.get(0) : null;
        if (rows.isEmpty() || rows.size() == 1 && !Objects.equals(oldExecutorName, newExecutorName)) {
            val taal = new TaskAssignmentAggregatedLog();
            taal.setLog(tal);
            //noinspection ConstantConditions
            taal.setAssignDate(assignmentDate);
            taal.setOldExecutorName(oldExecutorName);
            taal.setNewExecutorName(newExecutorName);
            sessionFactory.getCurrentSession().save(taal);
        }
    }
}
