package it.unibas.lunatic.model.chase.chasemc.costmanager;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasede.costmanager.ICostManagerDE;
import it.unibas.lunatic.model.chase.chasede.costmanager.StandardCostManagerDE;
import it.unibas.lunatic.model.chase.chasede.costmanager.StandardSymmetricCostManagerDE;
import it.unibas.lunatic.model.chase.chasemc.costmanager.nonsymmetric.GreedyCostManager;
import it.unibas.lunatic.model.chase.chasemc.costmanager.nonsymmetric.SimilarityToPreferredValueCostManager;
import it.unibas.lunatic.model.chase.chasemc.costmanager.nonsymmetric.StandardCostManager;
import it.unibas.lunatic.model.chase.chasemc.costmanager.symmetric.GreedySymmetricCostManager;
import it.unibas.lunatic.model.chase.chasemc.costmanager.symmetric.SimilarityToPreferredValueSymmetricCostManager;
import it.unibas.lunatic.model.chase.chasemc.costmanager.symmetric.StandardSymmetricCostManager;
import it.unibas.lunatic.model.dependency.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CostManagerFactory {

    private static Logger logger = LoggerFactory.getLogger(CostManagerFactory.class);

    public static ICostManagerMC getCostManagerMC(Dependency dependency, Scenario scenario) {
        ICostManagerMC result;
        if (scenario.getConfiguration().isUseSymmetricOptimization() && dependency.hasSymmetricChase()) {
            result = getSymmetricChaseMC(dependency, scenario.getCostManagerConfiguration());
        } else {
            result = getNonSymmetricChaseMC(dependency, scenario.getCostManagerConfiguration());
        }
        if (logger.isInfoEnabled()) logger.info("## Using CostManager " + result.getClass().getSimpleName() + " for Dependency " + dependency.getId());
        return result;
    }

    private static ICostManagerMC getSymmetricChaseMC(Dependency dependency, CostManagerConfiguration costManagerConfiguration) {
        if (costManagerConfiguration.getType().equals(LunaticConstants.COST_MANAGER_STANDARD)) {
            return new StandardSymmetricCostManager();
        }
        if (costManagerConfiguration.getType().equals(LunaticConstants.COST_MANAGER_SIMILARITY)) {
            return new SimilarityToPreferredValueSymmetricCostManager();
        }
        if (costManagerConfiguration.getType().equals(LunaticConstants.COST_MANAGER_GREEDY)) {
            return new GreedySymmetricCostManager();
        }
        throw new IllegalArgumentException("Unknown costmanager type " + costManagerConfiguration.getType());
    }

    private static ICostManagerMC getNonSymmetricChaseMC(Dependency dependency, CostManagerConfiguration costManagerConfiguration) {
        if (costManagerConfiguration.getType().equals(LunaticConstants.COST_MANAGER_STANDARD)) {
            return new StandardCostManager();
        }
        if (costManagerConfiguration.getType().equals(LunaticConstants.COST_MANAGER_SIMILARITY)) {
            return new SimilarityToPreferredValueCostManager();
        }
        if (costManagerConfiguration.getType().equals(LunaticConstants.COST_MANAGER_GREEDY)) {
            return new GreedyCostManager();
        }
        throw new IllegalArgumentException("Unknown costmanager type " + costManagerConfiguration.getType());
    }

    public static ICostManagerDE getCostManagerDE(Dependency dependency, Scenario scenario) {
        ICostManagerDE result;
        if (scenario.getConfiguration().isUseSymmetricOptimization() && dependency.hasSymmetricChase()) {
            result = new StandardSymmetricCostManagerDE();
        } else {
            result = new StandardCostManagerDE();
        }
        if (logger.isInfoEnabled()) logger.info("## Using CostManager " + result.getClass().getSimpleName() + " for Dependency " + dependency.getId());
        return result;
    }

}
