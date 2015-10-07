package it.unibas.lunatic.test.mc.mainmemory;

import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasemc.ChaseMCScenario;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.test.References;
import it.unibas.lunatic.test.UtilityTest;
import it.unibas.lunatic.test.checker.CheckExpectedSolutionsTest;
import junit.framework.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPersons extends CheckExpectedSolutionsTest {

    private static Logger logger = LoggerFactory.getLogger(TestPersons.class);

    
    public void testScenarioNoPermutation() throws Exception {
        Scenario scenario = UtilityTest.loadScenarioFromResources(References.persons);
        setConfigurationForTest(scenario);
        scenario.getConfiguration().setUseSymmetricOptimization(false); //TODO++ Remove
//        setCheckEGDsAfterEachStep(scenario);
        scenario.getSymmetricCostManager().setDoBackward(false);
        scenario.getSymmetricCostManager().setDoPermutations(false);
        ChaseMCScenario chaser = scenario.getSymmetricCostManager().getChaser(scenario);
        DeltaChaseStep result = chaser.doChase(scenario);
        if (logger.isDebugEnabled()) logger.debug("Scenario " + getTestName("persons", scenario));
        if (logger.isDebugEnabled()) logger.debug(result.toStringWithSort());
        if (logger.isDebugEnabled()) logger.debug("Number of solutions: " + resultSizer.getPotentialSolutions(result));
        if (logger.isDebugEnabled()) logger.debug("Number of duplicate solutions: " + resultSizer.getDuplicates(result));
        Assert.assertEquals(1, resultSizer.getPotentialSolutions(result));
        checkSolutions(result);
        checkExpectedSolutions("expected-nop", result);
    }

    public void testScenario() throws Exception {
        Scenario scenario = UtilityTest.loadScenarioFromResources(References.persons);
        if (logger.isDebugEnabled()) logger.debug(scenario.toString());
        setConfigurationForTest(scenario);
        scenario.getConfiguration().setUseSymmetricOptimization(false);//TODO++ Remove
//        setCheckEGDsAfterEachStep(scenario);
//        scenario.getSymmetricCostManager().setDoBackward(false);
//        scenario.getSymmetricCostManager().setDoPermutations(false);
        ChaseMCScenario chaser = scenario.getSymmetricCostManager().getChaser(scenario);
        DeltaChaseStep result = chaser.doChase(scenario);
//        if (logger.isDebugEnabled()) logger.debug("Scenario " + getTestName("persons", scenario));
        if (logger.isDebugEnabled()) logger.debug(scenario.toString());
        if (logger.isDebugEnabled()) logger.debug(result.toStringWithSort());
        if (logger.isDebugEnabled()) logger.debug("Number of solutions: " + resultSizer.getPotentialSolutions(result));
        if (logger.isDebugEnabled()) logger.debug("Number of duplicate solutions: " + resultSizer.getDuplicates(result));
        Assert.assertEquals(9, resultSizer.getPotentialSolutions(result));
        checkSolutions(result);
//        exportResults("/Users/enzoveltri/Temp/lunatic_tmp/expectedPersons", result);
        checkExpectedSolutions("expectedPersons", result);
//        if (logger.isDebugEnabled()) logger.debug("Delta db:\n" + result.getDeltaDB().printInstances());
    }
}
