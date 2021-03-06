package it.unibas.lunatic.model.chase.chasemc.operators.dbms;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.utility.LunaticUtility;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.commons.ChaseStats;
import it.unibas.lunatic.model.chase.commons.operators.ChaseUtility;
import it.unibas.lunatic.model.chase.chasemc.operators.AbstractBuildDeltaDB;
import speedy.model.database.Attribute;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.dbms.DBMSTable;
import it.unibas.lunatic.persistence.relational.LunaticDBMSUtility;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.SpeedyConstants;
import speedy.persistence.relational.AccessConfiguration;
import speedy.persistence.relational.QueryManager;
import speedy.utility.DBMSUtility;
import speedy.utility.SpeedyUtility;

public class BuildSQLDeltaDB extends AbstractBuildDeltaDB {

    private static Logger logger = LoggerFactory.getLogger(BuildSQLDeltaDB.class);

    public DBMSDB generate(IDatabase database, Scenario scenario, String rootName) {
        long start = new Date().getTime();
        AccessConfiguration accessConfiguration = ((DBMSDB) database).getAccessConfiguration().clone();
        accessConfiguration.setSchemaName(SpeedyConstants.WORK_SCHEMA);
        DBMSDB deltaDB = new DBMSDB(accessConfiguration);
        LunaticDBMSUtility.createWorkSchema(accessConfiguration, scenario);
        StringBuilder script = new StringBuilder();
        script.append(createCellGroupTable(accessConfiguration, scenario));
        Set<AttributeRef> affectedAttributes = findAllAffectedAttributes(scenario);
        if (logger.isDebugEnabled()) logger.debug("Affected attributes " + affectedAttributes);
        List<String> deltaTables = new ArrayList<String>();
        script.append(createDeltaRelationsSchema(database, deltaTables, accessConfiguration, affectedAttributes, scenario));
        script.append(insertIntoDeltaRelations(database, accessConfiguration, rootName, affectedAttributes, scenario));
        QueryManager.executeScript(script.toString(), accessConfiguration, true, true, true, false);
        analyzeDeltaTables(deltaTables, accessConfiguration);
        long end = new Date().getTime();
        ChaseStats.getInstance().addStat(ChaseStats.DELTA_DB_BUILDER, end - start);
        return deltaDB;
    }

    private void analyzeDeltaTables(List<String> deltaTables, AccessConfiguration accessConfiguration) {
        QueryManager.executeScript("ANALYZE " + DBMSUtility.getSchemaNameAndDot(accessConfiguration) + LunaticConstants.CELLGROUP_TABLE, accessConfiguration, true, true, true, false);
        for (String tableName : deltaTables) {
            QueryManager.executeScript("ANALYZE " + tableName, accessConfiguration, true, true, true, false);
        }
    }

    private String createCellGroupTable(AccessConfiguration accessConfiguration, Scenario scenario) {
        StringBuilder script = new StringBuilder();
        script.append("----- Generating cell group table -----\n");
        String unloggedOption = (scenario.getConfiguration().isUseUnloggedWorkTables() ? " UNLOGGED " : "");
        script.append("CREATE ").append(unloggedOption).append(" TABLE ").append(DBMSUtility.getSchemaNameAndDot(accessConfiguration)).append(LunaticConstants.CELLGROUP_TABLE).append("(").append("\n");
        script.append(SpeedyConstants.INDENT).append(SpeedyConstants.STEP).append(" text,").append("\n");
        String valueType = " text,";
        if (scenario.getConfiguration().isUseDictionaryEncoding()) {
            valueType = " bigint,";
        }
        script.append(SpeedyConstants.INDENT).append(LunaticConstants.GROUP_ID).append(valueType).append("\n");
        script.append(SpeedyConstants.INDENT).append(LunaticConstants.CELL_OID).append(" bigint,").append("\n");
        script.append(SpeedyConstants.INDENT).append(LunaticConstants.CELL_TABLE).append(" text,").append("\n");
        script.append(SpeedyConstants.INDENT).append(LunaticConstants.CELL_ATTRIBUTE).append(" text,").append("\n");
        script.append(SpeedyConstants.INDENT).append(LunaticConstants.CELL_ORIGINAL_VALUE).append(" text,").append("\n");
        script.append(SpeedyConstants.INDENT).append(LunaticConstants.CELL_TYPE).append(" text").append("\n");
        script.append(") WITH OIDS;").append("\n\n");
        if (logger.isDebugEnabled()) logger.debug("----Generating occurrences tables: " + script);
        return script.toString();
    }

