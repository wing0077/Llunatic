package it.unibas.lunatic.model.chase.chasemc.operators.dbms;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.utility.LunaticUtility;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.commons.ChaseStats;
import it.unibas.lunatic.model.chase.commons.ChaseUtility;
import it.unibas.lunatic.model.chase.chasemc.operators.AbstractBuildDeltaDB;
import it.unibas.lunatic.model.database.Attribute;
import it.unibas.lunatic.model.database.AttributeRef;
import it.unibas.lunatic.model.database.IDatabase;
import it.unibas.lunatic.model.database.dbms.DBMSDB;
import it.unibas.lunatic.model.database.dbms.DBMSTable;
import it.unibas.lunatic.persistence.relational.AccessConfiguration;
import it.unibas.lunatic.persistence.relational.DBMSUtility;
import it.unibas.lunatic.persistence.relational.QueryManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildSQLDeltaDB extends AbstractBuildDeltaDB {

    private static Logger logger = LoggerFactory.getLogger(BuildSQLDeltaDB.class);

    public DBMSDB generate(IDatabase database, Scenario scenario, String rootName) {
        long start = new Date().getTime();
        AccessConfiguration accessConfiguration = ((DBMSDB) database).getAccessConfiguration().clone();
        accessConfiguration.setSchemaName(LunaticConstants.WORK_SCHEMA);
        DBMSDB deltaDB = new DBMSDB(accessConfiguration);
        DBMSUtility.createWorkSchema(accessConfiguration);
        StringBuilder script = new StringBuilder();
        script.append(createOccurrencesAndProvenancesTable(accessConfiguration));
        List<AttributeRef> affectedAttributes = findAllAffectedAttributes(scenario);
        script.append(createDeltaRelationsSchema(database, accessConfiguration, affectedAttributes));
        script.append(insertIntoDeltaRelations(database, accessConfiguration, rootName, affectedAttributes));
        QueryManager.executeScript(script.toString(), accessConfiguration, true, true, true);
        long end = new Date().getTime();
        ChaseStats.getInstance().addStat(ChaseStats.DELTA_DB_BUILDER, end - start);
        return deltaDB;
    }

    private String createOccurrencesAndProvenancesTable(AccessConfiguration accessConfiguration) {
        StringBuilder script = new StringBuilder();
        script.append("----- Generating occurrences tables -----\n");
        script.append("CREATE TABLE ").append(accessConfiguration.getSchemaName()).append(".").append(LunaticConstants.CELLGROUP_TABLE).append("(").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.STEP).append(" text,").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.GROUP_ID).append(" text,").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.CELL_OID).append(" bigint,").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.CELL_TABLE).append(" text,").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.CELL_ATTRIBUTE).append(" text,").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.CELL_ORIGINAL_VALUE).append(" text,").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.CELL_TYPE).append(" text").append("\n");
        script.append(") WITH OIDS;").append("\n\n");
        if (logger.isDebugEnabled()) logger.debug("----Generating occurrences tables: " + script);
        return script.toString();
    }

    private String createDeltaRelationsSchema(IDatabase database, AccessConfiguration accessConfiguration, List<AttributeRef> affectedAttributes) {
        String deltaDBSchema = accessConfiguration.getSchemaName();
        StringBuilder script = new StringBuilder();
        script.append("----- Generating Delta Relations Schema -----\n");
        for (String tableName : database.getTableNames()) {
            DBMSTable table = (DBMSTable) database.getTable(tableName);
            List<Attribute> tableNonAffectedAttributes = new ArrayList<Attribute>();
            for (Attribute attribute : table.getAttributes()) {
                if (attribute.getName().equals(LunaticConstants.OID)) {
                    continue;
                }
                if (isAffected(new AttributeRef(table.getName(), attribute.getName()), affectedAttributes)) {
                    script.append(createDeltaRelationSchemaAndTrigger(deltaDBSchema, table.getName(), attribute.getName(), attribute.getType()));
                } else {
                    tableNonAffectedAttributes.add(attribute);
                }
            }
            script.append(createTableForNonAffected(deltaDBSchema, table.getName(), tableNonAffectedAttributes));
        }
        if (logger.isDebugEnabled()) logger.debug("\n----Generating Delta Relations Schema: " + script);
        return script.toString();
    }

    private String createDeltaRelationSchemaAndTrigger(String deltaDBSchema, String tableName, String attributeName, String attributeType) {
        StringBuilder script = new StringBuilder();
        String deltaRelationName = ChaseUtility.getDeltaRelationName(tableName, attributeName);
        script.append("CREATE TABLE ").append(deltaDBSchema).append(".").append(deltaRelationName).append("(").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.STEP).append(" text,").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.TID).append(" bigint,").append("\n");
        script.append(LunaticConstants.INDENT).append(attributeName).append(" ").append(DBMSUtility.convertDataSourceTypeToDBType(attributeType)).append(",").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.CELL_ORIGINAL_VALUE).append(" text,").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.GROUP_ID).append(" text").append("\n");
        script.append(") WITH OIDS;").append("\n\n");
