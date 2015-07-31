package it.unibas.lunatic.model.chase.chasemc.costmanager;

import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import it.unibas.lunatic.model.chase.chasemc.EGDEquivalenceClassCells;
import java.util.Comparator;

class TupleGroupComparator implements Comparator<EGDEquivalenceClassCells> {

    //V0
//    public int compare(TupleGroup t1, TupleGroup t2) {
//        int sizeDifference = t1.getConclusionGroup().getOccurrences().size() - t2.getConclusionGroup().getOccurrences().size();
//        if (sizeDifference != 0) {
//            return sizeDifference;
//        }
//        return t1.getConclusionGroup().getValue().toString().compareTo(t2.getConclusionGroup().getValue().toString());
//    }
        
//    public int compare(EGDEquivalenceClassCells t1, EGDEquivalenceClassCells t2) {
//        int sizeDifference = getOccurrencesAndProvenances(t1) - getOccurrencesAndProvenances(t2);
//        if (sizeDifference != 0) {
//            return sizeDifference;
//        }
//        return t1.getCellGroupForForwardRepair().getValue().toString().compareTo(t2.getCellGroupForForwardRepair().getValue().toString());
//    }
//
//    private int getOccurrencesAndProvenances(EGDEquivalenceClassCells t) {
//        int count = 0;
//        for (Set<Cell> witnessCells : t.getWitnessCells().values()) {
//            count += witnessCells.size();
//        }
//        return count;
//    }

    //V2 TODO++ check
    public int compare(EGDEquivalenceClassCells t1, EGDEquivalenceClassCells t2) {
        int sizeDifference = size(t1.getCellGroupForForwardRepair()) - size(t2.getCellGroupForForwardRepair());
        if (sizeDifference != 0) {
            return sizeDifference;
        }
        return t1.getCellGroupForForwardRepair().getValue().toString().compareTo(t2.getCellGroupForForwardRepair().getValue().toString());
    }
    
    private int size(CellGroup cellGroup) {
        return cellGroup.getOccurrences().size() + cellGroup.getJustifications().size() + cellGroup.getUserCells().size();
    }
}