    private String createDeltaRelationsSchema(IDatabase database, List<String> deltaTables, AccessConfiguration accessConfiguration, Set<AttributeRef> affectedAttributes, Scenario scenario) {
        String deltaDBSchema = LunaticDBMSUtility.getSchemaWithSuffix(accessConfiguration, scenario);
        StringBuilder script = new StringBuilder();
        script.append("----- Generating Delta Relations Schema -----\n");
        for (String tableName : database.getTableNames()) {
            DBMSTable table = (DBMSTable) database.getTable(tableName);
            List<Attribute> tableNonAffectedAttributes = new ArrayList<Attribute>();
            for (Attribute attribute : table.getAttributes()) {
                if (attribute.getName().equals(SpeedyConstants.OID)) {
                    continue;
                }
                if (isAffected(new AttributeRef(table.getName(), attribute.getName()), affectedAttributes)) {
                    script.append(createDeltaRelationSchemaAndTrigger(deltaDBSchema, deltaTables, table.getName(), attribute.getName(), attribute.getType(), scenario));
                } else {
                    tableNonAffectedAttributes.add(attribute);
                }
            }
            if (!tableNonAffectedAttributes.isEmpty()) {
                script.append(createTableForNonAffected(deltaDBSchema, deltaTables, table.getName(), tableNonAffectedAttributes, scenario));
            }
        }
        if (logger.isDebugEnabled()) logger.debug("\n----Generating Delta Relations Schema: " + script);
        return script.toString();
    }

    private String createDeltaRelationSchemaAndTrigger(String deltaDBSchema, List<String> deltaTables, String tableName, String attributeName, String attributeType, Scenario scenario) {
        StringBuilder script = new StringBuilder();
        String deltaRelationName = ChaseUtility.getDeltaRelationName(tableName, attributeName);
        String schemaAndTable = deltaDBSchema + "." + deltaRelationName;
        deltaTables.add(schemaAndTable);
        String unloggedOption = (scenario.getConfiguration().isUseUnloggedWorkTables() ? " UNLOGGED " : "");
        script.append("CREATE ").append(unloggedOption).append(" TABLE ").append(schemaAndTable).append("(").append("\n");
        script.append(SpeedyConstants.INDENT).append(SpeedyConstants.STEP).append(" text,").append("\n");
        script.append(SpeedyConstants.INDENT).append(SpeedyConstants.TID).append(" bigint,").append("\n");
        script.append(SpeedyConstants.INDENT).append(attributeName).append(" ").append(LunaticDBMSUtility.convertDataSourceTypeToDBType(attributeType, scenario.getConfiguration())).append(",").append("\n");
        script.append(SpeedyConstants.INDENT).append(LunaticConstants.CELL_ORIGINAL_VALUE).append(" text,").append("\n");
        String idType = " text";
        if (scenario.getConfiguration().isUseDictionaryEncoding()) {
            idType = " bigint";
        }
        script.append(SpeedyConstants.INDENT).append(LunaticConstants.GROUP_ID).append(idType).append("\n");
        script.append(") WITH OIDS;").append("\n\n");
//        script.append("CREATE INDEX ").append(attributeName).append("_oid  ON ").append(deltaDBSchema).append(".").append(deltaRelationName).append(" USING btree(tid ASC);\n");
//        script.append("CREATE INDEX ").append(attributeName).append("_step  ON ").append(deltaDBSchema).append(".").append(deltaRelationName).append(" USING btree(step ASC);\n\n");
//        script.append("REINDEX TABLE ").append(deltaDBSchema).append(".").append(deltaRelationName).append(";\n");
        script.append(generateTriggerFunction(deltaDBSchema, tableName, attributeName, attributeType));
        script.append(addTriggerToTable(deltaDBSchema, tableName, attributeName));
        return script.toString();
    }

