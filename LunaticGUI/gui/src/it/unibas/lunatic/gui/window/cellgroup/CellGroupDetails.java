package it.unibas.lunatic.gui.window.cellgroup;

import it.unibas.lunatic.gui.R;
import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import org.openide.awt.Actions;

public class CellGroupDetails extends javax.swing.JPanel implements Cloneable {

    public CellGroupDetails() {
        initComponents();
        editButton.setAction(Actions.forID("Edit", R.ActionId.EDIT_CELL_GROUP_VALUE));
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        lblValue = new javax.swing.JLabel();
        cgValue = new javax.swing.JLabel();
        editButton = new javax.swing.JButton();
        checkBoxInvalidCell = new javax.swing.JCheckBox();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));

        org.openide.awt.Mnemonics.setLocalizedText(lblValue, org.openide.util.NbBundle.getMessage(CellGroupDetails.class, "CellGroupDetails.lblValue.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(cgValue, org.openide.util.NbBundle.getMessage(CellGroupDetails.class, "CellGroupDetails.cgValue.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(editButton, org.openide.util.NbBundle.getMessage(CellGroupDetails.class, "CellGroupDetails.editButton.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(checkBoxInvalidCell, org.openide.util.NbBundle.getMessage(CellGroupDetails.class, "CellGroupDetails.checkBoxInvalidCell.text")); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(lblValue, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cgValue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(checkBoxInvalidCell, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editButton)))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblValue)
                    .addComponent(cgValue))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(editButton)
                    .addComponent(checkBoxInvalidCell))
                .addGap(0, 0, 0))
        );

        add(mainPanel);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel cgValue;
    private javax.swing.JCheckBox checkBoxInvalidCell;
    private javax.swing.JButton editButton;
    private javax.swing.JLabel lblValue;
    private javax.swing.JPanel mainPanel;
    // End of variables declaration//GEN-END:variables

    public void setCellGroupValue(CellGroup cellGroup) {
        cgValue.setText(cellGroup.getValue().toString());
        checkBoxInvalidCell.setSelected(cellGroup.hasInvalidCell());
        for (ActionListener actionListener : checkBoxInvalidCell.getActionListeners()) {
            checkBoxInvalidCell.removeActionListener(actionListener);
        }
        checkBoxInvalidCell.addActionListener(new KeepOriginalStatus(cellGroup.hasInvalidCell()));
    }

    private static class KeepOriginalStatus implements ActionListener {

        private boolean originalStatuts;

        public KeepOriginalStatus(boolean originalStatuts) {
            this.originalStatuts = originalStatuts;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBox checkBox = (JCheckBox) e.getSource();
            checkBox.setSelected(originalStatuts);
        }
    }
}
