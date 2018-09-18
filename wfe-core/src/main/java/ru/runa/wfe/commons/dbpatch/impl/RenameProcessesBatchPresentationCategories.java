package ru.runa.wfe.commons.dbpatch.impl;

import java.sql.Connection;
import java.util.ArrayList;
import lombok.val;
import ru.runa.wfe.commons.dbpatch.DbPatch;
import ru.runa.wfe.util.Pair;

/**
 * See TNMS #5204.
 */
public class RenameProcessesBatchPresentationCategories extends DbPatch {

    @Override
    public void executeDML(Connection conn) throws Exception {
        val replacements = new ArrayList<Pair<String,String>>() {{
            add(new Pair<>("listProcessesForm", "listCurrentProcessesForm"));
            add(new Pair<>("listProcessesWithTasksForm", "listCurrentProcessesWithTasksForm"));
        }};

        try (val stmt = conn.prepareStatement("update batch_presentation set category = ? where category = ?")) {
            for (val r : replacements) {
                stmt.setString(1, r.getValue2());
                stmt.setString(2, r.getValue1());
                stmt.executeUpdate();
            }
        }
    }
}
