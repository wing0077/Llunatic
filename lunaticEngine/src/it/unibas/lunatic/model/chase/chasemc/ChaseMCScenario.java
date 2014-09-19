package it.unibas.lunatic.model.chase.chasemc;

import it.unibas.lunatic.LunaticConfiguration;
import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.exceptions.ChaseException;
import it.unibas.lunatic.model.algebra.IAlgebraOperator;
import it.unibas.lunatic.model.algebra.operators.BuildAlgebraTreeForEGD;
import it.unibas.lunatic.model.algebra.operators.BuildAlgebraTreeForTGD;
import it.unibas.lunatic.model.algebra.operators.IInsertTuple;
import it.unibas.lunatic.model.chase.commons.ChaseDeltaDCs;
import it.unibas.lunatic.model.chase.commons.ChaseStats;
import it.unibas.lunatic.model.chase.commons.ChaseUtility;
import it.unibas.lunatic.model.chase.commons.IChaseSTTGDs;
import it.unibas.lunatic.model.chase.commons.control.IChaseState;
import it.unibas.lunatic.model.chase.commons.control.ImmutableChaseState;
import it.unibas.lunatic.model.chase.chasemc.operators.ChaseDeltaExtEGDs;
import it.unibas.lunatic.model.chase.chasemc.operators.ChaserResult;
import it.unibas.lunatic.model.chase.chasemc.operators.CheckSolution;
import it.unibas.lunatic.model.chase.chasemc.operators.IBuildDatabaseForChaseStep;
import it.unibas.lunatic.model.chase.chasemc.operators.IBuildDeltaDB;
import it.unibas.lunatic.model.chase.chasemc.operators.IChaseDeltaExtTGDs;
import it.unibas.lunatic.model.chase.chasemc.operators.IInsertTuplesForTGDs;
import it.unibas.lunatic.model.chase.chasemc.operators.IRunQuery;
import it.unibas.lunatic.model.chase.chasemc.operators.IValueOccurrenceHandlerMC;
import it.unibas.lunatic.model.database.IDatabase;
import it.unibas.lunatic.model.dependency.Dependency;
import it.unibas.lunatic.model.dependency.operators.AnalyzeDependencies;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaseMCScenario {

    private static Logger logger = LoggerFactory.getLogger(ChaseMCScenario.class);
    public static final int ITERATION_LIMIT = 10;
    private BuildAlgebraTreeForTGD treeBuilderForTGD = new BuildAlgebraTreeForTGD();
    private BuildAlgebraTreeForEGD treeBuilderForEGD = new BuildAlgebraTreeForEGD();
    private IChaseSTTGDs stChaser;
    private IChaseDeltaExtTGDs extTgdChaser;
    private ChaseDeltaDCs dChaser;
    private ChaseDeltaExtEGDs egdChaser;
    private IBuildDeltaDB deltaBuilder;
    private IInsertTuplesForTGDs insertOperatorForTgds;
    private AnalyzeDependencies stratificationBuilder = new AnalyzeDependencies();
    private CheckSolution solutionChecker;

    public ChaseMCScenario(IChaseSTTGDs stChaser, IChaseDeltaExtTGDs extTgdChaser,
            IBuildDeltaDB deltaBuilder, IBuildDatabaseForChaseStep stepBuilder, IRunQuery queryRunner,
            IInsertTuple insertOperatorForEgds, IInsertTuplesForTGDs insertOperatorForTgds, IValueOccurrenceHandlerMC occurrenceHandler,
            ChaseDeltaExtEGDs egdChaser, CheckSolution solutionChecker) {
        this.stChaser = stChaser;
        this.deltaBuilder = deltaBuilder;
        this.insertOperatorForTgds = insertOperatorForTgds;
        this.extTgdChaser = extTgdChaser;
        this.dChaser = new ChaseDeltaDCs(queryRunner, stepBuilder);
        this.egdChaser = egdChaser;
        this.solutionChecker = solutionChecker;
    }

    public DeltaChaseStep doChase(Scenario scenario, IChaseState chaseState) {
        long start = new Date().getTime();
        stChaser.doChase(scenario, false);
        ChaseTree chaseTree = new ChaseTree(scenario);
        IDatabase targetDB = scenario.getTarget();
//        insertOperatorForTgds.initializeOIDs(targetDB);
        if (logger.isDebugEnabled()) logger.debug("-------------------Chasing dependencies on mc scenario: " + scenario);
        //Generate stratification (must be first step because affects other steps)
        stratificationBuilder.prepareDependenciesAndGenerateStratification(scenario);
        IDatabase deltaDB = deltaBuilder.generate(targetDB, scenario, LunaticConstants.CHASE_STEP_ROOT);
        if (logger.isDebugEnabled()) logger.debug("DeltaDB: " + deltaDB);
        DeltaChaseStep root = new DeltaChaseStep(scenario, chaseTree, LunaticConstants.CHASE_STEP_ROOT, targetDB, deltaDB);
        DeltaChaseStep result = doChase(root, scenario, chaseState);
        long end = new Date().getTime();
        ChaseStats.getInstance().addStat(ChaseStats.TOTAL_TIME, end - start);
        ChaseStats.getInstance().printStats();
        return result;
    }

    public DeltaChaseStep doChase(DeltaChaseStep root, Scenario scenario, IChaseState chaseState) {
        if(scenario.isDEDScenario()){
            throw new ChaseException("ChaseMCScenario cannot handle scenarios with deds");
        }
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_STTGDS, scenario.getSTTgds().size());
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_EXTEGDS, scenario.getExtEGDs().size());
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_EXTGDS, scenario.getExtTGDs().size());
        ChaseStats.getInstance().addStat(ChaseStats.NUMBER_OF_DCS, scenario.getDCs().size());
        ChaseStats.getInstance().printStats();
        Map<Dependency, IAlgebraOperator> egdQueryMap = treeBuilderForEGD.buildPremiseAlgebraTreesForEGDs(scenario.getExtEGDs(), scenario);
        Map<Dependency, IAlgebraOperator> tgdQueryMap = treeBuilderForTGD.buildAlgebraTreesForTGDViolations(scenario.getExtTGDs(), scenario);
        Map<Dependency, IAlgebraOperator> tgdQuerySatisfactionMap = treeBuilderForTGD.buildAlgebraTreesForTGDSatisfaction(scenario.getExtTGDs(), scenario);
        Map<Dependency, IAlgebraOperator> dQueryMap = treeBuilderForTGD.buildAlgebraTreesForDTGD(scenario.getDCs(), scenario);
        IDatabase targetDB = scenario.getTarget();
        insertOperatorForTgds.initializeOIDs(targetDB);
        boolean userInteractionRequired = false;
        int iterations = 0;
        while (!userInteractionRequired) {
            if (chaseState.isCancelled()) {
                //throw new ChaseException("Chase interrupted by user");
                ChaseUtility.stopChase(chaseState);
            }
            boolean newTgdNodes = extTgdChaser.doChase(root, scenario, chaseState, tgdQueryMap, tgdQuerySatisfactionMap);
            if (newTgdNodes && logger.isDebugEnabled()) logger.debug("Chase tree after tgd enforcement:\n" + root);
            ChaserResult egdResult = egdChaser.doChase(root, scenario, chaseState, egdQueryMap);
            userInteractionRequired = egdResult.isUserInteractionRequired();
            if (egdResult.isNewNodes() && logger.isDebugEnabled()) logger.debug("Chase tree after egd enforcement:\n" + root);
            if (!newTgdNodes && !egdResult.isNewNodes()) {
                break;
            } else {
                iterations++;
            }
            if (iterations > ITERATION_LIMIT) {
                throw new ChaseException("Reached iteration limit " + ITERATION_LIMIT + " with no solution...");
            }
        }
        if (!scenario.getConfiguration().isCheckSolutions() && !userInteractionRequired) {
            solutionChecker.markLeavesAsSolutions(root, scenario);
        } else {
            if (LunaticConfiguration.sout) System.out.println("------ Checking solutions...");
            solutionChecker.checkSolutions(root, scenario);
        }
        dChaser.doChase(root, scenario, chaseState, dQueryMap);
        if (logger.isDebugEnabled()) logger.debug("------------------Final Chase tree: ----\n" + root);
        return root;
    }

    public DeltaChaseStep doChase(Scenario scenario) {
        return doChase(scenario, ImmutableChaseState.getInstance());
    }

    public DeltaChaseStep doChase(DeltaChaseStep root, Scenario scenario) {
        return doChase(root, scenario, ImmutableChaseState.getInstance());
    }
}
