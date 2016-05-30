package it.unibas.lunatic.test.mc.mainmemory.basicscenarios;

import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasemc.operators.ChaseMCScenario;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.model.chase.commons.operators.ChaserFactory;
import it.unibas.lunatic.test.References;
import it.unibas.lunatic.test.UtilityTest;
import it.unibas.lunatic.test.checker.CheckExpectedSolutionsTest;
import junit.framework.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSynthetic05 extends CheckExpectedSolutionsTest {

    private static Logger logger = LoggerFactory.getLogger(TestSynthetic05.class);

    public void testScenario() throws Exception {
        Scenario scenario = UtilityTest.loadScenarioFromResources(References.synthetic_05);
        setConfigurationForTest(scenario);
//        scenario.getConfiguration().setUseLimit1(true);
        scenario.getCostManagerConfiguration().setDoBackward(false);
        ChaseMCScenario chaser = ChaserFactory.getChaser(scenario);
        DeltaChaseStep result = chaser.doChase(scenario);
        if (logger.isDebugEnabled()) logger.debug(scenario.toString());
        if (logger.isDebugEnabled()) logger.debug(result.toStringWithSort());
        if (logger.isDebugEnabled()) logger.debug("Solutions: " + resultSizer.getPotentialSolutions(result));
        if (logger.isDebugEnabled()) logger.debug("Duplicate solutions: " + resultSizer.getDuplicates(result));
        Assert.assertEquals(1, resultSizer.getPotentialSolutions(result));
        Assert.assertEquals(0, resultSizer.getDuplicates(result));
        checkSolutions(result);
//        exportResults("/Users/enzoveltri/Temp/lunatic_tmp/expectedSynthetic05", result);
        checkExpectedSolutions("expectedSynthetic05", result);
    }

}