    private String createTableForNonAffected(String deltaDBSchema, List<String> deltaTables, String tableName, List<Attribute> tableNonAffectedAttributes, Scenario scenario) {
        String deltaRelationName = tableName + LunaticConstants.NA_TABLE_SUFFIX;
        String schemaAndTable = deltaDBSchema + "." + deltaRelationName;
        deltaTables.add(schemaAndTable);
        StringBuilder script = new StringBuilder();
        String unloggedOption = (scenario.getConfiguration().isUseUnloggedWorkTables() ? " UNLOGGED " : "");
        script.append("CREATE ").append(unloggedOption).append(" TABLE ").append(schemaAndTable).append("(").append("\n");
        script.append(SpeedyConstants.INDENT).append(SpeedyConstants.TID).append(" bigint,").append("\n");
        for (Attribute attribute : tableNonAffectedAttributes) {
            script.append(SpeedyConstants.INDENT).append(attribute.getName()).append(" ").append(LunaticDBMSUtility.convertDataSourceTypeToDBType(attribute.getType(), scenario.getConfiguration())).append(",\n");
        }
        LunaticUtility.removeChars(",\n".length(), script);
        script.append("\n").append(") WITH OIDS;").append("\n\n");
//        script.append("CREATE INDEX ").append(deltaRelationName).append("_oid  ON ").append(deltaDBSchema).append(".").append(deltaRelationName).append(" USING btree(tid ASC);\n");
        return script.toString();
    }

    private String insertIntoDeltaRelations(IDatabase database, AccessConfiguration accessConfiguration, String rootStepId, Set<AttributeRef> affectedAttributes, Scenario scenario) {
        String originalDBSchema = LunaticDBMSUtility.getSchemaWithSuffix(((DBMSDB) database).getAccessConfiguration(), scenario);
        String deltaDBSchema = LunaticDBMSUtility.getSchemaWithSuffix(accessConfiguration, scenario);
        StringBuilder script = new StringBuilder();
        script.append("----- Insert into Delta Relations -----\n");
        for (String tableName : database.getTableNames()) {
            DBMSTable table = (DBMSTable) database.getTable(tableName);
            List<Attribute> tableNonAffectedAttributes = new ArrayList<Attribute>();
            for (Attribute attribute : table.getAttributes()) {
                if (attribute.getName().equals(SpeedyConstants.OID)) {
                    continue;
                }
                if (isAffected(new AttributeRef(table.getName(), attribute.getName()), affectedAttributes)) {
                    script.append(insertIntoDeltaRelation(originalDBSchema, deltaDBSchema, table.getName(), attribute.getName(), rootStepId));
                } else {
                    tableNonAffectedAttributes.add(attribute);
                }
            }
            if (!tableNonAffectedAttributes.isEmpty()) {
                script.append(insertIntoNonAffectedRelation(originalDBSchema, deltaDBSchema, table.getName(), tableNonAffectedAttributes));
            }
        }
        if (logger.isDebugEnabled()) logger.debug("----Insert into Delta Relations: " + script);
        return script.toString();
    }

    private String insertIntoDeltaRelation(String originalDBSchema, String deltaDBSchema, String tableName, String attributeName, String rootStepId) {
        StringBuilder script = new StringBuilder();
        String deltaRelationName = ChaseUtility.getDeltaRelationName(tableName, attributeName);
        //NOTE: Insert is done from select. To initalize cellGroupId and original value, we need to use the trigger
        script.append("INSERT INTO ").append(deltaDBSchema).append(".").append(deltaRelationName).append("\n");
        script.append("SELECT cast('").append(rootStepId).append("' AS varchar) AS step, ").append(SpeedyConstants.OID).append(", ").append(attributeName);
        script.append("\n").append(SpeedyConstants.INDENT);
        script.append("FROM ").append(originalDBSchema).append(".").append(tableName).append(";");
        script.append("\n");
        return script.toString();
    }

