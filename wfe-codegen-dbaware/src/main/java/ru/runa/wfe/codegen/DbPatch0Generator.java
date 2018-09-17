package ru.runa.wfe.codegen;

import java.io.File;
import java.io.FileWriter;
import lombok.val;
import lombok.var;
import ru.runa.wfe.codegen.DbStructureAnalyzer.Structure;

class DbPatch0Generator {

    static void generate(Structure st, File f) throws Exception {
        val w = new FileWriter(f);
        w.write("//\n" +
                "// ATTENTION!!! This file is auto-generated by wfe-codegen-dbaware subproject. See README.txt there.\n" +
                "//\n" +
                "package ru.runa.wfe.commons.dbpatch;\n" +
                "\n" +
                "import com.google.common.collect.ImmutableList;\n" +
                "import java.sql.Connection;\n" +
                "import java.util.List;\n" +
                "\n" +
                "public class DbPatch0 extends DbPatch {\n" +
                "\n" +
                "    @Override\n" +
                "    protected List<String> getDDLQueriesBefore() {\n" +
                "        return ImmutableList.of(\n");

        for (val t : st.tables) {
            w.write("                getDDLCreateTable(\"" + t.name + "\", ImmutableList.of(");
            var firstColumn = true;
            for (val c : t.columns) {
                if (firstColumn) {
                    firstColumn = false;
                    w.write("\n");
                } else {
                    w.write(",\n");
                }
                w.write("                        new ");

                val quotedName = "\"" + c.name + "\"";
                val allowNulls = c.isNotNull ? "false" : "true";
                switch (c.type) {
                    case BIGINT:
                        w.write("BigintColumnDef(" + quotedName + ", " + allowNulls + ")");
                        break;
                    case BLOB:
                        w.write("BlobColumnDef(" + quotedName + ", " + allowNulls + ")");
                        break;
                    case BOOLEAN:
                        w.write("BooleanColumnDef(" + quotedName + ", " + allowNulls + ")");
                        break;
                    case CHAR:
                        w.write("CharColumnDef(" + quotedName + ", " + c.typeLength + ", " + allowNulls + ")");
                        break;
                    case DOUBLE:
                        w.write("DoubleColumnDef(" + quotedName + ", " + allowNulls + ")");
                        break;
                    case INT:
                        w.write("IntColumnDef(" + quotedName + ", " + allowNulls + ")");
                        break;
                    case TIMESTAMP:
                        w.write("TimestampColumnDef(" + quotedName + ", " + allowNulls + ")");
                        break;
                    case VARCHAR:
                        w.write("VarcharColumnDef(" + quotedName + ", " + c.typeLength + ", " + allowNulls + ")");
                        break;
                    default:
                        throw new Exception("Internal error: unhandled column type " + c.type);
                }
                if (c.isPrimaryKey) {
                    w.write(".setPrimaryKey()");
                }
            }
            w.write("\n" +
                    "                )),\n");
        }  // tables

        for (val uk : st.uniqueKeys) {
            w.write("                getDDLCreateUniqueKey(\"" + uk.table.name + "\", \"" + uk.name + "\"");
            for (val c : uk.columns) {
                w.write(", \"" + c.name + "\"");
            }
            w.write("),\n");
        }  // uniqueKeys

        for (val fk : st.foreignKeys) {
            // TODO ...
        }


        w.write("                null\n" +
                "        );\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void executeDML(Connection conn) throws Exception {\n" +
                "    }\n" +
                "}\n");
        w.close();
    }
}
