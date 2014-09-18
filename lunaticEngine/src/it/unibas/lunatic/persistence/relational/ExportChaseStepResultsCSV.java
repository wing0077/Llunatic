package it.unibas.lunatic.persistence.relational;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.exceptions.DAOException;
import it.unibas.lunatic.model.algebra.Distinct;
import it.unibas.lunatic.model.algebra.IAlgebraOperator;
import it.unibas.lunatic.model.algebra.Project;
import it.unibas.lunatic.model.algebra.Scan;
import it.unibas.lunatic.model.algebra.operators.BuildAlgebraTreeForTGD;
import it.unibas.lunatic.model.algebra.operators.ITupleIterator;
import it.unibas.lunatic.model.algebra.sql.AlgebraTreeToSQL;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.model.chase.chasemc.operators.IBuildDatabaseForChaseStep;
import it.unibas.lunatic.model.chase.chasemc.operators.IRunQuery;
import it.unibas.lunatic.model.database.Attribute;
import it.unibas.lunatic.model.database.AttributeRef;
import it.unibas.lunatic.model.database.Cell;
import it.unibas.lunatic.model.database.IDatabase;
import it.unibas.lunatic.model.database.ITable;
import it.unibas.lunatic.model.database.TableAlias;
import it.unibas.lunatic.model.database.Tuple;
import it.unibas.lunatic.model.database.dbms.DBMSDB;
import it.unibas.lunatic.model.database.dbms.DBMSTable;
import it.unibas.lunatic.model.database.dbms.DBMSVirtualDB;
import it.unibas.lunatic.model.database.mainmemory.MainMemoryVirtualDB;
import it.unibas.lunatic.model.database.mainmemory.MainMemoryVirtualTable;
import it.unibas.lunatic.model.dependency.Dependency;
import it.unibas.lunatic.model.dependency.IFormulaAtom;
import it.unibas.lunatic.model.dependency.RelationalAtom;
import it.unibas.lunatic.utility.LunaticUtility;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportChaseStepResultsCSV {

    private IBuildDatabaseForChaseStep databaseBuilder;
    private IRunQuery queryRunner;
    private String CSV_SEPARATOR = ",";
    private String tablePrefix = "#";
    private int counter = 0;

    public ExportChaseStepResultsCSV(IBuildDatabaseForChaseStep databaseBuilder, IRunQuery queryRunner) {
        this.databaseBuilder = databaseBuilder;
        this.queryRunner = queryRunner;
    }

    public List<String> exportResult(DeltaChaseStep result, String folder, boolean materializeFKJoins) throws DAOException {
        counter = 0;
        if (!folder.endsWith(File.separator)) {
            folder = folder + File.separator;
        }
        File fileFolder = new File(folder);
        fileFolder.mkdirs();
        List<String> resultFiles = new ArrayList<String>();
        exportSolutions(result, folder, resultFiles, materializeFKJoins);
        return resultFiles;
    }

    private void exportSolutions(DeltaChaseStep step, String folder, List<String> results, boolean materializeFKJoins) {
        if (step.isLeaf()) {
            if (step.isDuplicate() || step.isInvalid()) {
                return;
            }
            counter++;
            String resultFile = folder + "Solution" + (counter < 10 ? "0" + counter : counter) + ".csv";
            IDatabase database = databaseBuilder.extractDatabaseWithDistinct(step.getId(), step.getDeltaDB(), step.getOriginalDB());
            if (materializeFKJoins) {
                materializeFKJoins(database, step);
            }
            exportDatabase(database, resultFile);
            results.add(resultFile);
        } else {
            for (DeltaChaseStep child : step.getChildren()) {
                exportSolutions(child, folder, results, materializeFKJoins);
            }
        }
    }

    public void exportDatabase(DeltaChaseStep step, String stepId, String file) throws DAOException {
        IDatabase database = databaseBuilder.extractDatabaseWithDistinct(stepId, step.getDeltaDB(), step.getOriginalDB());
        exportDatabase(database, file);
    }

    public void exportDatabase(IDatabase database, String file) throws DAOException {
        File outputFile = new File(file);
        outputFile.getParentFile().mkdirs();
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8")));
            for (String tableName : database.getTableNames()) {
                ITable table = database.getTable(tableName);
                writeTable(writer, table, database);
            }
        } catch (Exception ex) {
            throw new DAOException("Unable to export database " + ex);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void writeTable(PrintWriter writer, ITable table, IDatabase database) {
        writer.println(tablePrefix + table.getName());
        StringBuilder line = new StringBuilder();
        for (Attribute attribute : table.getAttributes()) {
            if (excludeAttribute(attribute.getName())) {
                continue;
            }
            line.append(cleanAttributeName(attribute.getName())).append(CSV_SEPARATOR);
        }
        LunaticUtility.removeChars(CSV_SEPARATOR.length(), line);
        writer.println(line.toString());
        ITupleIterator it = table.getTupleIterator();
//        ITupleIterator it = getDistinctFromTable(table, database);
        while (it.hasNext()) {
            line = new StringBuilder();
            Tuple tuple = it.next();
            for (Cell cell : tuple.getCells()) {
                if (excludeAttribute(cell.getAttribute())) {
                    continue;
                }
                String value = cell.getValue().toString();
//                if (LunaticConstants.NULL_VALUE.equals(value)) {
//                    value = "";
//                }
                if (value.startsWith(LunaticConstants.SKOLEM_PREFIX)) {
                    value = "NULL";
                }
                line.append(value).append(CSV_SEPARATOR);
            }
            LunaticUtility.removeChars(CSV_SEPARATOR.length(), line);
            writer.println(line.toString());
        }
        it.close();
        writer.println();
    }

//    private ITupleIterator getDistinctFromTable(ITable table, IDatabase database) {
//        Project project = new Project(buildAttributes(table.getAttributes()));
//        project.addChild(new Scan(new TableAlias(table.getName())));
//        Distinct distinct = new Distinct();
//        distinct.addChild(project);
//        return queryRunner.run(distinct, null, database);
//    }
//
//    private List<AttributeRef> buildAttributes(List<Attribute> attributes) {
//        List<AttributeRef> result = new ArrayList<AttributeRef>();
//        for (Attribute attribute : attributes) {
//            if (excludeAttribute(attribute.getName())) {
//                continue;
//            }
//            result.add(new AttributeRef(attribute.getTableName(), attribute.getName()));
//        }
//        return result;
//    }

    private String cleanAttributeName(String attributeName) {
        if (!attributeName.contains("_")) {
            return attributeName;
        }
        return attributeName.substring(attributeName.lastIndexOf("_") + 1);
    }

    private boolean excludeAttribute(String attribute) {
        return LunaticConstants.OID.equals(attribute) || LunaticConstants.TID.equals(attribute);
//        return false;
//        return LunaticConstants.OID.equals(attribute) || LunaticConstants.TID.equals(attribute) || attribute.endsWith("_" + LunaticConstants.OID);
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///////                       MATERIALIZE FOREIGN KEY JOINS                                  //////
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    private BuildAlgebraTreeForTGD treeBuilderForTGD = new BuildAlgebraTreeForTGD();
    private AlgebraTreeToSQL queryBuilder = new AlgebraTreeToSQL();

    private void materializeFKJoins(IDatabase database, DeltaChaseStep step) {
        if (step.getScenario().isDBMS()) {
            materializeFKJoinsDBMS(database, step);
        } else {
            materializeFKJoinsMainMemory(database, step);
        }
    }

    private void materializeFKJoinsDBMS(IDatabase database, DeltaChaseStep step) {
        Scenario scenario = step.getScenario();
        for (Dependency dependency : scenario.getExtTGDs()) {
            IAlgebraOperator satisfactionQuery = treeBuilderForTGD.buildCheckSatisfactionAlgebraTreesForTGD(dependency, scenario, false);
            String tableName = getJoinTableName(dependency);
            materializeJoin(tableName, satisfactionQuery, database);
            DBMSVirtualDB virtualDB = (DBMSVirtualDB) database;
            DBMSTable joinTable = new DBMSTable(tableName, ((DBMSDB) step.getDeltaDB()).getAccessConfiguration());
            virtualDB.addTable(joinTable);
        }
    }

    private void materializeJoin(String tableName, IAlgebraOperator satisfactionQuery, IDatabase databaseForStep) {
        AccessConfiguration accessConfiguration = ((DBMSVirtualDB) databaseForStep).getAccessConfiguration();
        StringBuilder script = new StringBuilder();
        script.append("DROP TABLE IF EXISTS ").append(accessConfiguration.getSchemaName()).append(".").append(tableName).append(";\n");
        script.append("CREATE TABLE ").append(accessConfiguration.getSchemaName()).append(".").append(tableName).append(" WITH OIDS AS (\n");
        script.append(queryBuilder.treeToSQL(satisfactionQuery, null, databaseForStep, ""));
        script.append(");\n");
        QueryManager.executeScript(script.toString(), accessConfiguration, true, true, true);
    }

    private void materializeFKJoinsMainMemory(IDatabase database, DeltaChaseStep step) {
        Scenario scenario = step.getScenario();
        Map<Dependency, IAlgebraOperator> tgdQuerySatisfactionMap = treeBuilderForTGD.buildAlgebraTreesForTGDSatisfaction(scenario.getExtTGDs(), scenario);
        for (Dependency dependency : scenario.getExtTGDs()) {
            IAlgebraOperator satisfactionQuery = tgdQuerySatisfactionMap.get(dependency);
            String tableName = getJoinTableName(dependency);
            MainMemoryVirtualTable joinTable = new MainMemoryVirtualTable(tableName, satisfactionQuery, database, step.getOriginalDB());
            MainMemoryVirtualDB virtualDB = (MainMemoryVirtualDB) database;
            virtualDB.addTable(joinTable);
        }
    }

    private String getJoinTableName(Dependency dependency) {
        StringBuilder sb = new StringBuilder();
        for (IFormulaAtom atom : dependency.getPremise().getAtoms()) {
            if (atom instanceof RelationalAtom) {
                RelationalAtom relationalAtom = (RelationalAtom) atom;
                sb.append(relationalAtom.getTableName()).append("_");
            }
        }
        sb.append("join_");
        for (IFormulaAtom atom : dependency.getConclusion().getAtoms()) {
            if (atom instanceof RelationalAtom) {
                RelationalAtom relationalAtom = (RelationalAtom) atom;
                sb.append(relationalAtom.getTableName()).append("_");
            }
        }
        LunaticUtility.removeChars("_".length(), sb);
        return sb.toString();
    }
}