    private String insertIntoNonAffectedRelation(String originalDBSchema, String deltaDBSchema, String tableName, List<Attribute> tableNonAffectedAttributes) {
        String deltaRelationName = tableName + LunaticConstants.NA_TABLE_SUFFIX;
        StringBuilder script = new StringBuilder();
        script.append("INSERT INTO ").append(deltaDBSchema).append(".").append(deltaRelationName).append("\n");
        script.append("SELECT ").append(SpeedyConstants.OID).append(",");
        for (Attribute attribute : tableNonAffectedAttributes) {
            script.append(attribute.getName()).append(",");
        }
        LunaticUtility.removeChars(",".length(), script);
        script.append("\n").append(SpeedyConstants.INDENT);
        script.append("FROM ").append(originalDBSchema).append(".").append(tableName).append(";");
        script.append("\n");
        return script.toString();
    }

    private String addTriggerToTable(String deltaDBSchema, String tableName, String attributeName) {
        StringBuilder result = new StringBuilder();
        String deltaRelationName = ChaseUtility.getDeltaRelationName(tableName, attributeName);
        String triggerName = "trigg_update_occurrences_" + deltaRelationName;
        triggerName = DBMSUtility.cleanFunctionName(triggerName);
        String functionName = "update_occurrences_" + deltaRelationName;
        functionName = DBMSUtility.cleanFunctionName(functionName);
        result.append("DROP TRIGGER IF EXISTS ").append(triggerName);
        result.append(" ON ").append(deltaDBSchema).append(".").append(deltaRelationName).append(";").append("\n\n");
        result.append("CREATE TRIGGER ").append(triggerName).append("\n");
        result.append("BEFORE INSERT OR UPDATE OF ").append(attributeName);
        result.append(" OR DELETE ON ").append(deltaDBSchema).append(".").append(deltaRelationName).append("\n");
        result.append("FOR EACH ROW EXECUTE PROCEDURE ").append(deltaDBSchema).append(".");
        result.append(functionName).append("();").append("\n\n");
        return result.toString();
    }

    private String generateTriggerFunction(String deltaDBSchema, String tableName, String attributeName, String attributeType) {
        StringBuilder result = new StringBuilder();
        String deltaRelationName = ChaseUtility.getDeltaRelationName(tableName, attributeName);
        String functionName = "update_occurrences_" + deltaRelationName;
        functionName = DBMSUtility.cleanFunctionName(functionName);
        result.append("CREATE OR REPLACE FUNCTION ").append(deltaDBSchema).append(".");
        result.append(functionName).append("() RETURNS TRIGGER AS $$").append("\n");
        result.append(SpeedyConstants.INDENT).append("BEGIN").append("\n");
        String indent = SpeedyConstants.INDENT + SpeedyConstants.INDENT;
        String longIndent = indent + SpeedyConstants.INDENT;
        result.append(indent).append("IF (TG_OP = 'DELETE') THEN").append("\n");
        result.append(createDeletePart(deltaDBSchema, tableName, attributeName, longIndent));
        result.append(longIndent).append("RETURN OLD;").append("\n");
        result.append(indent).append("ELSIF (TG_OP = 'UPDATE') THEN").append("\n");
        result.append(longIndent).append("RETURN NEW;").append("\n");
        result.append(indent).append("ELSIF (TG_OP = 'INSERT') THEN").append("\n");
        result.append(createInsertPart(deltaDBSchema, tableName, attributeName, attributeType, longIndent));
        result.append(longIndent).append("RETURN NEW;").append("\n");
        result.append(indent).append("END IF;").append("\n");
        result.append(SpeedyConstants.INDENT).append("END;").append("\n");
        result.append("$$ LANGUAGE plpgsql;").append("\n\n");
        return result.toString();
    }

