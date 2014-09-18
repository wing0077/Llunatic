package it.unibas.lunatic.model.chase.chasemc.costmanager;

import it.unibas.lunatic.LunaticConfiguration;
import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.utility.LunaticUtility;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.exceptions.ChaseException;
import it.unibas.lunatic.model.chase.chasemc.BackwardAttribute;
import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import it.unibas.lunatic.model.chase.chasemc.ChangeSet;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import it.unibas.lunatic.model.chase.chasemc.EquivalenceClass;
import it.unibas.lunatic.model.chase.chasemc.Repair;
import it.unibas.lunatic.model.chase.chasemc.TargetCellsToChange;
import it.unibas.lunatic.model.chase.chasemc.operators.CellGroupIDGenerator;
import it.unibas.lunatic.model.chase.chasemc.operators.IValueOccurrenceHandlerMC;
import it.unibas.lunatic.model.database.IDatabase;
import it.unibas.lunatic.model.database.LLUNValue;
import it.unibas.lunatic.model.database.NullValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrequencyPartitionCostManager extends AbstractCostManager {

    private static Logger logger = LoggerFactory.getLogger(FrequencyPartitionCostManager.class);

    private double lowFrequencyThreshold = 0.1;
    private int lowFrequencyValue = 1;
    private double highFrequencyThreshold = 0.5;
    private double highFrequencyDifferenceThreshold = 0.2;

    @SuppressWarnings("unchecked")
    public List<Repair> chooseRepairStrategy(EquivalenceClass equivalenceClass, DeltaChaseStep chaseTreeRoot,
            List<Repair> repairsForDependency, Scenario scenario, String stepId,
            IValueOccurrenceHandlerMC occurrenceHandler) {
        if (isDoBackward() && !isDoPermutations()) {
            throw new ChaseException("SinglePermutation CostManager works must return singleton repairs. Configuration with doBackward and not doPermutations is not allowed with StandardCostManager");
        }
        if (logger.isDebugEnabled()) logger.debug("########?Choosing repair strategy for equivalence class: " + equivalenceClass);
        List<TargetCellsToChange> tupleGroups = equivalenceClass.getTupleGroups();
        if (isNotViolation(tupleGroups, scenario)) {
            return Collections.EMPTY_LIST;
        }
        List<Repair> result = new ArrayList<Repair>();
        // generate forward repair for all groups
        ChangeSet changesForForwardRepair = generateForwardRepair(equivalenceClass.getTupleGroups(), scenario, chaseTreeRoot.getDeltaDB(), stepId);
        Repair forwardRepair = new Repair();
        forwardRepair.addChanges(changesForForwardRepair);
        if (logger.isDebugEnabled()) logger.debug("########Forward repair: " + forwardRepair);
        result.add(forwardRepair);
        if (isDoBackward()) {
            // check if repairs with backward chasing are possible
            int chaseBranching = chaseTreeRoot.getNumberOfLeaves();
//            int repairsForDependenciesSize = repairsForDependency.size();
            int potentialSolutions = chaseTreeRoot.getPotentialSolutions();
//            int databaseSize = scenario.getTarget().getSize();
//            if (isTreeSizeBelowThreshold(chaseTreeSize, potentialSolutions, repairsForDependenciesSize)) {
            if (isTreeSizeBelowThreshold(chaseBranching, potentialSolutions)) {
                int equivalenceClassSize = calculateSize(equivalenceClass);
                List<TargetCellsToChange> lowFrequencyGroups = new ArrayList<TargetCellsToChange>();
                List<TargetCellsToChange> mediumFrequencyGroups = new ArrayList<TargetCellsToChange>();
                List<TargetCellsToChange> highFrequencyGroups = new ArrayList<TargetCellsToChange>();
                partitionGroups(equivalenceClass, equivalenceClassSize, scenario, lowFrequencyGroups, mediumFrequencyGroups, highFrequencyGroups);
                List<Repair> backwardRepairs = generateBackwardRepairs(equivalenceClass, lowFrequencyGroups, mediumFrequencyGroups, highFrequencyGroups,
                        scenario, chaseTreeRoot.getDeltaDB(), stepId, occurrenceHandler);
                result.addAll(backwardRepairs);
                if (logger.isDebugEnabled()) logger.debug("########Backward repairs: " + backwardRepairs);
            }
        }
        return result;
    }

    private int calculateSize(EquivalenceClass equivalenceClass) {
        int size = 0;
        for (TargetCellsToChange tupleGroup : equivalenceClass.getTupleGroups()) {
            size += tupleGroup.getOccurrenceSize();
        }
        return size;
    }

    private void partitionGroups(EquivalenceClass equivalenceClass, int equivalenceClassSize, Scenario scenario,
            List<TargetCellsToChange> lowFrequencyGroups, List<TargetCellsToChange> mediumFrequencyGroups, List<TargetCellsToChange> highFrequencyGroups) {
        for (int i = 0; i < equivalenceClass.getTupleGroups().size(); i++) {
            TargetCellsToChange tupleGroup = equivalenceClass.getTupleGroups().get(i);
            if (isLowFrequency(tupleGroup, equivalenceClassSize, scenario.getConfiguration())) {
                lowFrequencyGroups.add(tupleGroup);
                continue;
            }
            if (i != 0) {
                TargetCellsToChange previousGroup = equivalenceClass.getTupleGroups().get(i - 1);
                if (isHighFrequency(tupleGroup, previousGroup, equivalenceClassSize, scenario.getConfiguration())) {
                    highFrequencyGroups.add(tupleGroup);
                    continue;
                }
            }
            mediumFrequencyGroups.add(tupleGroup);
        }
        if (logger.isDebugEnabled()) logger.debug("Low frequency groups: " + LunaticUtility.printCollection(lowFrequencyGroups));
        if (logger.isDebugEnabled()) logger.debug("Medium frequency groups: " + LunaticUtility.printCollection(mediumFrequencyGroups));
        if (logger.isDebugEnabled()) logger.debug("High frequency groups: " + LunaticUtility.printCollection(highFrequencyGroups));
    }

    private boolean isLowFrequency(TargetCellsToChange tupleGroup, int equivalenceClassSize, LunaticConfiguration configuration) {
        double frequency = ((double) tupleGroup.getOccurrenceSize()) / equivalenceClassSize;
        return (frequency < lowFrequencyThreshold || tupleGroup.getOccurrenceSize() <= lowFrequencyValue);
    }

    private boolean isHighFrequency(TargetCellsToChange tupleGroup, TargetCellsToChange previousGroup, int equivalenceClassSize, LunaticConfiguration configuration) {
        double frequency = ((double) tupleGroup.getOccurrenceSize()) / equivalenceClassSize;
        double previousFrequency = ((double) previousGroup.getOccurrenceSize()) / equivalenceClassSize;
        double increase = frequency - previousFrequency;
        return (frequency >= highFrequencyThreshold && increase > highFrequencyDifferenceThreshold);
    }

    private List<Repair> generateBackwardRepairs(EquivalenceClass equivalenceClass, List<TargetCellsToChange> lowFrequencyGroups, List<TargetCellsToChange> mediumFrequencyGroups, List<TargetCellsToChange> highFrequencyGroups, Scenario scenario, IDatabase deltaDB, String stepId, IValueOccurrenceHandlerMC occurrenceHandler) {
        List<Repair> result = new ArrayList<Repair>();
        if (logger.isDebugEnabled()) logger.debug("Generating backward repairs for groups:\n" + "High frequency:\n" + highFrequencyGroups + "Medium frequency:\n" + mediumFrequencyGroups + "Low frequency:\n" + lowFrequencyGroups);
        for (TargetCellsToChange lowFrequencyGroup : lowFrequencyGroups) {
            for (BackwardAttribute premiseAttribute : lowFrequencyGroup.getCellGroupsForBackwardAttributes().keySet()) {
                List<TargetCellsToChange> forwardGroups = new ArrayList<TargetCellsToChange>();
                forwardGroups.addAll(mediumFrequencyGroups);
                forwardGroups.addAll(highFrequencyGroups);
                forwardGroups.addAll(lowFrequencyGroups);
                List<TargetCellsToChange> backwardGroups = new ArrayList<TargetCellsToChange>();
                CellGroup cellGroup = lowFrequencyGroup.getCellGroupsForBackwardAttributes().get(premiseAttribute);
                if (backwardIsAllowed(cellGroup, occurrenceHandler, deltaDB, stepId)) {
                    backwardGroups.add(lowFrequencyGroup);
                    forwardGroups.remove(lowFrequencyGroup);
                }
                if (backwardGroups.isEmpty()) {
                    continue;
                }
                Repair repair = generateRepairWithBackwards(equivalenceClass, forwardGroups, backwardGroups, premiseAttribute, scenario, deltaDB, stepId);
                result.add(repair);
            }
        }
        return result;
    }

    private boolean backwardIsAllowed(CellGroup cellGroup, IValueOccurrenceHandlerMC occurrenceHandler, IDatabase deltaDB, String stepId) {
        // never change LLUNs backward L(L(x)) = L(x)            
        if (cellGroup.getValue() instanceof LLUNValue) {
            if (logger.isDebugEnabled()) logger.debug("Backward on LLUN (" + cellGroup.getValue() + ") is not allowed");
            return false;
        }
        // never change equal null values          
        if (cellGroup.getValue() instanceof NullValue) {
            if (logger.isDebugEnabled()) logger.debug("Backward on Null (" + cellGroup.getValue() + ") is not allowed");
            return false;
        }
        if (!cellGroup.getProvenances().isEmpty()) {
            if (logger.isDebugEnabled()) logger.debug("Backward on " + cellGroup.getValue() + " with provenance " + cellGroup.getProvenances() + " is not allowed");
            return false;
        }
        if (logger.isDebugEnabled()) logger.debug("Backward on " + cellGroup.getValue() + " is allowed");
        return true;
    }

    private Repair generateRepairWithBackwards(EquivalenceClass equivalenceClass, List<TargetCellsToChange> forwardGroups, List<TargetCellsToChange> backwardGroups, BackwardAttribute premiseAttribute,
            Scenario scenario, IDatabase deltaDB, String stepId) {
        Repair repair = new Repair();
        if (forwardGroups.size() > 1) {
            ChangeSet forwardChanges = generateForwardRepair(forwardGroups, scenario, deltaDB, stepId);
            repair.addChanges(forwardChanges);
        }
        for (TargetCellsToChange backwardGroup : backwardGroups) {
            CellGroup cellGroup = backwardGroup.getCellGroupsForBackwardAttributes().get(premiseAttribute);
//            int llunId = ChaseUtility.generateLLUNId(cellGroup);
//            LLUNValue llunValue = new LLUNValue(LunaticConstants.LLUN_PREFIX + LunaticConstants.CHASE_BACKWARD + llunId);
            LLUNValue llunValue = CellGroupIDGenerator.getNextLLUNID();
            CellGroup cellsTochange = new CellGroup(llunValue, true);
            cellsTochange.getOccurrences().addAll(cellGroup.getOccurrences());
            ChangeSet backwardChangesForGroup = new ChangeSet(cellsTochange, LunaticConstants.CHASE_BACKWARD, buildWitnessCellGroups(backwardGroups));
//            ChangeSet backwardChangesForGroup = new ChangeSet(cellsTochange, LunaticConstants.CHASE_BACKWARD, premiseAttribute);
            repair.addChanges(backwardChangesForGroup);
        }
        return repair;
    }

    @Override
    public String toString() {
        return "Frequency partition";
    }
}