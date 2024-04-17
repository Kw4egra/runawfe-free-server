package ru.runa.wf.web.tag;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.jsp.PageContext;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.ecs.html.Area;
import org.apache.ecs.html.TD;
import org.apache.ecs.html.TR;
import org.apache.ecs.html.Table;
import ru.runa.common.WebResources;
import ru.runa.common.web.HTMLUtils;
import ru.runa.common.web.Messages;
import ru.runa.common.web.Resources;
import ru.runa.wf.web.MessagesProcesses;
import ru.runa.wf.web.action.ShowGraphModeHelper;
import ru.runa.wf.web.html.GraphElementPresentationHelper;
import ru.runa.wfe.audit.ActionLog;
import ru.runa.wfe.audit.BaseProcessLog;
import ru.runa.wfe.audit.Severity;
import ru.runa.wfe.commons.CalendarUtil;
import ru.runa.wfe.graph.view.ExclusiveGatewayGraphElement;
import ru.runa.wfe.graph.view.MultiSubprocessNodeGraphElement;
import ru.runa.wfe.graph.view.NodeGraphElement;
import ru.runa.wfe.graph.view.NodeGraphElementVisitor;
import ru.runa.wfe.graph.view.ScriptNodeGraphElement;
import ru.runa.wfe.graph.view.SubprocessNodeGraphElement;
import ru.runa.wfe.graph.view.TaskNodeGraphElement;
import ru.runa.wfe.graph.view.TimerNodeGraphElement;
import ru.runa.wfe.graph.view.VariableContainerNodeGraphElement;
import ru.runa.wfe.lang.NodeType;
import ru.runa.wfe.service.delegate.Delegates;
import ru.runa.wfe.user.User;
import ru.runa.wfe.var.VariableMapping;

/**
 * Operation to create links to subprocesses and tool tips to minimized elements.
 */
public class ProcessNodeGraphElementVisitor extends NodeGraphElementVisitor {
    private static final String TITLE = "title";

    private static final Pattern ACTION_LOG_PATTERN = Pattern.compile(".*?class=(.*?), configuration.*?", Pattern.DOTALL);

    /**
     * Helper to create links to subprocesses.
     */
    private final GraphElementPresentationHelper presentationHelper;
    private final User user;
    private final PageContext pageContext;
    private final boolean showElementDefinitionDetails;
    private final boolean showLogs;
    /**
     * Helper to create tool tips for task graph elements.
     */
    private final TD td;

    /**
     * Creates operation to create links to subprocesses and tool tips for elements.
     *
     * @param pageContext Rendered page context.
     * @param td          Root form element.
     */
    public ProcessNodeGraphElementVisitor(User user, PageContext pageContext, TD td, String subprocessId, boolean showElementDefinitionDetails,
            boolean showLogs) {
        this.user = user;
        this.pageContext = pageContext;
        this.showElementDefinitionDetails = showElementDefinitionDetails;
        this.showLogs = showLogs;
        this.td = td;
        presentationHelper = new GraphElementPresentationHelper(pageContext, subprocessId);
    }

