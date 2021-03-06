package it.unibas.lunatic.test.checker;

import it.unibas.lunatic.OperatorFactory;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasemc.operators.CellGroupIDGenerator;
import it.unibas.lunatic.model.chase.chasemc.operators.ChaseTreeSize;
import it.unibas.lunatic.model.chase.chasemc.partialorder.FrequencyPartialOrder;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.model.chase.chasemc.costmanager.CostManagerConfiguration;
import it.unibas.lunatic.model.chase.chasemc.operators.CheckConsistencyOfCellGroups;
import it.unibas.lunatic.model.chase.chasemc.operators.PrintRankedSolutions;
import it.unibas.lunatic.model.chase.commons.ChaseStats;
import it.unibas.lunatic.persistence.relational.GenerateModifiedCells;
import it.unibas.lunatic.test.comparator.repairs.PrecisionAndRecall;
import java.util.List;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.database.IDatabase;
import speedy.model.database.mainmemory.datasource.IntegerOIDGenerator;
import speedy.persistence.relational.QueryStatManager;

public class CheckTest extends TestCase {
    
    private static Logger logger = LoggerFactory.getLogger(CheckTest.class);
    
    protected ChaseTreeSize resultSizer = new ChaseTreeSize();
    protected PrintRankedSolutions solutionPrinter = new PrintRankedSolutions();
//    protected RepairsComparator comparator = new RepairsComparator();
    protected ChaseStats chaseStats = ChaseStats.getInstance();
    protected QueryStatManager queryStats = QueryStatManager.getInstance();
    protected CheckConsistencyOfCellGroups validSolutionChecker = new CheckConsistencyOfCellGroups();
    
    protected GenerateModifiedCells getModifiedCellGenerator(Scenario scenario) {
        return new GenerateModifiedCells(OperatorFactory.getInstance().getOccurrenceHandlerMC(scenario));
    }
    
    @Override
    protected void setUp() throws Exception {
    }
    
    @Override
    protected void tearDown() throws Exception {
        IntegerOIDGenerator.resetCounter();
        IntegerOIDGenerator.clearCache();
        CellGroupIDGenerator.resetCounter();
        OperatorFactory.getInstance().reset();
        chaseStats.resetStatistics();
        queryStats.resetStatistics();
    }
    
    protected void setConfigurationForTest(Scenario scenario) {
        scenario.getConfiguration().setCheckSolutions(true);
        scenario.getConfiguration().setCheckSolutionsQuery(true);
        scenario.getConfiguration().setCheckAllNodesForEGDSatisfaction(true);
        if (scenario.isMainMemory()) scenario.getConfiguration().setDebugMode(true);
    }
    
    protected void setCheckEGDsAfterEachStep(Scenario scenario) {
        scenario.getConfiguration().setCheckAllNodesForEGDSatisfaction(true);
    }
    
    protected void checkSolutions(DeltaChaseStep result) {
        if (logger.isDebugEnabled()) logger.debug("Checking that leaves are solutions...");
        validSolutionChecker.checkConsistencyOfCellGroupsInStep(result);
        Assert.assertTrue("No solution...", resultSizer.getSolutions(result) > 0);
        Assert.assertTrue("No solution...", resultSizer.getAllNodes(result) > 0);
        Assert.assertEquals("Expected solutions", resultSizer.getPotentialSolutions(result), resultSizer.getSolutions(result));
    }
    
    protected String getTestName(String scenarioName, Scenario scenario) {
        StringBuilder name = new StringBuilder(scenarioName.toUpperCase());
        if (scenario.isDEScenario()) {
            return name.toString() + "-DE";
        }
        if (scenario.getPartialOrder() instanceof FrequencyPartialOrder) {
            name.append("-FR");
        }
        CostManagerConfiguration costManager = scenario.getCostManagerConfiguration();
        if (costManager.getDependencyLimit() == 1) {
            name.append("-SP");
        } else {
//            name.append("-").append(costManager.getChaseTreeSizeThreshold());
            name.append("-").append(costManager.getPotentialSolutionsThreshold());
        }
        if (!scenario.getCostManagerConfiguration().isDoBackwardForAllDependencies()) {
            name.append("-FO");
        }
        return name.toString();
    }
    
    protected PrecisionAndRecall computeMin(List<PrecisionAndRecall> listPrecisionAndRecall) {
        return listPrecisionAndRecall.get(listPrecisionAndRecall.size() - 1);
    }
    
    protected PrecisionAndRecall computeMax(List<PrecisionAndRecall> listPrecisionAndRecall) {
        return listPrecisionAndRecall.get(0);
    }
    
    protected PrecisionAndRecall computeMean(List<PrecisionAndRecall> listPrecisionAndRecall) {
        double sumP = 0.0;
        double sumR = 0.0;
        for (PrecisionAndRecall precisionAndRecall : listPrecisionAndRecall) {
            sumP += precisionAndRecall.getPrecision();
            sumR += precisionAndRecall.getRecall();
        }
        double precision = sumP / ((double) listPrecisionAndRecall.size());
        double recall = sumR / ((double) listPrecisionAndRecall.size());
        double fMeasure = (2 * precision * recall) / (precision + recall);
        return new PrecisionAndRecall(precision, recall, fMeasure);
    }
    
    @SuppressWarnings("unchecked")
    protected void checkExpectedInstances(IDatabase result, Scenario scenario) throws Exception {
        String expectedResultFile = generateExpectedFileName(scenario);
        checkExpectedInstances(result, expectedResultFile, scenario);
    }
    
    protected void checkExpectedInstances(IDatabase result, String expectedResultFile, Scenario scenario) throws Exception {
        DataSourceTxtInstanceChecker checker = new DataSourceTxtInstanceChecker();
        checker.checkInstance(result, expectedResultFile);
    }
    
    private String generateExpectedFileName(Scenario scenario) {
        String fileName = scenario.getFileName().substring(0, scenario.getFileName().lastIndexOf("."));
        return fileName + "-expectedSolution.txt";
    }
    
    protected String generateOutputFileName(Scenario scenario) {
        String fileName = scenario.getFileName().substring(0, scenario.getFileName().lastIndexOf("."));
        fileName = fileName.replace("build/classes", "misc");
        return fileName + ".out";
    }
    
    protected String generateOutputFileName(Scenario scenario, String suffix) {
        String fileName = scenario.getFileName().substring(0, scenario.getFileName().lastIndexOf("."));
        fileName = fileName.replace("build/classes", "misc");
        return fileName + "." + suffix + ".out";
    }
    
    public void testDummy() {
    }
}
