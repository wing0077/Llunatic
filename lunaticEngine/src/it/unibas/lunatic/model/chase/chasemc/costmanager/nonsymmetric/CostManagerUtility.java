package it.unibas.lunatic.model.chase.chasemc.costmanager.nonsymmetric;

import it.unibas.lunatic.LunaticConstants;
import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import it.unibas.lunatic.model.chase.chasemc.ChangeDescription;
import it.unibas.lunatic.model.chase.chasemc.Repair;
import it.unibas.lunatic.model.chase.chasemc.ViolationContext;
import it.unibas.lunatic.model.chase.chasemc.operators.CellGroupIDGenerator;
import it.unibas.lunatic.model.chase.chasemc.partialorder.IPartialOrder;
import it.unibas.lunatic.model.dependency.VariableEquivalenceClass;
import it.unibas.lunatic.model.similarity.SimilarityFactory;
import it.unibas.lunatic.utility.LunaticUtility;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.database.Cell;
import speedy.model.database.IValue;
import speedy.model.database.LLUNValue;
import speedy.model.database.NullValue;

public class CostManagerUtility {

    private static Logger logger = LoggerFactory.getLogger(CostManagerUtility.class);

    public static List<CellGroup> extractConclusionCellGroupsFromContexts(List<ViolationContext> contexts) {
        Set<CellGroup> result = new HashSet<CellGroup>();
        for (ViolationContext context : contexts) {
            result.addAll(context.getAllConclusionGroups());
        }
        return new ArrayList<CellGroup>(result);
    }

