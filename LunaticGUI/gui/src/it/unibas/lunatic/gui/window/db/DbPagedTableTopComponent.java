package it.unibas.lunatic.gui.window.db;

import it.unibas.lunatic.gui.R;
import it.unibas.lunatic.gui.node.ITupleFactory;
import it.unibas.lunatic.gui.node.TableNode;
import it.unibas.lunatic.gui.node.TableTupleNode;
import it.unibas.lunatic.gui.node.TupleGenerationStrategy;
import it.unibas.lunatic.gui.table.TablePopupFactory;
import it.unibas.lunatic.gui.window.db.actions.ActionFirstPage;
import it.unibas.lunatic.gui.window.db.actions.ActionGotoPage;
import it.unibas.lunatic.gui.window.db.actions.ActionLastPage;
import it.unibas.lunatic.gui.window.db.actions.ActionNextPage;
import it.unibas.lunatic.gui.window.db.actions.ActionPreviousPage;
import it.unibas.lunatic.model.database.Cell;
import java.lang.reflect.InvocationTargetException;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import org.netbeans.swing.etable.ETable;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;

/**
 * Top component which displays something.
 */
//@ConvertAsProperties(
//        dtd = "-//it.unibas.lunatic.gui//DbTable//EN",
//        autostore = false)
@TopComponent.Description(
        preferredID = R.Window.PAGED_TABLE,
        persistenceType = TopComponent.PERSISTENCE_NEVER)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
public final class DbPagedTableTopComponent extends TableWindow implements PagedTableView {

    private TupleGenerationStrategy tupleGenerationStrategy = TupleGenerationStrategy.getInstance();
    private TablePaginationSupport tablePaginationSupport;
    private ITupleFactory tupleFactory;
    private TableNode tableNode;
    private int offset;
    private int pageSize = 1000;