    private String createDeletePart(String deltaDBSchema, String tableName, String attributeName, String indent) {
        StringBuilder result = new StringBuilder();
        String longIndent = indent + SpeedyConstants.INDENT;
        result.append(longIndent).append("DELETE FROM ");
        result.append(deltaDBSchema).append(".").append(LunaticConstants.CELLGROUP_TABLE).append(" WHERE ");
        result.append(SpeedyConstants.STEP).append(" = ");
        result.append("OLD.").append(SpeedyConstants.STEP).append(" AND ");
        result.append(LunaticConstants.CELL_OID).append(" = ");
        result.append("OLD.").append(SpeedyConstants.TID).append(" AND ");
        result.append(LunaticConstants.CELL_TABLE).append(" = ");
        result.append("'").append(tableName).append("'").append(" AND ");
        result.append(LunaticConstants.CELL_ATTRIBUTE).append(" = ");
        result.append("'").append(attributeName).append("'").append(";\n");
        return result.toString();
    }

    private String createInsertPart(String deltaDBSchema, String tableName, String attributeName, String attributeType, String indent) {
        StringBuilder result = new StringBuilder();
        //If is NULL or LLUN and the cluster id is not defined, copy its value as cluster id
        String longIndent = indent + SpeedyConstants.INDENT;
        result.append(indent).append("IF POSITION ('").append(SpeedyConstants.SKOLEM_PREFIX).append("' IN cast(NEW.").append(attributeName).append(" as varchar)) = 1 ");
        result.append("OR POSITION ('").append(SpeedyConstants.LLUN_PREFIX).append("' IN cast(NEW.").append(attributeName).append(" as varchar) ) = 1 ");
        if (SpeedyUtility.isBigInt(attributeType)) {
            result.append(indent).append("OR (POSITION ('").append(SpeedyConstants.BIGINT_SKOLEM_PREFIX).append("' IN cast(NEW.").append(attributeName).append(" as varchar)) = 1 ");
            result.append(indent).append("   AND LENGTH(cast(NEW.").append(attributeName).append(" as varchar)) > ").append(SpeedyConstants.MIN_LENGTH_FOR_NUMERIC_PLACEHOLDERS).append(" )");
        }
        if (SpeedyUtility.isDoublePrecision(attributeType)) {
            result.append(indent).append("OR (POSITION ('").append(SpeedyConstants.DOUBLE_SKOLEM_PREFIX).append("' IN cast(NEW.").append(attributeName).append(" as varchar)) = 1 ");
            result.append(indent).append("   AND LENGTH(cast(NEW.").append(attributeName).append(" as varchar)) > ").append(SpeedyConstants.MIN_LENGTH_FOR_NUMERIC_PLACEHOLDERS).append(" )");
        }
        result.append("THEN").append("\n");
        result.append(longIndent).append("NEW.").append(LunaticConstants.GROUP_ID).append(" = NEW.").append(attributeName).append(";\n");
        result.append(indent).append("END IF;").append("\n");
        //Using cluster id value to update skolem table
        result.append(indent).append("IF NEW.").append(LunaticConstants.GROUP_ID).append(" IS NOT NULL THEN").append("\n");
        result.append(longIndent).append("INSERT INTO ");
        result.append(deltaDBSchema).append(".").append(LunaticConstants.CELLGROUP_TABLE).append(" VALUES(");
        result.append("NEW.").append(SpeedyConstants.STEP).append(", ");
        result.append("NEW.").append(LunaticConstants.GROUP_ID).append(", ");
        result.append("NEW.").append(SpeedyConstants.TID).append(", ");
        result.append("'").append(tableName).append("'").append(", ");
        result.append("'").append(attributeName).append("'").append(", ");
        result.append("NEW.").append(LunaticConstants.CELL_ORIGINAL_VALUE).append(", ");
        result.append("'").append(LunaticConstants.TYPE_OCCURRENCE).append("'").append(");\n");
        result.append(indent).append("END IF;").append("\n");
        return result.toString();
    }
}