    @Override
    public void visit(NodeGraphElement element) {
        Area area;
        if (element.getNodeType() == NodeType.SUBPROCESS && ((SubprocessNodeGraphElement) element).isSubprocessAccessible()) {
            area = presentationHelper.createSubprocessLink((SubprocessNodeGraphElement) element, ShowGraphModeHelper.getManageProcessAction(),
                    "javascript:showEmbeddedSubprocess", showElementDefinitionDetails, showLogs);
        } else {
            area = presentationHelper.createArea(element);
        }
        if (element.getNodeType() == NodeType.MULTI_SUBPROCESS) {
            td.addElement(presentationHelper.createMultiSubprocessLinks((MultiSubprocessNodeGraphElement) element,
                    ShowGraphModeHelper.getManageProcessAction()));
        }
        if (showElementDefinitionDetails) {
            Table table = presentationHelper.createCommonTooltip(element);
            if (element.getNodeType() == NodeType.TASK_STATE) {
                TaskNodeGraphElement taskNodeGraphElement = (TaskNodeGraphElement) element;
                presentationHelper.createTaskTooltip(taskNodeGraphElement, table);
            }
            if (element.getNodeType() == NodeType.ACTION_NODE) {
                presentationHelper.createScriptTooltip((ScriptNodeGraphElement) element, table);
            }
            if (element.getNodeType() == NodeType.EXCLUSIVE_GATEWAY || element.getNodeType() == NodeType.BUSINESS_RULE) {
                presentationHelper.createExclusiveGatewayTooltip((ExclusiveGatewayGraphElement) element, table);
            }
            if (element.getNodeType() == NodeType.SUBPROCESS) {
                presentationHelper.createSubprocessNameTooltip((SubprocessNodeGraphElement) element, table);
            }
            if (element.getNodeType() == NodeType.SEND_MESSAGE || element.getNodeType() == NodeType.RECEIVE_MESSAGE) {
                List<VariableMapping> variableMappings = ((VariableContainerNodeGraphElement) element).getVariableMappings();
                List<VariableMapping> selectorMappings = variableMappings.stream().filter(m -> m.isPropertySelector()).collect(Collectors.toList());
                List<VariableMapping> dataMappings = variableMappings.stream().filter(m -> !m.isPropertySelector()).collect(Collectors.toList());
                presentationHelper.createVariableMappingTooltip(table,
                        MessagesProcesses.LABEL_PROCESS_GRAPH_TOOLTIP_ROUTING_DATA.message(pageContext), selectorMappings, false);
                presentationHelper.createVariableMappingTooltip(table,
                        MessagesProcesses.LABEL_PROCESS_GRAPH_TOOLTIP_CONTENT_DATA.message(pageContext), dataMappings, false);
            }
            if (element.getNodeType() == NodeType.SUBPROCESS) {
                presentationHelper.createVariableMappingTooltip(table,
                        MessagesProcesses.LABEL_PROCESS_GRAPH_TOOLTIP_VARIABLE_MAPPING.message(pageContext),
                        ((VariableContainerNodeGraphElement) element).getVariableMappings(), true);
            }
            if (element.getNodeType() == NodeType.TIMER) {
                presentationHelper.createTimerTooltip((TimerNodeGraphElement) element, table);
            }
            area.setTitle(area.getAttribute(TITLE) + StringEscapeUtils.escapeHtml4(table.toString()));
        }
        if (showLogs && element.getData() != null) {
            Table table = new Table();
            table.setClass(Resources.CLASS_LIST_TABLE);
            int limit = WebResources.getProcessGraphNodeLogsLimitCount();
            if (limit > 0 && element.getData().size() > limit * 2 + 1) {
                element.getData().stream().limit(limit).forEach((log) -> {
                    addLogRow(table, log);
                });
                TR tr = new TR();
                tr.addElement(((TD) new TD().addAttribute("colspan", 2)).addElement("...").setClass(Resources.CLASS_LIST_TABLE_TD));
                table.addElement(tr);
                element.getData().stream().skip(element.getData().size() - limit).forEach((log) -> {
                    addLogRow(table, log);
                });
            } else {
                for (BaseProcessLog log : element.getData()) {
                    addLogRow(table, log);
                }
            }
            presentationHelper.addTooltip(element, area, (showElementDefinitionDetails ? "<br>" : "") + table.toString());
        }
    }

    public GraphElementPresentationHelper getPresentationHelper() {
        return presentationHelper;
    }

    private void addLogRow(Table table, BaseProcessLog log) {
        String description;
        try {
            String format = Messages.getMessage("history.log." + log.getPatternName(), pageContext);
            Object[] arguments = log.getPatternArguments();
            if (log instanceof ActionLog) {
                // #812
                Matcher matcher = ACTION_LOG_PATTERN.matcher((String) arguments[0]);
                if (matcher.find()) {
                    String className = matcher.group(1);
                    arguments[0] = Delegates.getSystemService().getLocalized(className);
                }
            }
            Object[] substitutedArguments = HTMLUtils.substituteArguments(user, pageContext, arguments);
            description = log.toString(format, substitutedArguments);
        } catch (Exception e) {
            description = log.toString();
        }
        TR tr = new TR();
        String eventDateString = CalendarUtil.format(log.getCreateDate(), CalendarUtil.DATE_WITH_HOUR_MINUTES_SECONDS_FORMAT);
        tr.addElement(new TD().addElement(eventDateString).setClass(Resources.CLASS_LIST_TABLE_TD));
        if (log.getSeverity() == Severity.ERROR) {
            // to be escaped in js
            description = "<error>" + description + "</error>";
        }
        tr.addElement(new TD().addElement(description).setClass(Resources.CLASS_LIST_TABLE_TD));
        table.addElement(tr);
    }

}