    public DbPagedTableTopComponent(TableNode tableNode, String name) {
        this.tableNode = tableNode;
        initComponents();
        initTable();
        setToolTipText(Bundle.HINT_DbTableTopComponent());
        associateExplorerLookup();
        tablePaginationSupport = new TablePaginationSupport();
        btnFirst.setAction(new ActionFirstPage(this, tablePaginationSupport));
        btnLast.setAction(new ActionLastPage(this, tablePaginationSupport));
        btnNext.setAction(new ActionNextPage(this, tablePaginationSupport));
        btnPrevious.setAction(new ActionPreviousPage(this, tablePaginationSupport));
        txtPage.setAction(new ActionGotoPage(this, tablePaginationSupport));
        setName(name);
        setColumns(tableNode);
        lblRowNumber.setText(tableNode.getTable().getSize() + "");
        updateTable();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        outlineView1 = new org.openide.explorer.view.OutlineView();
        jPanel1 = new javax.swing.JPanel();
        btnFirst = new javax.swing.JButton();
        btnPrevious = new javax.swing.JButton();
        btnLast = new javax.swing.JButton();
        btnNext = new javax.swing.JButton();
        txtPage = new javax.swing.JTextField();
        lblRowText = new javax.swing.JLabel();
        lblRowNumber = new javax.swing.JLabel();
        lblQuery = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        outlineView1.setDragSource(false);
        outlineView1.setDropTarget(false);
        add(outlineView1, java.awt.BorderLayout.CENTER);

        org.openide.awt.Mnemonics.setLocalizedText(btnFirst, org.openide.util.NbBundle.getMessage(DbPagedTableTopComponent.class, "DbPagedTableTopComponent.btnFirst.text")); // NOI18N
        btnFirst.setPreferredSize(new java.awt.Dimension(24, 24));

        org.openide.awt.Mnemonics.setLocalizedText(btnPrevious, org.openide.util.NbBundle.getMessage(DbPagedTableTopComponent.class, "DbPagedTableTopComponent.btnPrevious.text")); // NOI18N
        btnPrevious.setPreferredSize(new java.awt.Dimension(24, 24));

        org.openide.awt.Mnemonics.setLocalizedText(btnLast, org.openide.util.NbBundle.getMessage(DbPagedTableTopComponent.class, "DbPagedTableTopComponent.btnLast.text")); // NOI18N
        btnLast.setPreferredSize(new java.awt.Dimension(24, 24));

        org.openide.awt.Mnemonics.setLocalizedText(btnNext, org.openide.util.NbBundle.getMessage(DbPagedTableTopComponent.class, "DbPagedTableTopComponent.btnNext.text")); // NOI18N
        btnNext.setPreferredSize(new java.awt.Dimension(24, 24));

        txtPage.setText(org.openide.util.NbBundle.getMessage(DbPagedTableTopComponent.class, "DbPagedTableTopComponent.txtPage.text")); // NOI18N
        txtPage.setPreferredSize(new java.awt.Dimension(36, 22));

        lblRowText.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(lblRowText, org.openide.util.NbBundle.getMessage(DbPagedTableTopComponent.class, "DbPagedTableTopComponent.lblRowText.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lblRowNumber, org.openide.util.NbBundle.getMessage(DbPagedTableTopComponent.class, "DbPagedTableTopComponent.lblRowNumber.text")); // NOI18N

        lblQuery.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(lblQuery, org.openide.util.NbBundle.getMessage(DbPagedTableTopComponent.class, "DbPagedTableTopComponent.lblQuery.text")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnFirst, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(btnPrevious, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnNext, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(btnLast, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblQuery, javax.swing.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblRowText, javax.swing.GroupLayout.DEFAULT_SIZE, 41, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblRowNumber)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(lblQuery, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnPrevious, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnFirst, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnLast, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnNext, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblRowText, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblRowNumber, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtPage, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(8, 8, 8))
        );

        add(jPanel1, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnFirst;
    private javax.swing.JButton btnLast;
    private javax.swing.JButton btnNext;
    private javax.swing.JButton btnPrevious;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblQuery;
    private javax.swing.JLabel lblRowNumber;
    private javax.swing.JLabel lblRowText;
    private org.openide.explorer.view.OutlineView outlineView1;
    private javax.swing.JTextField txtPage;
    // End of variables declaration//GEN-END:variables
    private LoadedScenarioListener scenarioListener = new LoadedScenarioListener();

    @Override
    public void componentOpened() {
        scenarioListener.register(this);
    }

    @Override
    public void componentClosed() {
        scenarioListener.remove();
    }

    void writeProperties(java.util.Properties p) {
        p.setProperty("version", "1.4");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }

    private void initTable() {
        outlineView1.setNodePopupFactory(new TablePopupFactory());
        Outline outline = outlineView1.getOutline();
        outline.setRootVisible(false);
        outline.setCellSelectionEnabled(true);
//        outline.setAutoResizeMode(ETable.AUTO_RESIZE_OFF);
        outline.setAutoResizeMode(ETable.AUTO_RESIZE_ALL_COLUMNS);
        ((DefaultOutlineModel) outline.getOutlineModel()).setNodesColumnLabel("tid");
        outline.setFullyNonEditable(true);
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return explorer;
    }

    @Override
    public void updateTable() {
        updatePage(offset);
        //SORT BY ID
        outlineView1.getOutline().setColumnSorted(0, true, 1);
    }

    @Override
    public void updatePage(int newOffset) {
        this.offset = newOffset;
        tupleFactory = tupleGenerationStrategy.getPagedFactory(tableNode, offset, pageSize);
        explorer.setRootContext(tupleFactory.createTuples());
        lblQuery.setText(getPaginationQuery());
        updatePageText();
    }

    public void setColumns(TableNode ts) {
        outlineView1.setPropertyColumns();
        for (String col : ts.getVisibleColumns()) {
            outlineView1.addPropertyColumn(col, col);
        }
    }

    @Override
    public TableTupleNode getSelectedNode() {
        Node[] nodes = explorer.getSelectedNodes();
        if (nodes.length == 1) {
            return (TableTupleNode) nodes[0];
        }
        return null;
    }

    @Override
    public Cell getSelectedCell() throws IllegalAccessException, InvocationTargetException {
        Outline o = outlineView1.getOutline();
        int selectedColumn = o.getSelectedColumn();
        if (selectedColumn > 0) {
            String columnName = o.getColumnName(selectedColumn);
            TableTupleNode node = getSelectedNode();
            return node.getCell(columnName);
        }
        return null;
    }

    @Override
    public void setRootContext(Node node) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void removeRootContext() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public TableNode getTableNode() {
        return tableNode;
    }

    public String getPageText() {
        return txtPage.getText();
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getPaginationQuery() {
        return tableNode.getTable().getPaginationQuery(offset, offset + pageSize);
    }

    @Override
    public int getTableSize() {
        return tableNode.getTable().getSize();
    }

    public void updatePageText() {
        txtPage.setText(tablePaginationSupport.getCurrentPageNumber(offset, pageSize) + "");
    }
}
