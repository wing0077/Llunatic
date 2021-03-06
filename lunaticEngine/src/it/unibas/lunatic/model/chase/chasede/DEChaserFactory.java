package it.unibas.lunatic.model.chase.chasede;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.OperatorFactory;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasede.operators.ChaseDEScenario;
import it.unibas.lunatic.model.chase.chasede.operators.ChaseDeltaEGDs;
import it.unibas.lunatic.model.chase.chasede.operators.IBuildDatabaseForDE;
import it.unibas.lunatic.model.chase.chasede.operators.IReplaceDatabase;
import it.unibas.lunatic.model.chase.commons.operators.IBuildDeltaDB;
import it.unibas.lunatic.model.chase.commons.operators.IChaseSTTGDs;
import speedy.model.database.operators.IAnalyzeDatabase;
import speedy.model.database.operators.IRunQuery;

public class DEChaserFactory {

    public static IDEChaser getChaser(Scenario scenario) {
        String deChaserStrategy = scenario.getConfiguration().getDeChaser();
        if (deChaserStrategy.equals(LunaticConstants.DE_OPTIMIZED_CHASER)) {
            return getDEOptimizedChaser(scenario);
        }
        throw new IllegalArgumentException("DE Chaser " + deChaserStrategy + " is not supported");
    }

    private static IDEChaser getDEOptimizedChaser(Scenario scenario) {
        IChaseSTTGDs stChaser = OperatorFactory.getInstance().getSTChaser(scenario);
        ChaseDeltaEGDs egdChaser = OperatorFactory.getInstance().getDeltaEGDChaser(scenario);
        IRunQuery queryRunner = OperatorFactory.getInstance().getQueryRunner(scenario);
        IBuildDeltaDB deltaBuilder = OperatorFactory.getInstance().getDeltaDBBuilderDE(scenario);
        IBuildDatabaseForDE databaseBuilder = OperatorFactory.getInstance().getDatabaseBuilderDE(scenario);
        IAnalyzeDatabase databaseAnalyzer = OperatorFactory.getInstance().getDatabaseAnalyzer(scenario);
        IReplaceDatabase databaseReplacer = OperatorFactory.getInstance().getDatabaseReplacer(scenario);
        ChaseDEScenario tgdChaser = new ChaseDEScenario(stChaser, egdChaser, queryRunner, deltaBuilder, databaseBuilder, databaseAnalyzer, databaseReplacer);
        return tgdChaser;
    }

}
