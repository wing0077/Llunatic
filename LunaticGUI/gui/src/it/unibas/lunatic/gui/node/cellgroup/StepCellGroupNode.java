package it.unibas.lunatic.gui.node.cellgroup;

import it.unibas.lunatic.gui.R;
import it.unibas.lunatic.gui.node.chase.mc.ChaseStepNode;
import it.unibas.lunatic.gui.node.utils.StringProperty;
import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import speedy.model.database.LLUNValue;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import java.lang.reflect.InvocationTargetException;
import javax.swing.Action;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openide.awt.Actions;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;

public class StepCellGroupNode extends AbstractNode {

    private Log logger = LogFactory.getLog(getClass());
    private CellGroup cellGroup;
    private ChaseStepNode chaseStep;
    boolean edited;

    public StepCellGroupNode(CellGroup key, ChaseStepNode chaseStep) {
        super(Children.LEAF);
        setName(key.getValue().toString());
        setDisplayName(key.getValue().toString());
        this.cellGroup = key;
        this.chaseStep = chaseStep;
        updateIcon(false);
    }

    private void updateIcon(boolean fireChange) {
        if (cellGroup.getValue() instanceof LLUNValue) {
            setIconBaseWithExtension("it/unibas/lunatic/icons/cg-llun.png");
        } else {
            setIconBaseWithExtension("it/unibas/lunatic/icons/cg-value.png");
        }
        if (fireChange) {
            fireIconChange();
        }
    }

    @Override
    public Action[] getActions(boolean context) {
        Action[] actions;
        if (chaseStep.getChaseStep().isEditedByUser()) {
            actions = new Action[]{
                Actions.forID("Window", R.ActionId.SHOW_CELL_GROUP_DETAILS),
                Actions.forID("Edit", R.ActionId.EDIT_CELL_GROUP_VALUE)
            };
        } else {
            actions = new Action[]{
                Actions.forID("Window", R.ActionId.SHOW_CELL_GROUP_DETAILS)
            };
        }
        return actions;
    }

    public void setUserCellGroup(CellGroup editedResult) {
        assert chaseStep.getChaseStep().isEditedByUser();
        String oldValue = this.cellGroup.getValue().toString();
        String newValue = editedResult.getValue().toString();
        this.cellGroup = editedResult;
        this.chaseStep.refreshCellGroups();
        edited = true;
        logger.debug("Cell group value: " + newValue);
        setDisplayName(newValue);
        updateIcon(true);
    }
//
//    @Override
//    public String getDisplayName() {
//        return null;
//    }
//
//    @Override
//    public String getHtmlDisplayName() {
//        if (edited) {
//            return "<html><font color=\"#FF0000\">" + getDisplayName() + "</font></html>";
//        }
//        return this.cellGroup.getValue().toString();
//    }

    public CellGroup getCellGroup() {
        return cellGroup;
    }

    public DeltaChaseStep getChaseStep() {
        return chaseStep.getChaseStep();
    }

    public ChaseStepNode getChaseStepNode() {
        return chaseStep;
    }

    @Override
    public Action getPreferredAction() {
        return getActions(true)[0];
    }

    public String getValue() {
        return cellGroup.getValue().toString();
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        sheet.put(set);
        set.put(new StringProperty("Value") {
            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                return cellGroup.getValue().toString();
            }
        });
        set.put(new StringProperty("Occurrences") {
            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                return cellGroup.getOccurrences().size() + "";
            }
        });
        set.put(new StringProperty("Justifications") {
            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                return cellGroup.getJustifications().size() + "";
            }
        });
        set.put(new StringProperty("User Cells") {
            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                return cellGroup.getUserCells().size() + "";
            }
        });
        set.put(new StringProperty("Invalid Cell") {
            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                return (cellGroup.hasInvalidCell() ? "Yes" : "No");
            }
        });
        set.put(new StringProperty("Additional Cells") {
            @Override
            public String getValue() throws IllegalAccessException, InvocationTargetException {
                return cellGroup.getAdditionalCells().size() + "";
            }
        });
        return sheet;
    }
}
