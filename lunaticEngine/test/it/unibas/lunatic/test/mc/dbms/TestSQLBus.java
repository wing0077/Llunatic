package it.unibas.lunatic.test.mc.dbms;

import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasemc.operators.ChaseMCScenario;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.model.chase.commons.operators.ChaserFactoryMC;
import it.unibas.lunatic.model.dependency.Dependency;
import it.unibas.lunatic.test.References;
import it.unibas.lunatic.test.UtilityTest;
import it.unibas.lunatic.test.checker.CheckTest;
import junit.framework.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.persistence.relational.QueryStatManager;

public class TestSQLBus extends CheckTest {

    private static Logger logger = LoggerFactory.getLogger(TestSQLBus.class);

    public void testScenario() throws Exception {
        Scenario scenario = UtilityTest.loadScenarioFromResources(References.bus_30_5p_dbms, true);
//        Scenario scenario = UtilityTest.loadScenarioFromAbsolutePath("/Users/donatello/Desktop/BART-SIGMOD-Demo/bus/lunatic/bus-5k-5-dbms.xml", "", false);
        for (Dependency extEGD : scenario.getExtEGDs()) {
            System.out.println(extEGD.toSaveString());
        }
        setConfigurationForTest(scenario);
//        scenario.getConfiguration().setRemoveDuplicates(true);
        ChaseMCScenario chaser = ChaserFactoryMC.getChaser(scenario);
        DeltaChaseStep result = chaser.doChase(scenario);
        QueryStatManager.getInstance().printStatistics();
        if (logger.isDebugEnabled()) logger.debug("Scenario " + getTestName("bus", scenario));
        if (logger.isDebugEnabled()) logger.debug(result.toString());
        if (logger.isDebugEnabled()) logger.debug("Number of solutions: " + resultSizer.getPotentialSolutions(result));
        if (logger.isDebugEnabled()) logger.debug("Number of duplicates: " + resultSizer.getDuplicates(result));
        Assert.assertEquals(1, resultSizer.getSolutions(result));
        Assert.assertEquals(1, resultSizer.getDuplicates(result));
        checkSolutions(result);
    }
}
