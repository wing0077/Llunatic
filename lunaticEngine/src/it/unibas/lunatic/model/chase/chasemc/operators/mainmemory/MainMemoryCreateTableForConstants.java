package it.unibas.lunatic.model.chase.chasemc.operators.mainmemory;

import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.model.chase.chasemc.operators.ICreateTablesForConstants;
import it.unibas.lunatic.model.dependency.AllConstantsInFormula;
import it.unibas.lunatic.utility.LunaticUtility;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.model.database.EmptyDB;
import speedy.model.database.mainmemory.MainMemoryDB;
import speedy.model.database.mainmemory.datasource.DataSource;
import speedy.model.database.mainmemory.datasource.INode;
import speedy.model.database.mainmemory.datasource.IntegerOIDGenerator;
import speedy.model.database.mainmemory.datasource.nodes.AttributeNode;
import speedy.model.database.mainmemory.datasource.nodes.LeafNode;
import speedy.model.database.mainmemory.datasource.nodes.SetNode;
import speedy.model.database.mainmemory.datasource.nodes.TupleNode;
import speedy.persistence.PersistenceConstants;
import speedy.persistence.Types;

public class MainMemoryCreateTableForConstants implements ICreateTablesForConstants {

    private static final Logger logger = LoggerFactory.getLogger(MainMemoryCreateTableForConstants.class.getName());

    public void createTable(AllConstantsInFormula constantsInFormula, Scenario scenario) {
        if (scenario.getSource() instanceof EmptyDB) {
            MainMemoryDB newSource = createEmptySourceDatabase();
            scenario.setSource(newSource);
        }
        MainMemoryDB mainMemorySource = (MainMemoryDB) scenario.getSource();
        String tableName = constantsInFormula.getTableName();
        if (mainMemorySource.getTableNames().contains(tableName)) {
            return;
        }
        createSchema(tableName, mainMemorySource, constantsInFormula);
        createInstance(tableName, mainMemorySource, constantsInFormula);
        scenario.getAuthoritativeSources().add(tableName);
    }

    private void createSchema(String tableName, MainMemoryDB mainMemorySource, AllConstantsInFormula constantsInFormula) {
        INode setNodeSchema = new SetNode(tableName);
        mainMemorySource.getDataSource().getSchema().addChild(setNodeSchema);
        TupleNode tupleNodeSchema = new TupleNode(tableName + "Tuple");
        setNodeSchema.addChild(tupleNodeSchema);
        List<String> attributeNames = constantsInFormula.getAttributeNames();
        List<Object> constantValues = constantsInFormula.getConstantValues();
        for (int i = 0; i < constantValues.size(); i++) {
            String attributeName = attributeNames.get(i);
            Object constantValue = constantValues.get(i);
            tupleNodeSchema.addChild(createAttributeSchema(attributeName, constantValue));
        }
    }

    private AttributeNode createAttributeSchema(String attributeName, Object value) {
        AttributeNode attributeNodeInstance = new AttributeNode(attributeName);
        String type = LunaticUtility.findType(value);
        LeafNode leafNodeInstance = new LeafNode(type);
        attributeNodeInstance.addChild(leafNodeInstance);
        return attributeNodeInstance;
    }

    private void createInstance(String tableName, MainMemoryDB mainMemorySource, AllConstantsInFormula constantsInFormula) {
        INode setNodeInstance = new SetNode(tableName, IntegerOIDGenerator.getNextOID());
        mainMemorySource.getDataSource().getInstances().get(0).addChild(setNodeInstance);
        TupleNode tupleNodeInstance = new TupleNode(tableName + "Tuple", IntegerOIDGenerator.getNextOID());
        setNodeInstance.addChild(tupleNodeInstance);
        List<String> attributeNames = constantsInFormula.getAttributeNames();
        List<Object> constantValues = constantsInFormula.getConstantValues();
        for (int i = 0; i < constantValues.size(); i++) {
            String attributeName = attributeNames.get(i);
            Object constantValue = constantValues.get(i);
            tupleNodeInstance.addChild(createAttributeInstance(attributeName, constantValue));
        }
    }

    private AttributeNode createAttributeInstance(String attributeName, Object value) {
        AttributeNode attributeNodeInstance = new AttributeNode(attributeName, IntegerOIDGenerator.getNextOID());
        LeafNode leafNodeInstance = new LeafNode(Types.STRING, value);
        attributeNodeInstance.addChild(leafNodeInstance);
        return attributeNodeInstance;
    }

    private MainMemoryDB createEmptySourceDatabase() {
        INode schemaNode = new TupleNode(PersistenceConstants.DATASOURCE_ROOT_LABEL);
        schemaNode.setRoot(true);
        DataSource dataSource = new DataSource(PersistenceConstants.TYPE_META_INSTANCE, schemaNode);
        INode instanceNode = new TupleNode(PersistenceConstants.DATASOURCE_ROOT_LABEL, IntegerOIDGenerator.getNextOID());
        instanceNode.setRoot(true);
//        dataSource.addInstanceWithCheck(instanceNode);
        dataSource.addInstance(instanceNode);
        return new MainMemoryDB(dataSource);
    }
}
