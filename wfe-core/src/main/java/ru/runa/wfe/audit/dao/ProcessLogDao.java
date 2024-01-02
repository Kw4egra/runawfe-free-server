package ru.runa.wfe.audit.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.runa.wfe.audit.ArchivedProcessLog;
import ru.runa.wfe.audit.BaseProcessLog;
import ru.runa.wfe.audit.CurrentProcessLog;
import ru.runa.wfe.audit.ProcessLogFilter;
import ru.runa.wfe.audit.ProcessLogVisitor;
import ru.runa.wfe.audit.ProcessLogsCleanLog;
import ru.runa.wfe.commons.dao.ArchiveAwareGenericDao;
import ru.runa.wfe.definition.dao.ProcessDefinitionLoader;
import ru.runa.wfe.execution.ArchivedProcess;
import ru.runa.wfe.execution.CurrentProcess;
import ru.runa.wfe.execution.CurrentToken;
import ru.runa.wfe.execution.Process;
import ru.runa.wfe.execution.dao.ProcessDao;
import ru.runa.wfe.lang.ParsedProcessDefinition;
import ru.runa.wfe.user.User;

@Component
public class ProcessLogDao extends ArchiveAwareGenericDao<BaseProcessLog, CurrentProcessLog, CurrentProcessLogDao, ArchivedProcessLog, ArchivedProcessLogDao> {

    private ProcessDao processDao;
    private ProcessDefinitionLoader processDefinitionLoader;
    private SystemLogDao systemLogDao;
    private List<ProcessLogVisitor> processLogVisitors;

    @Autowired
    public ProcessLogDao(CurrentProcessLogDao currentDao, ArchivedProcessLogDao archivedDao, ProcessDao processDao, ProcessDefinitionLoader loader,
            SystemLogDao systemLogDao, List<ProcessLogVisitor> processLogVisitors) {
        super(currentDao, archivedDao);
        this.processDao = processDao;
        this.processDefinitionLoader = loader;
        this.systemLogDao = systemLogDao;
        this.processLogVisitors = processLogVisitors;
    }

    public List<? extends BaseProcessLog> getAll(@NonNull Process process) {
        if (process.isArchived()) {
            return archivedDao.getAll(process.getId());
        } else {
            return currentDao.getAll(process.getId());
        }
    }

    /**
     * Called with TemporaryGroup.processId; other contexts have Process instance available.
     */
    public List<? extends BaseProcessLog> getAll(@NonNull Long processId) {
        return getAll(processDao.getNotNull(processId));
    }

    public List<BaseProcessLog> getAll(final ProcessLogFilter filter) {
        val process = filter.getProcessId() != null
                ? processDao.get(filter.getProcessId())
                : null;
        if (process == null) {
            val current = currentDao.getAll(filter);
            val archived = archivedDao.getAll(filter);
            val result = new ArrayList<BaseProcessLog>(current.size() + archived.size());
            result.addAll(current);
            result.addAll(archived);
            result.sort(new Comparator<BaseProcessLog>() {
                    @Override
                    public int compare(BaseProcessLog o1, BaseProcessLog o2) {
                        return Long.compare(o1.getId(), o2.getId());
                    }
                }
            );
            return result;
        } else if (!process.isArchived()) {
            return currentDao.getAll(filter);
        } else if (filter.getTokenId() != null) {
            // Archive does not have TOKEN_ID field.
            return Collections.emptyList();
        } else {
            return archivedDao.getAll(filter);
        }
    }

    public List<? extends BaseProcessLog> get(@NonNull Process process, ParsedProcessDefinition definition) {
        if (process.isArchived()) {
            return archivedDao.get((ArchivedProcess) process, definition);
        } else {
            return currentDao.get((CurrentProcess) process, definition);
        }
    }

    public boolean isNodeEntered(@NonNull Process process, String nodeId) {
        if (process.isArchived()) {
            return archivedDao.isNodeEntered((ArchivedProcess) process, nodeId);
        } else {
            return currentDao.isNodeEntered((CurrentProcess) process, nodeId);
        }
    }

    public void addLog(CurrentProcessLog processLog, CurrentProcess process, CurrentToken token) {
        processLog.setProcessId(process.getId());
        if (token != null) {
            processLog.setTokenId(token.getId());
        }
        if (processLog.getNodeId() == null && token != null) {
            processLog.setNodeId(token.getNodeId());
        }
        processLog.setCreateDate(new Date());
        processLog.serializeAttributes();

        currentDao.create(processLog);

        for (ProcessLogVisitor processLogVisitor : processLogVisitors) {
            processLog.processBy(processLogVisitor);
        }
    }

    /**
     * Deletes all process logs.
     */
    public void deleteAll(CurrentProcess process) {
        currentDao.deleteAll(process);
    }

    public void deleteBeforeDate(User user, Date date) {
        currentDao.deleteBeforeDate(user, date);
        systemLogDao.create(new ProcessLogsCleanLog(user.getActor().getId(), date));
    }
}
