package it.unibas.lunatic.model.chase.chasemc.costmanager.symmetric;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.exceptions.ChaseException;
import it.unibas.lunatic.model.chase.chasemc.BackwardAttribute;
import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import it.unibas.lunatic.model.chase.chasemc.ChangeDescription;
import it.unibas.lunatic.model.chase.chasemc.EquivalenceClassForSymmetricEGD;
import it.unibas.lunatic.model.chase.chasemc.Repair;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.model.chase.chasemc.EGDEquivalenceClassCells;
import it.unibas.lunatic.model.chase.chasemc.EquivalenceClassForEGDProxy;
import it.unibas.lunatic.model.chase.chasemc.costmanager.AbstractCostManager;
import it.unibas.lunatic.model.chase.chasemc.costmanager.TupleGroupComparator;
import it.unibas.lunatic.model.chase.chasemc.costmanager.nonsymmetric.CostManagerUtility;
import it.unibas.lunatic.model.chase.chasemc.operators.CellGroupIDGenerator;
import it.unibas.lunatic.model.chase.chasemc.operators.OccurrenceHandlerMC;
import it.unibas.lunatic.model.chase.chasemc.partialorder.FrequencyPartialOrder;
import it.unibas.lunatic.model.chase.chasemc.partialorder.StandardPartialOrder;
import speedy.model.database.ConstantValue;
import speedy.model.database.IDatabase;
import speedy.model.database.IValue;
import it.unibas.lunatic.model.similarity.SimilarityFactory;
import it.unibas.lunatic.utility.DependencyUtility;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.database.LLUNValue;

public class SimilarityToMostFrequentSymmetricCostManager extends AbstractCostManager {

    private static Logger logger = LoggerFactory.getLogger(SimilarityToMostFrequentSymmetricCostManager.class);

    private double similarityThreshold = 0.8;
//    private String similarityStrategy = SimilarityFactory.SIMPLE_EDITS;
    private String similarityStrategy = SimilarityFactory.LEVENSHTEIN_STRATEGY;

    @SuppressWarnings("unchecked")
    public List<Repair> chooseRepairStrategy(EquivalenceClassForEGDProxy equivalenceClassProxy, DeltaChaseStep chaseTreeRoot,
            List<Repair> repairsForDependency, Scenario scenario, String stepId,
            OccurrenceHandlerMC occurrenceHandler) {
        if (!(scenario.getPartialOrder() instanceof FrequencyPartialOrder)) {
            logger.warn("##################################################################################");
            logger.warn("   SimilarityToMostFrequentCostManager should be used with FrequencyPartialOrder");
            logger.warn("##################################################################################");
            throw new ChaseException("SimilarityToMostFrequentCostManager requires FrequencyPartialOrder");
        }
        EquivalenceClassForSymmetricEGD equivalenceClass = (EquivalenceClassForSymmetricEGD) equivalenceClassProxy.getEquivalenceClass();
        if (logger.isInfoEnabled()) logger.info("######## Choosing repair strategy for equivalence class: " + equivalenceClass);
        List<EGDEquivalenceClassCells> tupleGroups = equivalenceClass.getTupleGroups();
        Collections.sort(tupleGroups, new TupleGroupComparator());
        Collections.reverse(tupleGroups);
        if (logger.isDebugEnabled()) logger.debug("Sorted tuple groups: " + tupleGroups);
        if (DependencyUtility.hasSourceSymbols(equivalenceClass.getEGD()) && satisfactionChecker.isSatisfiedAfterUpgrades(tupleGroups, scenario)) {
            if (logger.isDebugEnabled()) logger.debug("No violations... Returning empty list");
            return Collections.EMPTY_LIST;
        }
        List<Repair> result = new ArrayList<Repair>();
        Repair standardRepair = generateConstantRepairWithStandardPartialOrder(equivalenceClass, tupleGroups, scenario);
        if (standardRepair != null) {
            if (logger.isDebugEnabled()) logger.debug("Returning standard repair " + standardRepair);
            result.add(standardRepair);
            // if lub is a constant, no need to search for frequency/similarity
            return result;
        }
        // search similarity/frequency
        List<EGDEquivalenceClassCells> forwardGroups = new ArrayList<EGDEquivalenceClassCells>();
        List<EGDEquivalenceClassCells> backwardGroups = new ArrayList<EGDEquivalenceClassCells>();
        //TODO++ check: we need a single backward attribute per group of cells
        Map<EGDEquivalenceClassCells, BackwardAttribute> backwardAttributes = partitionGroups(tupleGroups, forwardGroups, backwardGroups, chaseTreeRoot, stepId, scenario);
        if (logger.isDebugEnabled()) logger.debug("Forward groups: " + forwardGroups);
        if (logger.isDebugEnabled()) logger.debug("Backward groups: " + backwardGroups);
        Repair repair = generateRepairWithBackwards(equivalenceClass, forwardGroups, backwardGroups, backwardAttributes, scenario, chaseTreeRoot.getDeltaDB(), stepId);
        if (allSuspicious(backwardGroups)) {
            if (logger.isDebugEnabled()) logger.debug("CostManager generates a repair with all suspicious changes\n" + repair);
            repair = generateSymmetricForwardRepair(tupleGroups, scenario, chaseTreeRoot.getDeltaDB(), stepId);
        }
        if (repair != null) {
            if (logger.isDebugEnabled()) logger.debug("Returning repair " + repair);
            result.add(repair);
        }
        if (logger.isInfoEnabled()) logger.info("Returning repair " + repair);
        return result;
    }

