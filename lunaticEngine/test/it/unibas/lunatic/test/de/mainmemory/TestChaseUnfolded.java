package it.unibas.lunatic.test.de.mainmemory;

import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasede.DEChaserFactory;
import speedy.model.database.IDatabase;
import it.unibas.lunatic.test.References;
import it.unibas.lunatic.test.UtilityTest;
import it.unibas.lunatic.test.checker.CheckTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestChaseUnfolded extends CheckTest {

    private static Logger logger = LoggerFactory.getLogger(TestChaseUnfolded.class);

    public void testEmployees() throws Exception {
        Scenario scenario = UtilityTest.loadScenarioFromResources(References.unfolding_employees);
        scenario.getConfiguration().setOptimizeSTTGDs(false);
        IDatabase result = DEChaserFactory.getChaser(scenario).doChase(scenario);
        if (logger.isDebugEnabled()) logger.debug(result.toString());
//        checkExpectedInstances(result, scenario);
    }

}
