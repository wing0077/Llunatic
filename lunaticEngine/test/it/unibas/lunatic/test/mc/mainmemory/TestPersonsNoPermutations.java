package it.unibas.lunatic.test.mc.mainmemory;

import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasemc.ChaseMCScenario;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.model.chase.chasemc.costmanager.SimilarityToMostFrequentCostManager;
import it.unibas.lunatic.model.chase.chasemc.partialorder.FrequencyPartialOrder;
import it.unibas.lunatic.test.References;
import it.unibas.lunatic.test.UtilityTest;
import it.unibas.lunatic.test.checker.CheckExpectedSolutionsTest;
import junit.framework.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPersonsNoPermutations extends CheckExpectedSolutionsTest {

    private static Logger logger = LoggerFactory.getLogger(TestPersonsNoPermutations.class);

    public void testScenario() throws Exception {
        Scenario scenario = UtilityTest.loadScenarioFromResources(References.persons_fr_sp);
        setConfigurationForTest(scenario);
        ChaseMCScenario chaser = scenario.getSymmetricCostManager().getChaser(scenario);
        Assert.assertTrue(scenario.getPartialOrder() instanceof FrequencyPartialOrder);
        Assert.assertTrue(scenario.getSymmetricCostManager() instanceof SimilarityToMostFrequentCostManager);
        if (logger.isDebugEnabled()) logger.debug(scenario.toString());
//        if (logger.isDebugEnabled()) logger.debug("Scenario " + getTestName("persons", scenario));
        DeltaChaseStep result = chaser.doChase(scenario);
        if (logger.isDebugEnabled()) logger.debug(result.getDeltaDB().printInstances(false));
        if (logger.isDebugEnabled()) logger.debug(result.toStringWithSort());
        Assert.assertEquals(1, resultSizer.getPotentialSolutions(result));
        Assert.assertEquals(0, resultSizer.getDuplicates(result));
        checkSolutions(result);
        checkExpectedSolutions("expected-frsp", result);
    }
}