//        script.append("CREATE INDEX ").append(attributeName).append("_oid  ON ").append(deltaDBSchema).append(".").append(deltaRelationName).append(" USING btree(tid ASC);\n");
//        script.append("CREATE INDEX ").append(attributeName).append("_step  ON ").append(deltaDBSchema).append(".").append(deltaRelationName).append(" USING btree(step ASC);\n\n");
//        script.append("REINDEX TABLE ").append(deltaDBSchema).append(".").append(deltaRelationName).append(";\n");
        script.append(generateTriggerFunction(deltaDBSchema, tableName, attributeName));
        script.append(addTriggerToTable(deltaDBSchema, tableName, attributeName));
        return script.toString();
    }

    private String createTableForNonAffected(String deltaDBSchema, String tableName, List<Attribute> tableNonAffectedAttributes) {
        String deltaRelationName = tableName + LunaticConstants.NA_TABLE_SUFFIX;
        StringBuilder script = new StringBuilder();
        script.append("CREATE TABLE ").append(deltaDBSchema).append(".").append(deltaRelationName).append("(").append("\n");
        script.append(LunaticConstants.INDENT).append(LunaticConstants.TID).append(" bigint,").append("\n");
        for (Attribute attribute : tableNonAffectedAttributes) {
            script.append(LunaticConstants.INDENT).append(attribute.getName()).append(" ").append(DBMSUtility.convertDataSourceTypeToDBType(attribute.getType())).append(",\n");
        }
        LunaticUtility.removeChars(",\n".length(), script);
        script.append("\n").append(") WITH OIDS;").append("\n\n");
//        script.append("CREATE INDEX ").append(deltaRelationName).append("_oid  ON ").append(deltaDBSchema).append(".").append(deltaRelationName).append(" USING btree(tid ASC);\n");
        return script.toString();
    }

    private String insertIntoDeltaRelations(IDatabase database, AccessConfiguration accessConfiguration, String rootStepId, List<AttributeRef> affectedAttributes) {
        String originalDBSchema = ((DBMSDB) database).getAccessConfiguration().getSchemaName();
        String deltaDBSchema = accessConfiguration.getSchemaName();
        StringBuilder script = new StringBuilder();
        script.append("----- Insert into Delta Relations -----\n");
        for (String tableName : database.getTableNames()) {
            DBMSTable table = (DBMSTable) database.getTable(tableName);
            List<Attribute> tableNonAffectedAttributes = new ArrayList<Attribute>();
            for (Attribute attribute : table.getAttributes()) {
                if (attribute.getName().equals(LunaticConstants.OID)) {
                    continue;
                }
                if (isAffected(new AttributeRef(table.getName(), attribute.getName()), affectedAttributes)) {
                    script.append(insertIntoDeltaRelation(originalDBSchema, deltaDBSchema, table.getName(), attribute.getName(), rootStepId));
                } else {
                    tableNonAffectedAttributes.add(attribute);
                }
            }
            script.append(insertIntoNonAffectedRelation(originalDBSchema, deltaDBSchema, table.getName(), tableNonAffectedAttributes));
        }
        if (logger.isDebugEnabled()) logger.debug("----Insert into Delta Relations: " + script);
        return script.toString();
    }

    private String insertIntoDeltaRelation(String originalDBSchema, String deltaDBSchema, String tableName, String attributeName, String rootStepId) {
        StringBuilder script = new StringBuilder();
        String deltaRelationName = ChaseUtility.getDeltaRelationName(tableName, attributeName);
        //NOTE: Insert is done from select. To initalize cellGroupId and original value, we need to use the trigger
        script.append("INSERT INTO ").append(deltaDBSchema).append(".").append(deltaRelationName).append("\n");
        script.append("SELECT cast('").append(rootStepId).append("' AS varchar) AS step, " + LunaticConstants.OID + ", ").append(attributeName);
        script.append("\n").append(LunaticConstants.INDENT);
        script.append("FROM ").append(originalDBSchema).append(".").append(tableName).append(";");
        script.append("\n");
        return script.toString();
    }

    private String insertIntoNonAffectedRelation(String originalDBSchema, String deltaDBSchema, String tableName, List<Attribute> tableNonAffectedAttributes) {
        String deltaRelationName = tableName + LunaticConstants.NA_TABLE_SUFFIX;
        StringBuilder script = new StringBuilder();
        script.append("INSERT INTO ").append(deltaDBSchema).append(".").append(deltaRelationName).append("\n");
        script.append("SELECT ").append(LunaticConstants.OID + ",");
        for (Attribute attribute : tableNonAffectedAttributes) {
            script.append(attribute.getName()).append(",");
        }
        LunaticUtility.removeChars(",".length(), script);
        script.append("\n").append(LunaticConstants.INDENT);
        script.append("FROM ").append(originalDBSchema).append(".").append(tableName).append(";");
        script.append("\n");
        return script.toString();
    }

    private String addTriggerToTable(String deltaDBSchema, String tableName, String attributeName) {
        StringBuilder result = new StringBuilder();
        String deltaRelationName = ChaseUtility.getDeltaRelationName(tableName, attributeName);
        result.append("DROP TRIGGER IF EXISTS trigg_update_occurrences_").append(deltaRelationName);
        result.append(" ON ").append(deltaDBSchema).append(".").append(deltaRelationName).append(";").append("\n\n");
        result.append("CREATE TRIGGER trigg_update_occurrences_").append(deltaRelationName).append("\n");
        result.append("BEFORE INSERT OR UPDATE OF ").append(attributeName);
        result.append(" OR DELETE ON ").append(deltaDBSchema).append(".").append(deltaRelationName).append("\n");
        result.append("FOR EACH ROW EXECUTE PROCEDURE ").append(deltaDBSchema).append(".update_occurrences_");
        result.append(deltaRelationName).append("();").append("\n\n");
        return result.toString();
    }

    private String generateTriggerFunction(String deltaDBSchema, String tableName, String attributeName) {
        StringBuilder result = new StringBuilder();
        String deltaRelationName = ChaseUtility.getDeltaRelationName(tableName, attributeName);
        result.append("CREATE OR REPLACE FUNCTION ").append(deltaDBSchema).append(".update_occurrences_");
        result.append(deltaRelationName).append("() RETURNS TRIGGER AS $$").append("\n");
        result.append(LunaticConstants.INDENT).append("BEGIN").append("\n");
        String indent = LunaticConstants.INDENT + LunaticConstants.INDENT;
        String longIndent = indent + LunaticConstants.INDENT;
        result.append(indent).append("IF (TG_OP = 'DELETE') THEN").append("\n");
        result.append(createDeletePart(deltaDBSchema, tableName, attributeName, longIndent));
        result.append(longIndent).append("RETURN OLD;").append("\n");
        result.append(indent).append("ELSIF (TG_OP = 'UPDATE') THEN").append("\n");
        result.append(longIndent).append("RETURN NEW;").append("\n");
        result.append(indent).append("ELSIF (TG_OP = 'INSERT') THEN").append("\n");
        result.append(createInsertPart(deltaDBSchema, tableName, attributeName, longIndent));
        result.append(longIndent).append("RETURN NEW;").append("\n");
        result.append(indent).append("END IF;").append("\n");
        result.append(LunaticConstants.INDENT).append("END;").append("\n");
        result.append("$$ LANGUAGE plpgsql;").append("\n\n");
        return result.toString();
    }

    private String createDeletePart(String deltaDBSchema, String tableName, String attributeName, String indent) {
        StringBuilder result = new StringBuilder();
        String longIndent = indent + LunaticConstants.INDENT;
        result.append(longIndent).append("DELETE FROM ");
        result.append(deltaDBSchema).append(".").append(LunaticConstants.CELLGROUP_TABLE).append(" WHERE ");
        result.append(LunaticConstants.STEP).append(" = ");
        result.append("OLD.").append(LunaticConstants.STEP).append(" AND ");
        result.append(LunaticConstants.CELL_OID).append(" = ");
        result.append("OLD.").append(LunaticConstants.TID).append(" AND ");
        result.append(LunaticConstants.CELL_TABLE).append(" = ");
        result.append("'").append(tableName).append("'").append(" AND ");
        result.append(LunaticConstants.CELL_ATTRIBUTE).append(" = ");
        result.append("'").append(attributeName).append("'").append(";\n");
        return result.toString();
    }

    private String createInsertPart(String deltaDBSchema, String tableName, String attributeName, String indent) {
        StringBuilder result = new StringBuilder();
        result.append(indent).append("NEW.").append(LunaticConstants.CELL_ORIGINAL_VALUE).append(" = NEW.").append(attributeName).append(";\n");
        //If is NULL or LLUN and the cluster id is not defined, copy its value as cluster id
//        result.append(indent).append("IF NEW.").append(LunaticConstants.GROUP_ID).append(" IS NULL AND ");
        String longIndent = indent + LunaticConstants.INDENT;
        result.append(indent).append("IF POSITION ('").append(LunaticConstants.SKOLEM_PREFIX).append("' IN NEW.").append(attributeName).append(") = 1 ");
        result.append("OR POSITION ('").append(LunaticConstants.LLUN_PREFIX).append("' IN NEW.").append(attributeName).append(") = 1 ");
        result.append("THEN").append("\n");
        result.append(longIndent).append("NEW.").append(LunaticConstants.GROUP_ID).append(" = NEW.").append(attributeName).append(";\n");
//        result.append(indent).append("ELSIF NEW.").append(LunaticConstants.GROUP_ID).append(" = '").append(LunaticConstants.GEN_GROUP_ID).append("' THEN\n");
//        result.append(longIndent).append("NEW.").append(LunaticConstants.GROUP_ID).append(" = '");
//        result.append(LunaticConstants.LLUN_PREFIX).append("' || ").append("NEW.").append(LunaticConstants.TID).append(" || '").append(attributeName);
//        result.append("' || NEW.").append(attributeName).append(";\n");
        result.append(indent).append("END IF;").append("\n");
        //Using cluster id value to update skolem table
        result.append(indent).append("IF NEW.").append(LunaticConstants.GROUP_ID).append(" IS NOT NULL THEN").append("\n");
        result.append(longIndent).append("INSERT INTO ");
        result.append(deltaDBSchema).append(".").append(LunaticConstants.CELLGROUP_TABLE).append(" VALUES(");
        result.append("NEW.").append(LunaticConstants.STEP).append(", ");
        result.append("NEW.").append(LunaticConstants.GROUP_ID).append(", ");
        result.append("NEW.").append(LunaticConstants.TID).append(", ");
        result.append("'").append(tableName).append("'").append(", ");
        result.append("'").append(attributeName).append("'").append(", ");
        result.append("NEW.").append(LunaticConstants.CELL_ORIGINAL_VALUE).append(", ");
        result.append("'").append(LunaticConstants.TYPE_OCCURRENCE).append("'").append(");\n");
        result.append(indent).append("END IF;").append("\n");
        return result.toString();
    }
}
