package it.unibas.lunatic.test.mc.dbms.tgd;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.test.mc.mainmemory.tgd.*;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasemc.ChaseMCScenario;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.test.References;
import it.unibas.lunatic.test.UtilityTest;
import it.unibas.lunatic.test.checker.CheckTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSQLSyntheticTGD04 extends CheckTest {

    private static Logger logger = LoggerFactory.getLogger(TestSQLSyntheticTGD04.class);

    public void testScenario() throws Exception {
        Scenario scenario = UtilityTest.loadScenarioFromResources(References.synthetic_T04_dbms);
        setConfigurationForTest(scenario);
        ChaseMCScenario chaser = scenario.getCostManager().getChaser(scenario);
        DeltaChaseStep result = chaser.doChase(scenario);
        if (logger.isDebugEnabled()) logger.debug(result.getDeltaDB().getTable(LunaticConstants.OCCURRENCE_TABLE).toString());
        if (logger.isDebugEnabled()) logger.debug(result.toLongStringWithSort());
        if (logger.isDebugEnabled()) logger.debug("Solutions: " + resultSizer.getSolutions(result));
//        Assert.assertEquals(4, OperatorFactory.getInstance().getOccurrenceHandlerMC(scenario).loadAllCellGroupsInStep(result.getDeltaDB(), "r").size());
//        Assert.assertEquals(7, OperatorFactory.getInstance().getOccurrenceHandlerMC(scenario).loadAllCellGroups(result.getDeltaDB(), "r.tt0").size());
//        if (logger.isDebugEnabled()) logger.debug("Duplicate solutions: " + resultSizer.getDuplicates(result));
//        checkExpectedInstances((MainMemoryDB) result, scenario);
        checkSolutions(result);        
    }
}
