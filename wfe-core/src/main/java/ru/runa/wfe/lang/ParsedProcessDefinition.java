package ru.runa.wfe.lang;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.definition.DefinitionFileDoesNotExistException;
import ru.runa.wfe.definition.FileDataProvider;
import ru.runa.wfe.definition.InvalidDefinitionException;
import ru.runa.wfe.definition.Language;
import ru.runa.wfe.definition.ProcessDefinition;
import ru.runa.wfe.definition.ProcessDefinitionAccessType;
import ru.runa.wfe.definition.ProcessDefinitionChange;
import ru.runa.wfe.form.Interaction;
import ru.runa.wfe.security.SecuredObjectType;
import ru.runa.wfe.task.Task;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.var.UserType;
import ru.runa.wfe.var.VariableDefinition;
import ru.runa.wfe.var.format.ListFormat;
import ru.runa.wfe.var.format.LongFormat;
import ru.runa.wfe.var.format.MapFormat;
import ru.runa.wfe.var.format.VariableFormatContainer;

public class ParsedProcessDefinition extends GraphElement implements FileDataProvider {
    private final Long id;
    private final Long packId;
    private Language language = Language.BPMN2;
    private String name;
    private String description;
    private final String category;
    private final Long version;
    private final Date createDate;
    private final Actor createActor;
    private final Date updateDate;
    private final Actor updateActor;
    private final Date subprocessBindingDate;
    private final Integer secondsBeforeArchiving;
    private final SecuredObject securedObject = new SecuredObject();;
    private final Map<String, byte[]> processFiles = new HashMap<>();
    private StartNode manualStartNode;
    private final List<StartNode> eventStartNodes = new ArrayList<>();
    private final List<Node> nodeList = new ArrayList<>();
    private final Map<String, Node> nodesMap = new HashMap<>();
    private final List<SwimlaneDefinition> swimlaneDefinitions = new ArrayList<>();
    private final Map<String, SwimlaneDefinition> swimlaneDefinitionsMap = new HashMap<>();
    private final Map<String, Interaction> interactions = new HashMap<>();
    private final Map<String, UserType> userTypes = new HashMap<>();
    private final List<VariableDefinition> variables = new ArrayList<>();
    private final Map<String, VariableDefinition> variablesMap = new HashMap<>();
    private final Map<String, ParsedSubprocessDefinition> embeddedSubprocesses = new HashMap<>();
    private ProcessDefinitionAccessType accessType = ProcessDefinitionAccessType.Process;
    private boolean triggeredByEvent;
    private Boolean nodeAsyncExecution;
    private boolean graphActionsEnabled;
    private final List<ProcessDefinitionChange> changes = new ArrayList<>();
    private Boolean taskButtonLabelBySingleTransitionName = null;

    public class SecuredObject extends ru.runa.wfe.security.SecuredObject {
        private static final long serialVersionUID = 1L;

        @Override
        public Long getId() {
            return ParsedProcessDefinition.this.packId;
        }

        @Override
        public SecuredObjectType getSecuredObjectType() {
            return SecuredObjectType.DEFINITION;
        }

    }