    public static boolean backwardIsAllowed(Set<CellGroup> cellGroups) {
        for (CellGroup cellGroup : cellGroups) {
            if (!backwardIsAllowed(cellGroup)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canDoBackwardOnGroup(CellGroup cellGroupToChange, ViolationContext backwardContext) {
        if (!CostManagerUtility.backwardIsAllowed(cellGroupToChange)) {
            if (logger.isDebugEnabled()) logger.debug("Backward is not allowed on group " + cellGroupToChange);
            return false;
        }
//        //Checking that the backward change actually disrupts a join. (Was suspicious in previous versions)
        VariableEquivalenceClass variableEQC = findVariableEquivalenceClassForCellGroup(cellGroupToChange, backwardContext);
        Set<CellGroup> cellGroups = backwardContext.getWitnessCellGroupsForVariable(variableEQC);
        for (CellGroup cellGroup : cellGroups) {
            if (cellGroup.equals(cellGroupToChange)) {
                continue;
            }
            return true;
        }
        if (logger.isDebugEnabled()) logger.debug("Backward change doesn't disrupt a join " + cellGroupToChange);
        return false;
    }

    private static VariableEquivalenceClass findVariableEquivalenceClassForCellGroup(CellGroup witnessCellGroup, ViolationContext backwardContext) {
        for (VariableEquivalenceClass witnessVariable : backwardContext.getWitnessVariables()) {
            Set<CellGroup> cellGroups = backwardContext.getWitnessCellGroupsForVariable(witnessVariable);
            if (cellGroups.contains(witnessCellGroup)) {
                return witnessVariable;
            }
        }
        throw new IllegalArgumentException("Unable to find variable equivalence class for cell group " + witnessCellGroup + "\n\t in context " + backwardContext);
    }

    private static boolean backwardIsAllowed(CellGroup cellGroup) {
        if (cellGroup.getOccurrences().isEmpty()) {
            if (logger.isDebugEnabled()) logger.debug("Backward with empty occurrences (" + cellGroup + ") is not allowed");
            return false;
        }
        // never change LLUNs backward L(L(x)) = L(x)            
        if (cellGroup.getValue() instanceof LLUNValue || cellGroup.hasInvalidCell()) {
            if (logger.isDebugEnabled()) logger.debug("Backward on LLUN (" + cellGroup.getValue() + ") is not allowed");
            return false;
        }
        // never change equal null values          
        if (cellGroup.getValue() instanceof NullValue) {
            if (logger.isDebugEnabled()) logger.debug("Backward on Null (" + cellGroup.getValue() + ") is not allowed");
            return false;
        }
        if (!cellGroup.getAuthoritativeJustifications().isEmpty()) {
            if (logger.isDebugEnabled()) logger.debug("Backward on " + cellGroup.getValue() + " with authoritative justification " + cellGroup.getAuthoritativeJustifications() + " is not allowed");
            return false;
        }
        if (!cellGroup.getUserCells().isEmpty()) {
            if (logger.isDebugEnabled()) logger.debug("Backward on " + cellGroup.getValue() + " with user cell " + cellGroup.getUserCells() + " is not allowed");
            return false;
        }
        if (logger.isDebugEnabled()) logger.debug("Backward on " + cellGroup.getValue() + " is allowed");
        return true;
    }

    public static CellGroup getLUB(List<CellGroup> cellGroups, Scenario scenario) {
        CellGroup lub = null;
        IPartialOrder scriptPo = scenario.getScriptPartialOrder();
        if (scriptPo != null) {
            lub = scriptPo.findLUB(cellGroups, scenario);
        }
        if (lub == null) {
            lub = scenario.getPartialOrder().findLUB(cellGroups, scenario);
        }
        return lub;
    }

    public static boolean areSimilar(IValue v1, IValue v2, String similarityStrategy, double similarityThreshold) {
        if (v1 instanceof NullValue || v2 instanceof NullValue) {
            return true;
        }
        double similarity = SimilarityFactory.getInstance().getStrategy(similarityStrategy).computeSimilarity(v1, v2);
        //TODO: Handling numerical values
        try {
            double d1 = Double.parseDouble(v1.toString());
            double d2 = Double.parseDouble(v2.toString());
            similarity = 0.9;
        } catch (NumberFormatException nfe) {
        }
        //
        if (logger.isDebugEnabled()) logger.debug("Checking similarity between " + v1 + " and " + v2 + ". Result: " + similarity);
        return similarity > similarityThreshold;
    }

    public static Repair generateStandardForwardRepair(List<ViolationContext> forwardContexts, Scenario scenario) {
        Repair repair = new Repair();
        List<CellGroup> forwardCellGroups = extractConclusionCellGroupsFromContexts(forwardContexts);
        CellGroup lub = getLUB(forwardCellGroups, scenario);
        Set<Cell> contextCells = extractContextCellsFromContexts(forwardContexts);
        ChangeDescription forwardChanges = new ChangeDescription(lub, LunaticConstants.CHASE_FORWARD, contextCells);
        if (logger.isDebugEnabled()) logger.debug("Forward changes: " + forwardChanges);
        repair.addViolationContext(forwardChanges);
        return repair;
    }

    public static Repair generateRepairWithBackwards(List<ViolationContext> forwardContexts, List<CellGroup> backwardCellGroups, List<ViolationContext> backwardContexts, Scenario scenario) {
        if (logger.isDebugEnabled()) logger.debug("Generating repair with forward contexts: " + LunaticUtility.printViolationContextIDs(forwardContexts)
                    + "\n\tbackward contexts: " + LunaticUtility.printViolationContextIDs(backwardContexts)
                    + "\n\tbackward combination: " + backwardCellGroups);
        Repair repair;
        if (!forwardContexts.isEmpty()) {
            repair = CostManagerUtility.generateStandardForwardRepair(forwardContexts, scenario);
        } else {
            repair = new Repair();
        }
        for (CellGroup backwardGroup : backwardCellGroups) {
            if (hasBeenChanged(backwardGroup, repair)) {
                continue;
            }
            CellGroup backwardCellGroup = backwardGroup.clone();
            LLUNValue llunValue = CellGroupIDGenerator.getNextLLUNID();
            backwardCellGroup.setValue(llunValue);
            backwardCellGroup.setInvalidCell(CellGroupIDGenerator.getNextInvalidCell());
            Set<Cell> contextCells = CostManagerUtility.extractContextCellsFromContexts(backwardContexts);
            ChangeDescription backwardChangesForGroup = new ChangeDescription(backwardCellGroup, LunaticConstants.CHASE_BACKWARD, contextCells);
            repair.addViolationContext(backwardChangesForGroup);
        }
        return repair;
    }

    private static boolean hasBeenChanged(CellGroup backwardGroup, Repair repair) {
        for (ChangeDescription changeDescription : repair.getChangeDescriptions()) {
            if (changeDescription.getCellGroup().getOccurrences().containsAll(backwardGroup.getOccurrences())) {
                return true;
            }
        }
        return false;
    }

    private static Set<Cell> extractContextCellsFromContexts(List<ViolationContext> contexts) {
        Set<Cell> result = new HashSet<Cell>();
        for (ViolationContext context : contexts) {
            Set<Cell> cellsForContext = extractAllCellsFromContext(context);
            result.addAll(cellsForContext);
        }
        return result;
    }

    public static Set<Cell> extractAllCellsFromContext(ViolationContext context) {
        Set<Cell> result = new HashSet<Cell>();
        for (CellGroup conclusionGroup : context.getAllConclusionGroups()) {
            result.addAll(conclusionGroup.getOccurrences());
        }
        for (CellGroup witnessCellGroup : context.getAllWitnessCellGroups()) {
            result.addAll(witnessCellGroup.getOccurrences());
        }
        return result;
    }
}