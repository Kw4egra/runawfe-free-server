package ru.runa.wfe.definition;

import java.util.Date;
import ru.runa.wfe.presentation.BatchPresentationConsts;
import ru.runa.wfe.presentation.ClassPresentation;
import ru.runa.wfe.presentation.DefaultDbSource;
import ru.runa.wfe.presentation.FieldDescriptor;
import ru.runa.wfe.presentation.FieldFilterMode;
import ru.runa.wfe.presentation.filter.StringWithEmptyValueFilterCriteria;
import ru.runa.wfe.security.Permission;
import ru.runa.wfe.user.Actor;

/**
 * Class presentation for process definition. Shows only last deployed process definition.
 *
 * @author Dofs
 * @since 4.0
 */
public class DefinitionClassPresentation extends ClassPresentation {
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String VERSION = "version";
    public static final String TYPE = "type";
    public static final String CREATE_DATE = "createDate";
    public static final String CREATE_ACTOR = "createActor";
    public static final String UPDATE_DATE = "updateDate";
    public static final String UPDATE_ACTOR = "updateActor";
    public static final String SUBPROCESS_BINDING_DATE = "subprocessBindingDate";

    public static final ClassPresentation INSTANCE = new DefinitionClassPresentation();

    private DefinitionClassPresentation() {
        super(ProcessDefinitionPack.class, null, false, new FieldDescriptor[] {
                // display name field type DB source isSort filter mode
                // get value/show in web getter parameters
                new FieldDescriptor(NAME, String.class.getName(), new DefaultDbSource(ProcessDefinitionPack.class, "name"), true, 1,
                        BatchPresentationConsts.ASC, FieldFilterMode.DATABASE, "ru.runa.common.web.html.PropertyTdBuilder", new Object[] {
                        Permission.START_PROCESS, "name" }),
                new FieldDescriptor(DESCRIPTION, String.class.getName(), new DefaultDbSource(ProcessDefinitionPack.class, "description"), true,
                        FieldFilterMode.DATABASE, "ru.runa.wf.web.html.DescriptionProcessTdBuilder", new Object[] {}),
                new FieldDescriptor(TYPE, StringWithEmptyValueFilterCriteria.class.getName(), new DefaultDbSource(ProcessDefinitionPack.class, "category"),
                        true, FieldFilterMode.DATABASE, "ru.runa.wf.web.html.CategoryTdBuilder", new Object[] {}),
                new FieldDescriptor(CREATE_DATE, Date.class.getName(), new DefaultDbSource(ProcessDefinitionPack.class, "latest.createDate"),
                        true, FieldFilterMode.DATABASE, "ru.runa.wf.web.html.DefinitionCreateDateTdBuilder", new Object[] {}),
                new FieldDescriptor(CREATE_ACTOR, Actor.class.getName(), new DefaultDbSource(ProcessDefinitionPack.class, "latest.createActor"),
                        false, FieldFilterMode.NONE, "ru.runa.wf.web.html.DefinitionCreateActorTdBuilder", new Object[] {}),
                new FieldDescriptor(UPDATE_DATE, Date.class.getName(), new DefaultDbSource(ProcessDefinitionPack.class, "latest.updateDate"),
                        true, FieldFilterMode.DATABASE, "ru.runa.wf.web.html.DefinitionUpdateDateTdBuilder", new Object[] {}),
                new FieldDescriptor(UPDATE_ACTOR, Actor.class.getName(), new DefaultDbSource(ProcessDefinitionPack.class, "latest.updateActor"),
                        false, FieldFilterMode.NONE, "ru.runa.wf.web.html.DefinitionUpdateActorTdBuilder", new Object[] {}),
                new FieldDescriptor(SUBPROCESS_BINDING_DATE, Date.class.getName(), new DefaultDbSource(ProcessDefinitionPack.class,
                        "latest.subprocessBindingDate"), true, FieldFilterMode.DATABASE,
                        "ru.runa.wf.web.html.DefinitionSubprocessBindingDateTdBuilder", new Object[] {}).setVisible(false),
                new FieldDescriptor(VERSION, Integer.class.getName(), new DefaultDbSource(ProcessDefinitionPack.class, "latest.version"), true,
                        FieldFilterMode.DATABASE, "ru.runa.common.web.html.PropertyTdBuilder", new Object[] { Permission.READ, "version" })
                        .setVisible(false)
        });
    }
}