    public ParsedProcessDefinition(@NonNull ProcessDefinition processDefinition) {
        id = processDefinition.getId();
        packId = processDefinition.getPack().getId();
        version = processDefinition.getVersion();
        name = processDefinition.getPack().getName();
        description = processDefinition.getPack().getDescription();
        category = processDefinition.getPack().getCategory();
        createDate = processDefinition.getCreateDate();
        createActor = processDefinition.getCreateActor();
        updateDate = processDefinition.getUpdateDate();
        updateActor = processDefinition.getUpdateActor();
        subprocessBindingDate = processDefinition.getSubprocessBindingDate();
        secondsBeforeArchiving = processDefinition.getPack().getSecondsBeforeArchiving();
        parsedProcessDefinition = this;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getPackId() {
        return packId;
    }

    public String getCategory() {
        return category;
    }

    public Long getVersion() {
        return version;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public Actor getCreateActor() {
        return createActor;
    }

    public Actor getUpdateActor() {
        return updateActor;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public Date getSubprocessBindingDate() {
        return subprocessBindingDate;
    }

    public Integer getSecondsBeforeArchiving() {
        return secondsBeforeArchiving;
    }

    public SecuredObject getSecuredObject() {
        return securedObject;
    }

    public ProcessDefinitionAccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(ProcessDefinitionAccessType accessType) {
        this.accessType = accessType;
    }

    /**
     * add a file to this definition.
     */
    public void addFile(String name, byte[] bytes) {
        processFiles.put(name, bytes);
    }

    public void addInteraction(String name, Interaction interaction) {
        interactions.put(name, interaction);
    }

    public void addUserType(UserType userType) {
        userTypes.put(userType.getName(), userType);
    }

    public void addVariable(VariableDefinition variableDefinition) {
        variablesMap.put(variableDefinition.getScriptingName(), variableDefinition);
        variablesMap.put(variableDefinition.getName(), variableDefinition);
        variables.add(variableDefinition);
    }

    public VariableDefinition getVariable(String name, boolean searchInSwimlanes) {
        VariableDefinition variableDefinition = variablesMap.get(name);
        if (variableDefinition != null) {
            return variableDefinition;
        }
        if (searchInSwimlanes) {
            SwimlaneDefinition swimlaneDefinition = getSwimlane(name);
            if (swimlaneDefinition != null) {
                return swimlaneDefinition.toVariableDefinition();
            }
        }
        if (name.endsWith(VariableFormatContainer.SIZE_SUFFIX)) {
            String listVariableName = name.substring(0, name.length() - VariableFormatContainer.SIZE_SUFFIX.length());
            VariableDefinition listVariableDefinition = getVariable(listVariableName, false);
            if (listVariableDefinition != null) {
                return new VariableDefinition(name, null, LongFormat.class.getName(), null);
            }
            log.debug("Unable to build list (map) size variable by name '" + name + "'");
            return null;
        }
        return buildVariable(name);
    }

    private VariableDefinition buildVariable(String variableName) {
        int dotIndex = variableName.indexOf(UserType.DELIM);
        if (dotIndex != -1) {
            String parentName = variableName.substring(0, dotIndex);
            String remainderName = variableName.substring(dotIndex + 1);
            int componentStartIndex = parentName.indexOf(VariableFormatContainer.COMPONENT_QUALIFIER_START);
            String parentVariableName = componentStartIndex != -1 ? parentName.substring(0, componentStartIndex) : parentName;
            VariableDefinition parentVariableDefinition = variablesMap.get(parentVariableName);
            VariableDefinition parentUserTypeVariableDefinition = null;
            if (parentVariableDefinition != null && parentVariableDefinition.isUserType()) {
                parentUserTypeVariableDefinition = parentVariableDefinition;
            }
            if (parentVariableDefinition != null && componentStartIndex != -1) {
                if (ListFormat.class.getName().equals(parentVariableDefinition.getFormatClassName())) {
                    String appendix = parentName.substring(componentStartIndex);
                    String format = parentVariableDefinition.getFormatComponentClassNames()[0];
                    parentUserTypeVariableDefinition = new VariableDefinition(parentVariableDefinition.getName() + appendix,
                            parentVariableDefinition.getScriptingName() + appendix, format, getUserType(format));
                    parentUserTypeVariableDefinition.initComponentUserTypes(this);
                } else if (MapFormat.class.getName().equals(parentVariableDefinition.getFormatClassName())) {
                    String appendix = parentName.substring(componentStartIndex);
                    String userTypeName;
                    if (appendix.contains(":k")) {
                        userTypeName = parentVariableDefinition.getFormatComponentClassNames()[0];
                    } else {
                        userTypeName = parentVariableDefinition.getFormatComponentClassNames()[1];
                    }
                    parentUserTypeVariableDefinition = new VariableDefinition(parentVariableDefinition.getName() + appendix,
                            parentVariableDefinition.getScriptingName() + appendix, parentVariableDefinition.getFormat(), getUserType(userTypeName));
                    parentUserTypeVariableDefinition.initComponentUserTypes(this);
                }
            }
            if (parentUserTypeVariableDefinition != null) {
                VariableDefinition attributeDefinition = parentUserTypeVariableDefinition.getUserType().getAttributeExpanded(remainderName);
                if (attributeDefinition != null) {
                    String name = parentUserTypeVariableDefinition.getName() + UserType.DELIM + attributeDefinition.getName();
                    VariableDefinition variableDefinition = new VariableDefinition(name, null, attributeDefinition);
                    return variableDefinition;
                }
            }
            log.debug("Unable to build syntetic variable by name '" + variableName + "', last checked " + parentVariableDefinition + " with "
                    + remainderName);
            return null;
        }
        int componentStartIndex = variableName.indexOf(VariableFormatContainer.COMPONENT_QUALIFIER_START);
        if (componentStartIndex != -1) {
            String containerVariableName = variableName.substring(0, componentStartIndex);
            VariableDefinition containerVariableDefinition = variablesMap.get(containerVariableName);
            if (containerVariableDefinition == null) {
                log.debug("Unable to build syntetic container variable by name '" + variableName + "'");
                return null;
            }
            if (containerVariableDefinition.getFormatComponentClassNames().length == 0) {
                throw new InternalApplicationException("Not a list variable: " + containerVariableDefinition.getName());
            }
            String format = containerVariableDefinition.getFormatComponentClassNames()[0];
            VariableDefinition variableDefinition = new VariableDefinition(variableName, null, format, getUserType(format));
            variableDefinition.initComponentUserTypes(this);
            return variableDefinition;
        }
        return null;
    }

    public VariableDefinition getVariableNotNull(String name, boolean searchInSwimlanes) {
        VariableDefinition variableDefinition = getVariable(name, searchInSwimlanes);
        if (variableDefinition == null) {
            throw new InternalApplicationException("variable '" + name + "' not found in " + this);
        }
        return variableDefinition;
    }

    public UserType getUserType(String name) {
        return userTypes.get(name);
    }

    public UserType getUserTypeNotNull(String name) {
        UserType userType = getUserType(name);
        if (userType == null) {
            throw new InternalApplicationException("UserType '" + name + "' not found");
        }
        return userType;
    }

    public List<UserType> getUserTypes() {
        return new ArrayList<>(userTypes.values());
    }

    public List<VariableDefinition> getVariables() {
        return variables;
    }

    public Interaction getInteractionNotNull(String nodeId) {
        Interaction interaction = interactions.get(nodeId);
        if (interaction == null) {
            InteractionNode node = (InteractionNode) getNodeNotNull(nodeId);
            interaction = new Interaction(node, null, null, null, false, null, null, null, null);
        }
        return interaction;
    }

    public Map<String, byte[]> getProcessFiles() {
        return processFiles;
    }

    @Override
    public byte[] getFileData(String fileName) {
        Preconditions.checkNotNull(fileName, "fileName");
        return processFiles.get(fileName);
    }

    @Override
    public byte[] getFileDataNotNull(String fileName) {
        byte[] bytes = getFileData(fileName);
        if (bytes == null) {
            throw new DefinitionFileDoesNotExistException(fileName);
        }
        return bytes;
    }

    public byte[] getGraphImageBytesNotNull() {
        byte[] graphBytes = parsedProcessDefinition.getFileData(FileDataProvider.GRAPH_IMAGE_NEW_FILE_NAME);
        if (graphBytes == null) {
            graphBytes = parsedProcessDefinition.getFileData(FileDataProvider.GRAPH_IMAGE_OLD2_FILE_NAME);
        }
        if (graphBytes == null) {
            graphBytes = parsedProcessDefinition.getFileData(FileDataProvider.GRAPH_IMAGE_OLD1_FILE_NAME);
        }
        if (graphBytes == null) {
            throw new InternalApplicationException("No process graph image file found in process definition");
        }
        return graphBytes;
    }

    public Map<String, Object> getDefaultVariableValues() {
        Map<String, Object> result = new HashMap<>();
        for (VariableDefinition variableDefinition : variables) {
            if (variableDefinition.getDefaultValue() != null) {
                result.put(variableDefinition.getName(), variableDefinition.getDefaultValue());
            }
        }
        return result;
    }

    public StartNode getManualStartNode() {
        return manualStartNode;
    }

    public StartNode getManualStartStateNotNull() {
        Preconditions.checkNotNull(manualStartNode, "startNode");
        return manualStartNode;
    }

    public List<StartNode> getEventStartNodes() {
        return eventStartNodes;
    }

    public boolean isTriggeredByEvent() {
        return triggeredByEvent;
    }

    public void setTriggeredByEvent(boolean triggeredByEvent) {
        this.triggeredByEvent = triggeredByEvent;
    }

    public List<Node> getNodes(boolean withEmbeddedSubprocesses) {
        List<Node> result = new ArrayList<>(nodeList);
        if (withEmbeddedSubprocesses) {
            for (ParsedSubprocessDefinition subprocessDefinition : embeddedSubprocesses.values()) {
                result.addAll(subprocessDefinition.getNodes(withEmbeddedSubprocesses));
            }
        }
        return result;
    }

    public Node addNode(Node node) {
        Preconditions.checkArgument(node != null, "can't add a null node to a processdefinition");
        nodeList.add(node);
        if (nodesMap.put(node.getNodeId(), node) != null) {
            throw new InvalidDefinitionException(getName(), "found duplicated node " + node.getNodeId());
        }
        node.parsedProcessDefinition = this;
        if (node instanceof StartNode) {
            if (((StartNode) node).isStartByEvent()) {
                eventStartNodes.add((StartNode) node);
            } else {
                if (manualStartNode != null) {
                    throw new InvalidDefinitionException(getName(), "only one start-state allowed in a process");
                }
                manualStartNode = (StartNode) node;
            }
        }
        return node;
    }

    public List<Node> getNodeList() {
        return nodeList;
    }

    public Node getNode(String id) {
        Preconditions.checkNotNull(id);
        Node node = nodesMap.get(id);
        if (node != null) {
            return node;
        }
        if (id.startsWith(FileDataProvider.SUBPROCESS_DEFINITION_PREFIX)) {
            for (ParsedSubprocessDefinition subprocessDefinition : embeddedSubprocesses.values()) {
                node = subprocessDefinition.getNode(id);
                if (node != null) {
                    return node;
                }
            }
        }
        return null;
    }

    public Node getNodeNotNull(String id) {
        Preconditions.checkNotNull(id);
        Node node = getNode(id);
        if (node != null) {
            return node;
        }
        throw new InternalApplicationException("node '" + id + "' not found");
    }

    @Override
    public GraphElement getParentElement() {
        return null;
    }

    public void addSwimlane(SwimlaneDefinition swimlaneDefinition) {
        swimlaneDefinitions.add(swimlaneDefinition);
        swimlaneDefinitionsMap.put(swimlaneDefinition.getName(), swimlaneDefinition);
    }

    public void setSwimlaneScriptingName(String name, String scriptingName) {
        SwimlaneDefinition swimlaneDefinition = getSwimlaneNotNull(name);
        swimlaneDefinition.setScriptingName(scriptingName);
        swimlaneDefinitionsMap.put(swimlaneDefinition.getScriptingName(), swimlaneDefinition);
    }

    public List<SwimlaneDefinition> getSwimlanes() {
        return swimlaneDefinitions;
    }

    public SwimlaneDefinition getSwimlane(String swimlaneName) {
        return swimlaneDefinitionsMap.get(swimlaneName);
    }

    public SwimlaneDefinition getSwimlaneById(String id) {
        for (SwimlaneDefinition swimlaneDefinition : swimlaneDefinitions) {
            if (Objects.equal(id, swimlaneDefinition.getNodeId())) {
                return swimlaneDefinition;
            }
        }
        return null;
    }

    public SwimlaneDefinition getSwimlaneNotNull(String swimlaneName) {
        SwimlaneDefinition swimlaneDefinition = getSwimlane(swimlaneName);
        if (swimlaneDefinition == null) {
            throw new InternalApplicationException("swimlane '" + swimlaneName + "' not found in " + this);
        }
        return swimlaneDefinition;
    }

    public boolean ignoreSubsitutionRulesForTask(Task task) {
        InteractionNode interactionNode = (InteractionNode) getNodeNotNull(task.getNodeId());
        return interactionNode.getFirstTaskNotNull().isIgnoreSubsitutionRules();
    }

    public Boolean getNodeAsyncExecution() {
        return nodeAsyncExecution;
    }

    public void setNodeAsyncExecution(Boolean nodeAsyncExecution) {
        this.nodeAsyncExecution = nodeAsyncExecution;
    }

    public boolean isGraphActionsEnabled() {
        return graphActionsEnabled;
    }

    public void setGraphActionsEnabled(boolean graphActionsEnabled) {
        this.graphActionsEnabled = graphActionsEnabled;
    }

    public void addEmbeddedSubprocess(ParsedSubprocessDefinition subprocessDefinition) {
        embeddedSubprocesses.put(subprocessDefinition.getNodeId(), subprocessDefinition);
    }

    public List<String> getEmbeddedSubprocessNodeIds() {
        List<String> result = new ArrayList<>();
        for (Node node : nodeList) {
            if (node instanceof SubprocessNode && ((SubprocessNode) node).isEmbedded()) {
                result.add(node.getNodeId());
            }
        }
        return result;
    }

    public String getEmbeddedSubprocessNodeId(String subprocessName) {
        for (Node node : nodeList) {
            if (node instanceof SubprocessNode) {
                SubprocessNode subprocessNode = (SubprocessNode) node;
                if (subprocessNode.isEmbedded() && Objects.equal(subprocessName, subprocessNode.getSubProcessName())) {
                    return node.getNodeId();
                }
            }
        }
        for (ParsedSubprocessDefinition subprocessDefinition : embeddedSubprocesses.values()) {
            String nodeId = subprocessDefinition.getEmbeddedSubprocessNodeId(subprocessName);
            if (nodeId != null) {
                return nodeId;
            }
        }
        return null;
    }

    public String getEmbeddedSubprocessNodeIdNotNull(String subprocessName) {
        String subprocessNodeId = getEmbeddedSubprocessNodeId(subprocessName);
        if (subprocessNodeId == null) {
            throw new NullPointerException("No subprocess state found by subprocess name '" + subprocessName + "' in " + this);
        }
        return subprocessNodeId;
    }

    public Map<String, ParsedSubprocessDefinition> getEmbeddedSubprocesses() {
        return embeddedSubprocesses;
    }

    public ParsedSubprocessDefinition getEmbeddedSubprocessByIdNotNull(String id) {
        ParsedSubprocessDefinition subprocessDefinition = getEmbeddedSubprocesses().get(id);
        if (subprocessDefinition == null) {
            throw new InternalApplicationException(
                    "Embedded subprocess definition not found by id '" + id + "' in " + this + ", all = " + getEmbeddedSubprocesses().keySet());
        }
        return subprocessDefinition;
    }

    public ParsedSubprocessDefinition getEmbeddedSubprocessByNameNotNull(String name) {
        for (ParsedSubprocessDefinition subprocessDefinition : getEmbeddedSubprocesses().values()) {
            if (Objects.equal(name, subprocessDefinition.getName())) {
                return subprocessDefinition;
            }
        }
        throw new InternalApplicationException(
                "Embedded subprocess definition not found by name '" + name + "' in " + this + ", all = " + getEmbeddedSubprocesses().values());
    }

    public void mergeWithEmbeddedSubprocesses() {
        for (Node node : new ArrayList<>(nodeList)) {
            if (node instanceof SubprocessNode) {
                SubprocessNode subprocessNode = (SubprocessNode) node;
                if (subprocessNode.isEmbedded()) {
                    ParsedSubprocessDefinition subprocessDefinition = getEmbeddedSubprocessByNameNotNull(subprocessNode.getSubProcessName());
                    if (!subprocessNode.isTriggeredByEvent()) {
                        EmbeddedSubprocessStartNode startNode = subprocessDefinition.getManualStartStateNotNull();
                        for (Transition transition : subprocessNode.getArrivingTransitions()) {
                            startNode.addArrivingTransition(transition);
                        }
                        startNode.setSubprocessNode(subprocessNode);
                        for (EmbeddedSubprocessEndNode endNode : subprocessDefinition.getEndNodes()) {
                            endNode.addLeavingTransition(subprocessNode.getLeavingTransitions().get(0));
                            endNode.setSubprocessNode(subprocessNode);
                        }
                    } else {
                        for (StartNode startNode : subprocessDefinition.getEventStartNodes()) {
                            ((EmbeddedSubprocessStartNode) startNode).setSubprocessNode(subprocessNode);
                        }
                        for (EmbeddedSubprocessEndNode endNode : subprocessDefinition.getEndNodes()) {
                            endNode.setSubprocessNode(subprocessNode);
                        }
                    }
                    subprocessDefinition.mergeWithEmbeddedSubprocesses();
                }
            }
        }
    }

    public void setChanges(List<ProcessDefinitionChange> changes) {
        this.changes.addAll(changes);
    }

    public List<ProcessDefinitionChange> getChanges() {
        return changes;
    }

    public Boolean isTaskButtonLabelBySingleTransitionName() {
        return this.taskButtonLabelBySingleTransitionName;
    }

    public void setTaskButtonLabelBySingleTransitionName(Boolean value) {
        this.taskButtonLabelBySingleTransitionName = value;
    }

    @Override
    public String toString() {
        return name + " v " + version;
    }
}
