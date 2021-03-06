package it.unibas.lunatic.model.chase.chasemc.operators;

import it.unibas.lunatic.PartialOrderConstants;
import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import it.unibas.lunatic.model.chase.chasemc.CellGroupCell;
import it.unibas.lunatic.model.chase.commons.operators.ChaseUtility;
import it.unibas.lunatic.utility.LunaticUtility;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.database.AttributeRef;
import speedy.model.database.IValue;

public class CorrectCellGroupID implements ICorrectCellGroupID {

    private static Logger logger = LoggerFactory.getLogger(CorrectCellGroupID.class);

    @Override
    public void correctCellGroupId(CellGroup cellGroup) {
        if (logger.isDebugEnabled()) logger.debug("Correcting cell group id/value for cell group:\n" + cellGroup.toLongString());
        IValue mostFrequentCellGroupId = findMostFrequentCellGroupId(cellGroup);
        if (mostFrequentCellGroupId != null) {
            if (logger.isDebugEnabled()) logger.debug("Using existingClusterId " + mostFrequentCellGroupId);
            cellGroup.setId(mostFrequentCellGroupId);
        }
        correctToSaveInCells(cellGroup.getOccurrences(), mostFrequentCellGroupId, cellGroup.getId());
        correctToSaveInCells(cellGroup.getJustifications(), mostFrequentCellGroupId, cellGroup.getId());
        correctToSaveInCells(cellGroup.getUserCells(), mostFrequentCellGroupId, cellGroup.getId());
        if (cellGroup.hasInvalidCell()) {
            correctToSaveInCell(cellGroup.getInvalidCell(), mostFrequentCellGroupId, cellGroup.getId());
        }
        for (AttributeRef attributeRef : cellGroup.getAdditionalCells().keySet()) {
            Set<CellGroupCell> additionalCells = cellGroup.getAdditionalCells().get(attributeRef);
            correctToSaveInCells(additionalCells, mostFrequentCellGroupId, cellGroup.getId());
        }
        if (logger.isDebugEnabled()) logger.debug("Result " + cellGroup.toLongString());
    }

    private IValue findMostFrequentCellGroupId(CellGroup cellGroup) {
        Map<IValue, Integer> variableOccurrenceHistogram = buildOccurrenceHistogramForCellGroupId(cellGroup, cellGroup.getId());
        if (logger.isDebugEnabled()) logger.debug("Occurrence histogram: " + LunaticUtility.printMap(variableOccurrenceHistogram));
        if (variableOccurrenceHistogram.isEmpty()) {
            return null;
        }
        return ChaseUtility.findFirstOrderedValue(variableOccurrenceHistogram);
    }

    private Map<IValue, Integer> buildOccurrenceHistogramForCellGroupId(CellGroup cellGroup, IValue cellGroupID) {
        Map<IValue, Integer> result = new HashMap<IValue, Integer>();
        computeFrequencyOfCellGroupId(cellGroup.getOccurrences(), cellGroupID, result);
        computeFrequencyOfCellGroupId(cellGroup.getJustifications(), cellGroupID, result);
        computeFrequencyOfCellGroupId(cellGroup.getUserCells(), cellGroupID, result);
        for (AttributeRef attributeRef : cellGroup.getAdditionalCells().keySet()) {
            Set<CellGroupCell> additionalCells = cellGroup.getAdditionalCells().get(attributeRef);
            computeFrequencyOfCellGroupId(additionalCells, cellGroupID, result);
        }
        return result;
    }

    private void computeFrequencyOfCellGroupId(Set<CellGroupCell> cells, IValue lubCellGroupId, Map<IValue, Integer> result) {
        for (CellGroupCell cell : cells) {
            IValue originalCellGroupId = cell.getLastSavedCellGroupId();
            if (originalCellGroupId == null || !isCompatible(originalCellGroupId, lubCellGroupId)) {
                continue;
            }
            Integer occurrences = result.get(originalCellGroupId);
            if (occurrences == null) {
                result.put(originalCellGroupId, 1);
            } else {
                occurrences++;
                result.put(originalCellGroupId, occurrences);
            }
        }
    }

    private boolean isCompatible(IValue originalCellGroupId, IValue lubCellGroupId) {
        if (!originalCellGroupId.getType().equals(lubCellGroupId.getType())) {
            return false;
        }
        if (originalCellGroupId.getType().equals(PartialOrderConstants.NULL)
                || originalCellGroupId.getType().equals(PartialOrderConstants.LLUN)) {
            return true;
        }
        IValue originalConstantValue = CellGroupIDGenerator.getCellGroupValueFromGroupID(originalCellGroupId);
        IValue lubConstantValue = CellGroupIDGenerator.getCellGroupValueFromGroupID(lubCellGroupId);
        return (originalConstantValue.equals(lubConstantValue));
    }

    private void correctToSaveInCells(Set<CellGroupCell> cells, IValue mostFrequentCellGroupId, IValue lubCellGroupId) {
        for (CellGroupCell cell : cells) {
            correctToSaveInCell(cell, mostFrequentCellGroupId, lubCellGroupId);
        }
    }

    private void correctToSaveInCell(CellGroupCell cell, IValue mostFrequentCellGroupId, IValue lubCellGroupId) {
        if (mostFrequentCellGroupId != null) {
            if (cell.getLastSavedCellGroupId() != null && cell.getLastSavedCellGroupId().equals(mostFrequentCellGroupId)) {
                cell.setToSave(false);
            } else {
                cell.setToSave(true);
                cell.setLastSavedCellGroupId(mostFrequentCellGroupId); // new cell group id will be saved
            }
        } else {
            cell.setToSave(true);
            cell.setLastSavedCellGroupId(lubCellGroupId); // new cell group id will be saved
        }
    }
}
