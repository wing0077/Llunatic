package it.unibas.lunatic.test.mc.mainmemory;

import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasemc.ChaseMCScenario;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.test.References;
import it.unibas.lunatic.test.UtilityTest;
import it.unibas.lunatic.test.checker.CheckTest;
import junit.framework.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCustomers extends CheckTest {

    private static Logger logger = LoggerFactory.getLogger(TestCustomers.class);

    public void testScenarioDelta() throws Exception {
        Scenario scenario = UtilityTest.loadScenarioFromResources(References.customers_cfd);
        setConfigurationForTest(scenario);
//        scenario.getConfiguration().setRemoveDuplicates(true);
        ChaseMCScenario chaser = scenario.getCostManager().getChaser(scenario);
        DeltaChaseStep result = chaser.doChase(scenario);
        if (logger.isDebugEnabled()) logger.debug("Result: " + result.toStringLeavesOnlyWithSort());
//        Assert.assertEquals(26, resultSizer.getSolutions(result));
        Assert.assertEquals(19, resultSizer.getSolutions(result));
        Assert.assertEquals(7, resultSizer.getDuplicates(result));
        checkSolutions(result);
    }
}
