package it.unibas.lunatic.model.chase.chasemc.operators;

import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import it.unibas.lunatic.model.chase.chasemc.EGDEquivalenceClassCells;
import speedy.model.database.IValue;
import it.unibas.lunatic.utility.LunaticUtility;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckSatisfactionAfterUpgradesEGD {

    private static final Logger logger = LoggerFactory.getLogger(CheckSatisfactionAfterUpgradesEGD.class.getName());

    public boolean isSatisfiedAfterUpgrades(List<EGDEquivalenceClassCells> tupleGroups, Scenario scenario) {
        if (logger.isDebugEnabled()) logger.debug("Checking violations between tuple groups\n" + LunaticUtility.printCollection(tupleGroups));
        List<CellGroup> cellGroups = CellGroupUtility.extractCellGroups(tupleGroups);
        return isSatisfiedAfterUpgrades(cellGroups);
    }

    public boolean isSatisfiedAfterUpgrades(List<CellGroup> cellGroups) {
        Set<IValue> differentValues = CellGroupUtility.findDifferentValuesInCellGroupsWithOccurrences(cellGroups);
        if (differentValues.size() > 1) {
            return false;
        }
        return CellGroupUtility.checkContainment(cellGroups);
    }

}