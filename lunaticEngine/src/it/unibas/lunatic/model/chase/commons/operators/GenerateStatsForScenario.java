package it.unibas.lunatic.model.chase.commons.operators;

import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.commons.ChaseStats;
import it.unibas.lunatic.model.dependency.Dependency;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;

public class GenerateStatsForScenario {

    private final static Logger logger = LoggerFactory.getLogger(GenerateStatsForScenario.class);

    public void generateStats(Scenario scenario) {
        // schemas
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_SOURCE_TABLES, scenario.getSource().getTableNames().size());
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_SOURCE_ATTRIBUTES, countAttributes(scenario.getSource()));
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_TARGET_TABLES, scenario.getTarget().getTableNames().size());
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_TARGET_ATTRIBUTES, countAttributes(scenario.getTarget()));
        // dependencies
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_STTGDS, scenario.getSTTgds().size());
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_INCLUSION_DEPS, countInclusionDependencies(scenario.getExtTGDs()));
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_LINEAR_TGDS, countLinearTgds(scenario.getExtTGDs()));
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_EXTGDS, scenario.getExtTGDs().size());
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_EGDS, scenario.getEGDs().size());
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_EGDS_FDS, countFunctionalDependencies(scenario.getEGDs()));
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_EXTEGDS, scenario.getExtEGDs().size());
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_EXTEGDS_FDS, countFunctionalDependencies(scenario.getExtEGDs()));
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_DCS, scenario.getDCs().size());
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_QUERIES, scenario.getQueries().size());
        // deds
        if (scenario.isDEDScenario()) {
            ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_DED_STTGDS, scenario.getDEDstTGDs().size());
            ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_DED_EGDS, scenario.getDEDEGDs().size());
            ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_DED_EXTGDS, scenario.getDEDextTGDs().size());
        }

    }

    private long countAttributes(IDatabase db) {
        int counter = 0;
        for (String tableName : db.getTableNames()) {
            ITable table = db.getTable(tableName);
            counter += table.getAttributes().size() - 1;
        }
        return counter;
    }

    private long countInclusionDependencies(List<Dependency> tgds) {
        int counter = 0;
        for (Dependency tgd : tgds) {
            if (tgd.isInclusionDependency()) {
                counter++;
            }
        }
        return counter;
    }

    private long countLinearTgds(List<Dependency> tgds) {
        int counter = 0;
        for (Dependency tgd : tgds) {
            if (tgd.isLinearTgd()) {
                counter++;
            }
        }
        return counter;
    }

    private long countFunctionalDependencies(List<Dependency> egds) {
        int counter = 0;
        for (Dependency egd : egds) {
            if (egd.isFunctionalDependency()) {
                counter++;
            }
        }
        return counter;
    }

}