    private Repair generateConstantRepairWithStandardPartialOrder(EquivalenceClassForSymmetricEGD equivalenceClass, List<EGDEquivalenceClassCells> tupleGroups, Scenario scenario) {
        List<CellGroup> cellGroups = extractForwardCellGroups(tupleGroups);
        CellGroup cellGroup = new StandardPartialOrder().findLUB(cellGroups, scenario);
        IValue poValue = cellGroup.getValue();
        if (!(poValue instanceof ConstantValue)) {
            return null;
        }
        Repair repair = new Repair();
        ChangeDescription forwardChanges = new ChangeDescription(cellGroup, LunaticConstants.CHASE_FORWARD, buildWitnessCells(tupleGroups));
        repair.addViolationContext(forwardChanges);
        return repair;
    }

    private Map<EGDEquivalenceClassCells, BackwardAttribute> partitionGroups(List<EGDEquivalenceClassCells> tupleGroups, List<EGDEquivalenceClassCells> forwardGroups, List<EGDEquivalenceClassCells> backwardGroups, DeltaChaseStep chaseTreeRoot, String stepId, Scenario scenario) {
        Map<EGDEquivalenceClassCells, BackwardAttribute> result = new HashMap<EGDEquivalenceClassCells, BackwardAttribute>();
        EGDEquivalenceClassCells tupleGroup0 = tupleGroups.get(0);
        forwardGroups.add(tupleGroup0);
        for (int j = 1; j < tupleGroups.size(); j++) {
            EGDEquivalenceClassCells tupleGroupj = tupleGroups.get(j);
            if (tupleGroupj.getOccurrenceSize() == 0 || areSimilar(tupleGroup0, tupleGroupj)) {
                forwardGroups.add(tupleGroupj);
                continue;
            }
            BackwardAttribute backwardAttribute = canDoBackward(tupleGroupj);
            if (backwardAttribute == null) {
                forwardGroups.add(tupleGroupj);
                continue;
            }
            backwardGroups.add(tupleGroupj);
            result.put(tupleGroupj, backwardAttribute);
        }
        return result;
    }

    private Repair generateRepairWithBackwards(EquivalenceClassForSymmetricEGD equivalenceClass, List<EGDEquivalenceClassCells> forwardTupleGroups,
            List<EGDEquivalenceClassCells> backwardTupleGroups, Map<EGDEquivalenceClassCells, BackwardAttribute> backwardAttributes, Scenario scenario, IDatabase deltaDB, String stepId) {
        Repair repair = new Repair();
        if (forwardTupleGroups.size() > 1) {
            ChangeDescription forwardChanges = generateChangeDescriptionForForwardRepair(forwardTupleGroups, scenario, deltaDB, stepId);
            repair.addViolationContext(forwardChanges);
        }
        for (EGDEquivalenceClassCells backwardTupleGroup : backwardTupleGroups) {
            BackwardAttribute backwardAttribute = backwardAttributes.get(backwardTupleGroup);
            Set<CellGroup> backwardCellGroups = backwardTupleGroup.getCellGroupsForBackwardRepair().get(backwardAttribute);
            for (CellGroup backwardCellGroup : backwardCellGroups) {
                LLUNValue llunValue = CellGroupIDGenerator.getNextLLUNID();
                backwardCellGroup.setValue(llunValue);
                backwardCellGroup.setInvalidCell(CellGroupIDGenerator.getNextInvalidCell());
                ChangeDescription backwardChangesForGroup = new ChangeDescription(backwardCellGroup, LunaticConstants.CHASE_BACKWARD, buildWitnessCells(backwardTupleGroups));
                repair.addViolationContext(backwardChangesForGroup);
                if (scenario.getConfiguration().isRemoveSuspiciousSolutions() && isSuspicious(backwardCellGroup, backwardAttribute, equivalenceClass)) {
                    backwardTupleGroup.setSuspicious(true);
//                    repair.setSuspicious(true);
                }
            }
        }
        if (repair.getChangeDescriptions().isEmpty()) {
            return null;
        }
        return repair;
    }

    private boolean areSimilar(EGDEquivalenceClassCells t1, EGDEquivalenceClassCells t2) {
        IValue v1 = t1.getCellGroupForForwardRepair().getValue();
        IValue v2 = t2.getCellGroupForForwardRepair().getValue();
        return CostManagerUtility.areSimilar(v1, v2, similarityStrategy, similarityThreshold);
    }

    private BackwardAttribute canDoBackward(EGDEquivalenceClassCells tupleGroup) {
        for (BackwardAttribute backwardAttribute : tupleGroup.getWitnessCells().keySet()) {
            Set<CellGroup> backwardCellGroups = tupleGroup.getCellGroupsForBackwardRepair().get(backwardAttribute);
            if (CostManagerUtility.backwardIsAllowed(backwardCellGroups)) {
                return backwardAttribute;
            }
        }
        return null;
    }

    private boolean allSuspicious(List<EGDEquivalenceClassCells> backwardGroups) {
        if (backwardGroups.isEmpty()) {
            return false;
        }
        for (EGDEquivalenceClassCells targetCellsToChange : backwardGroups) {
            if (!targetCellsToChange.isSuspicious()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setDoBackward(boolean doBackward) {
        if (doBackward == false) {
            throw new IllegalArgumentException("SimilarityCostManager requires backward chase");
        }
        super.setDoBackward(doBackward);
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public String getSimilarityStrategy() {
        return similarityStrategy;
    }

    public void setSimilarityStrategy(String similarityStrategy) {
        this.similarityStrategy = similarityStrategy;
    }

    @Override
    public String toString() {
        return "Similarity To Most Frequent";
    }

    @Override
    public String toLongString() {
        return toString()
                + "\n\tSimilarity strategy: " + similarityStrategy
                + "\n\tSimilarity threashold: " + similarityThreshold
                + "\n"
                + super.toString();
    }
}