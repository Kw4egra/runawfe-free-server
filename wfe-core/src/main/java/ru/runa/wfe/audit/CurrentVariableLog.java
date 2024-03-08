package ru.runa.wfe.audit;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import ru.runa.wfe.InternalApplicationException;
import ru.runa.wfe.user.Executor;
import ru.runa.wfe.var.CurrentVariable;
import ru.runa.wfe.var.VariableDefinition;
import ru.runa.wfe.var.file.FileVariable;

/**
 * Variables base logging class.
 * 
 * @author Dofs
 */
@MappedSuperclass
public abstract class CurrentVariableLog extends CurrentProcessLog implements VariableLog {
    private static final long serialVersionUID = 1L;

    public CurrentVariableLog() {
    }

    public CurrentVariableLog(CurrentVariable<?> variable) {
        setVariableName(variable.getName());
    }

    protected void setVariableNewValue(CurrentVariable<?> variable, Object newValue, VariableDefinition variableDefinition) {
        String newValueString;
        if (newValue instanceof Executor) {
            newValueString = ((Executor) newValue).getName();
            addAttribute(ATTR_IS_EXECUTOR_VALUE, ATTR_VALUE_TRUE);
        } else {
            newValueString = variable.toString(newValue, variableDefinition);
            if (newValue instanceof FileVariable) {
                addAttribute(ATTR_IS_FILE_VALUE, ATTR_VALUE_TRUE);
            }
            if (variable.getStorableValue() instanceof byte[]) {
                setBytes((byte[]) variable.getStorableValue());
            }
        }
        addAttributeWithTruncation(ATTR_NEW_VALUE, newValueString);
    }

    @Override
    @Transient
    public Type getType() {
        return Type.VARIABLE;
    }

    @Override
    @Transient
    public String getVariableNewValueAttribute() {
        return getAttribute(ATTR_NEW_VALUE);
    }

    @Override
    @Transient
    public boolean isFileValue() {
        return ATTR_VALUE_TRUE.equals(getAttribute(ATTR_IS_FILE_VALUE));
    }

    @Override
    @Transient
    public boolean isExecutorValue() {
        return ATTR_VALUE_TRUE.equals(getAttribute(ATTR_IS_EXECUTOR_VALUE));
    }

    @Override
    @Transient
    public Object getVariableNewValue() {
        return CurrentAndArchiveCommons.variableLog_getVariableNewValue(this);
    }

    @Override
    @Transient
    public Object getVariableNewValueForPattern() {
        return CurrentAndArchiveCommons.variableLog_getVariableNewValueForPattern(this);
    }

    @Transient
    public CurrentVariableLog getContentCopy() {
        CurrentVariableLog copyLog;
        if (this instanceof CurrentVariableCreateLog) {
            copyLog = new CurrentVariableCreateLog();
        } else if (this instanceof CurrentVariableUpdateLog) {
            copyLog = new CurrentVariableUpdateLog();
        } else if (this instanceof CurrentVariableDeleteLog) {
            copyLog = new CurrentVariableDeleteLog();
        } else {
            throw new InternalApplicationException("Unexpected " + this);
        }
        copyLog.setBytes(getBytes());
        copyLog.setContent(getContent());
        return copyLog;
    }
}
