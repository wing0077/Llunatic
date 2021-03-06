package it.unibas.lunatic.model.algebra.operators;

import it.unibas.lunatic.model.dependency.BuiltInAtom;
import it.unibas.lunatic.model.dependency.ComparisonAtom;
import it.unibas.lunatic.model.dependency.Dependency;
import it.unibas.lunatic.model.dependency.FormulaAttribute;
import it.unibas.lunatic.model.dependency.FormulaVariable;
import it.unibas.lunatic.model.dependency.FormulaVariableOccurrence;
import it.unibas.lunatic.model.dependency.IFormulaAtom;
import it.unibas.lunatic.model.dependency.PositiveFormula;
import it.unibas.lunatic.model.dependency.RelationalAtom;
import it.unibas.lunatic.utility.LunaticUtility;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.SpeedyConstants;
import speedy.model.algebra.CartesianProduct;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Join;
import speedy.model.algebra.Scan;
import speedy.model.algebra.Select;
import speedy.model.database.AttributeRef;
import speedy.model.database.TableAlias;
import speedy.model.expressions.Expression;
import speedy.utility.SpeedyUtility;

public class BuildAlgebraTreeForPositiveFormula {

    private static Logger logger = LoggerFactory.getLogger(BuildAlgebraTreeForPositiveFormula.class);

    private FindConnectedTables connectedTablesFinder = new FindConnectedTables();

    public IAlgebraOperator buildTreeForPositiveFormula(Dependency dependency, PositiveFormula positiveFormula, boolean premise, boolean addOidInequality) {
        if (logger.isDebugEnabled()) logger.debug("Building tree for formula: " + positiveFormula);
        List<RelationalAtom> relationalAtoms = extractRelationalAtoms(positiveFormula);
        List<IFormulaAtom> builtInAtoms = extractBuiltInAtoms(positiveFormula);
        List<IFormulaAtom> comparisonAtoms = extractComparisonAtoms(positiveFormula);
        if (logger.isDebugEnabled()) logger.debug("--Relational atoms: " + relationalAtoms);
        if (logger.isDebugEnabled()) logger.debug("--Builtin atoms: " + builtInAtoms);
        if (logger.isDebugEnabled()) logger.debug("--Comparisons: " + comparisonAtoms);
        Map<TableAlias, IAlgebraOperator> treeMap = new HashMap<TableAlias, IAlgebraOperator>();
        initializeMap(relationalAtoms, treeMap, dependency, positiveFormula, premise);
        addLocalSelectionsForBuiltinsAndComparisons(builtInAtoms, treeMap, premise);
        addLocalSelectionsForBuiltinsAndComparisons(comparisonAtoms, treeMap, premise);
        IAlgebraOperator root;
        if (relationalAtoms.size() == 1) {
            root = treeMap.get(relationalAtoms.get(0).getTableAlias());
        } else {
            root = addJoinsAndCartesianProducts(dependency, positiveFormula, relationalAtoms, treeMap, premise, addOidInequality);
            root = addGlobalSelectionsForBuiltins(builtInAtoms, root);
            root = addGlobalSelectionsForComparisons(comparisonAtoms, root, positiveFormula, premise);
        }
        if (logger.isDebugEnabled()) logger.debug("--Result: " + root);
        return root;
    }

    //////////////////////          INIT DATA STRUCTURES
    private List<RelationalAtom> extractRelationalAtoms(PositiveFormula positiveFormula) {
        List<RelationalAtom> result = new ArrayList<RelationalAtom>();
        for (IFormulaAtom atom : positiveFormula.getAtoms()) {
            if (atom instanceof RelationalAtom) {
                result.add((RelationalAtom) atom);
            }
        }
        return result;
    }

    private List<IFormulaAtom> extractBuiltInAtoms(PositiveFormula positiveFormula) {
        List<IFormulaAtom> result = new ArrayList<IFormulaAtom>();
        for (IFormulaAtom atom : positiveFormula.getAtoms()) {
            if (atom instanceof BuiltInAtom) {
                result.add((BuiltInAtom) atom);
            }
        }
        return result;
    }

    private List<IFormulaAtom> extractComparisonAtoms(PositiveFormula positiveFormula) {
        List<IFormulaAtom> result = new ArrayList<IFormulaAtom>();
        for (IFormulaAtom atom : positiveFormula.getAtoms()) {
            if (atom instanceof ComparisonAtom) {
                result.add((ComparisonAtom) atom);
            }
        }
        return result;
    }

    private void initializeMap(List<RelationalAtom> atoms, Map<TableAlias, IAlgebraOperator> treeMap, Dependency dependency, PositiveFormula positiveFormula, boolean premise) {
        for (RelationalAtom atom : atoms) {
            if (logger.isDebugEnabled()) logger.debug("Initialize operator for table alias in atom " + atom);
            RelationalAtom relationalAtom = (RelationalAtom) atom;
            TableAlias tableAlias = relationalAtom.getTableAlias();
            IAlgebraOperator tableRoot = new Scan(tableAlias);
            tableRoot = addLocalSelectionsForVariableInSameRelationalAtom(tableRoot, relationalAtom, dependency, positiveFormula, premise);
            tableRoot = addLocalSelections(tableRoot, relationalAtom);
            treeMap.put(tableAlias, tableRoot);
        }
    }

    //////////////////////          LOCAL SELECTIONS
    private IAlgebraOperator addLocalSelectionsForVariableInSameRelationalAtom(IAlgebraOperator scan, RelationalAtom relationalAtom, Dependency dependency, PositiveFormula positiveFormula, boolean premise) {
        Map<FormulaVariable, List<FormulaVariableOccurrence>> variablesWithMultipleOccurrences = findVariablesWithMultipleOccurrencesInAtom(relationalAtom, dependency, positiveFormula, premise);
        if (variablesWithMultipleOccurrences.isEmpty()) {
            return scan;
        }
        if (logger.isDebugEnabled()) logger.debug("Variables with multiple occurrences in atom " + relationalAtom + ":\n" + SpeedyUtility.printMap(variablesWithMultipleOccurrences));
        List<Expression> selections = new ArrayList<Expression>();
        for (List<FormulaVariableOccurrence> formulaVariables : variablesWithMultipleOccurrences.values()) {
            for (int i = 0; i < formulaVariables.size() - 1; i++) {
                FormulaVariableOccurrence v1 = formulaVariables.get(i);
                AttributeRef a1 = v1.getAttributeRef();
                FormulaVariableOccurrence v2 = formulaVariables.get(i + 1);
                AttributeRef a2 = v2.getAttributeRef();
                Expression selection = new Expression(a1.getName() + "==" + a2.getName());
                selection.getJepExpression().getVar(a1.getName()).setDescription(a1);
                selection.getJepExpression().getVar(a2.getName()).setDescription(a2);
                selections.add(selection);
            }
        }
        if (!selections.isEmpty()) {
            Select select = new Select(selections);
            select.addChild(scan);
            scan = select;
        }
        return scan;
    }

    private Map<FormulaVariable, List<FormulaVariableOccurrence>> findVariablesWithMultipleOccurrencesInAtom(RelationalAtom relationalAtom, Dependency dependency, PositiveFormula positiveFormula, boolean premise) {
        if (logger.isDebugEnabled()) logger.debug("Finding variables with multiple occurrences in atom " + relationalAtom);
        Map<FormulaVariable, List<FormulaVariableOccurrence>> result = new HashMap<FormulaVariable, List<FormulaVariableOccurrence>>();
        List<FormulaVariable> allVariablesInDependency = new ArrayList<FormulaVariable>();
        allVariablesInDependency.addAll(dependency.getPremise().getLocalVariables());
        allVariablesInDependency.addAll(dependency.getConclusion().getLocalVariables());
        for (FormulaVariable variable : allVariablesInDependency) {
            List<FormulaVariableOccurrence> occurrencesInAtom = new ArrayList<FormulaVariableOccurrence>();
            List<FormulaVariableOccurrence> relationalOccurrences;
            if (premise) {
                relationalOccurrences = variable.getPremiseRelationalOccurrences();
            } else {
                relationalOccurrences = variable.getConclusionRelationalOccurrences();
            }
            if (logger.isDebugEnabled()) logger.debug("Variable: " + variable + ": " + relationalOccurrences);
            for (FormulaVariableOccurrence relationalOccurrence : relationalOccurrences) {
                if (relationalOccurrence.getAttributeRef().getTableAlias().equals(relationalAtom.getTableAlias())) {
                    occurrencesInAtom.add(relationalOccurrence);
                }
            }
            if (occurrencesInAtom.size() > 1) {
                result.put(variable, occurrencesInAtom);
            }
        }
        return result;
    }

    private IAlgebraOperator addLocalSelections(IAlgebraOperator scan, RelationalAtom relationalAtom) {
        IAlgebraOperator root = scan;
        List<Expression> selections = new ArrayList<Expression>();
        for (FormulaAttribute attribute : relationalAtom.getAttributes()) {
            if (attribute.getValue().isVariable()) {
                continue;
            }
            AttributeRef attributeRef = new AttributeRef(relationalAtom.getTableAlias(), attribute.getAttributeName());
            Expression selection;
            if (attribute.getValue().isNull()) {
                selection = new Expression("isNull(" + attribute.getAttributeName() + ")");
            } else {
                selection = new Expression(attribute.getAttributeName() + "==" + attribute.getValue());
            }
            selection.getJepExpression().getVar(attribute.getAttributeName()).setDescription(attributeRef);
//            selection.setVariableDescription(attribute.getAttributeName(), attributeRef);
            selections.add(selection);
        }
        if (!selections.isEmpty()) {
            Select select = new Select(selections);
            select.addChild(scan);
            root = select;
        }
        return root;
    }

    private void addLocalSelectionsForBuiltinsAndComparisons(List<IFormulaAtom> atoms, Map<TableAlias, IAlgebraOperator> treeMap, boolean premise) {
        if (logger.isDebugEnabled()) logger.debug("Adding selections for atoms: " + atoms);
        for (Iterator<IFormulaAtom> it = atoms.iterator(); it.hasNext();) {
            IFormulaAtom atom = it.next();
            List<TableAlias> aliasesForAtom = AlgebraUtility.findAliasesForAtom(atom);
            boolean atomToRemove = false;
            for (TableAlias tableAlias : aliasesForAtom) {
                if (hasLocalOccurrences(tableAlias, atom, premise)) {
                    atomToRemove = true;
                    IAlgebraOperator rootForAlias = treeMap.get(tableAlias);
                    if (rootForAlias == null) {
                        throw new IllegalArgumentException("Unable to find operator for table alias " + tableAlias);
                    }
                    if (rootForAlias instanceof Select) {
                        Select select = (Select) rootForAlias;
                        select.getSelections().add(atom.getExpression());
                    } else {
                        Select select = new Select(atom.getExpression());
                        select.addChild(rootForAlias);
                        treeMap.put(tableAlias, select);
                    }
                }
            }
            if (atomToRemove) {
                it.remove();
            }
        }
    }

    private boolean hasLocalOccurrences(TableAlias tableAlias, IFormulaAtom atom, boolean premise) {
        for (FormulaVariable variable : atom.getVariables()) {
            if (!hasOccurenceInTable(variable, tableAlias, premise)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasOccurenceInTable(FormulaVariable variable, TableAlias tableAlias, boolean premise) {
        for (FormulaVariableOccurrence occurrence : getFormulaVariableOccurrence(variable, premise)) {
            if (occurrence.getAttributeRef().getTableAlias().equals(tableAlias)) {
                return true;
            }
        }
        return false;
    }

    //////////////////////          JOINS
    private IAlgebraOperator addJoinsAndCartesianProducts(Dependency dependency, PositiveFormula positiveFormula, List<RelationalAtom> atoms, Map<TableAlias, IAlgebraOperator> treeMap, boolean premise, boolean addOidInequality) {
        List<FormulaVariable> equalityGeneratingVariables = findEqualityGeneratingVariables(positiveFormula, premise);
        if (logger.isDebugEnabled()) logger.debug("Equality generating variables: " + equalityGeneratingVariables);
        List<Equality> equalities = extractEqualities(equalityGeneratingVariables, positiveFormula, premise);
        if (logger.isDebugEnabled()) logger.debug("Join equalities: " + equalities);
        List<EqualityGroup> equalityGroups = groupEqualities(equalities, dependency);
        List<ConnectedTables> connectedTables = connectedTablesFinder.findConnectedEqualityGroups(atoms, equalityGroups);
        if (logger.isDebugEnabled()) logger.debug("Connected tables: " + connectedTables);
        assignEqualityGroupsToConnectedTables(connectedTables, equalityGroups);
        List<IAlgebraOperator> rootsForConnectedComponents = new ArrayList<IAlgebraOperator>();
        for (ConnectedTables connectedComponent : connectedTables) {
            rootsForConnectedComponents.add(generateRootForConnectedComponent(connectedComponent, dependency, treeMap, addOidInequality));
        }
        if (rootsForConnectedComponents.size() == 1) {
            return rootsForConnectedComponents.get(0);
        }
        CartesianProduct cartesianProduct = new CartesianProduct();
        for (IAlgebraOperator rootForConnectedComponent : rootsForConnectedComponents) {
            cartesianProduct.addChild(rootForConnectedComponent);
        }
        return cartesianProduct;
    }

    private List<FormulaVariable> findEqualityGeneratingVariables(PositiveFormula positiveFormula, boolean premise) {
        // finds variables that have multiple occurrences in relationala atoms; comparisons are handled as selections
        List<FormulaVariable> result = new ArrayList<FormulaVariable>();
        for (FormulaVariable formulaVariable : positiveFormula.getAllVariables()) {
            List<AttributeRef> occurrencesInFormula = findOccurrencesInFormula(formulaVariable, positiveFormula, premise);
            if (logger.isDebugEnabled()) logger.debug("Occurrences for variable " + formulaVariable + ": " + occurrencesInFormula);
            if (occurrencesInFormula.size() > 1) {
                result.add(formulaVariable);
            }
        }
        return result;
    }

    private List<AttributeRef> findOccurrencesInFormula(FormulaVariable formulaVariable, PositiveFormula positiveFormula, boolean premise) {
        List<TableAlias> aliasesInFormula = AlgebraUtility.findAliasesForFormula(positiveFormula);
        if (logger.isDebugEnabled()) logger.debug("Finding occurrences for variable: " + formulaVariable + " in aliases " + aliasesInFormula);
        List<AttributeRef> variableAliasesInFormula = new ArrayList<AttributeRef>();
        for (FormulaVariableOccurrence occurrence : getFormulaVariableOccurrence(formulaVariable, premise)) {
            if (logger.isDebugEnabled()) logger.debug("\tOccurrence: " + occurrence.toLongString());
            if (aliasesInFormula.contains(occurrence.getAttributeRef().getTableAlias())) {
                variableAliasesInFormula.add(occurrence.getAttributeRef());
            }
        }
        if (logger.isDebugEnabled()) logger.debug("Filtering result occurrences for variable: " + variableAliasesInFormula);
        return variableAliasesInFormula;
    }

    private List<Equality> extractEqualities(List<FormulaVariable> joinVariables, PositiveFormula positiveFormula, boolean premise) {
        List<Equality> result = new ArrayList<Equality>();
        for (FormulaVariable joinVariable : joinVariables) {
            List<AttributeRef> occurrencesInFormula = findOccurrencesInFormula(joinVariable, positiveFormula, premise);
            for (int i = 0; i < occurrencesInFormula.size() - 1; i++) {
                Equality equality = new Equality(occurrencesInFormula.get(i), occurrencesInFormula.get(i + 1));
                if (!equality.isTrivial()) {
                    result.add(equality);
                }
            }
        }
        return result;
    }

    private List<EqualityGroup> groupEqualities(List<Equality> equalities, Dependency dependency) {
        Map<String, EqualityGroup> groups = new HashMap<String, EqualityGroup>();
        for (Equality equality : equalities) {
            EqualityGroup group = groups.get(getHashString(equality.getLeftAttribute().getTableAlias(), equality.getRightAttribute().getTableAlias()));
            if (group == null) {
                group = new EqualityGroup(equality);
                if (dependency.joinGraphIsCyclic()) {
                    group.setCyclicJoinGraph(true);
                }
                groups.put(getHashString(equality.getLeftAttribute().getTableAlias(), equality.getRightAttribute().getTableAlias()), group);
            }
            group.getEqualities().add(equality);
        }
        return new ArrayList<EqualityGroup>(groups.values());
    }

    private String getHashString(TableAlias alias1, TableAlias alias2) {
        List<String> aliases = new ArrayList<String>();
        aliases.add(alias1.toString());
        aliases.add(alias2.toString());
        Collections.sort(aliases);
        return aliases.toString();
    }

    private void assignEqualityGroupsToConnectedTables(List<ConnectedTables> connectedTables, List<EqualityGroup> equalityGroups) {
        for (ConnectedTables connectedComponent : connectedTables) {
            List<EqualityGroup> equalityGroupsForConnectedComponent = new ArrayList<EqualityGroup>();
            for (EqualityGroup equalityGroup : equalityGroups) {
                if (connectedComponent.getTableAliases().contains(equalityGroup.getLeftTable()) && connectedComponent.getTableAliases().contains(equalityGroup.getRightTable())) {
                    equalityGroupsForConnectedComponent.add(equalityGroup);
                }
            }
            connectedComponent.setEqualityGroups(equalityGroupsForConnectedComponent);
        }
    }

    private IAlgebraOperator generateRootForConnectedComponent(ConnectedTables connectedTables, Dependency dependency, Map<TableAlias, IAlgebraOperator> treeMap, boolean addOidInequality) {
        if (connectedTables.getTableAliases().size() == 1) {
            TableAlias singletonTable = connectedTables.getTableAliases().iterator().next();
            return treeMap.get(singletonTable);
        }
        IAlgebraOperator root = null;
        List<TableAlias> addedTables = new ArrayList<TableAlias>();
        sortEqualityGroups(connectedTables.getEqualityGroups());
        List<EqualityGroup> equalityGroupClone = new ArrayList<EqualityGroup>(connectedTables.getEqualityGroups());
        for (Iterator<EqualityGroup> it = equalityGroupClone.iterator(); it.hasNext();) {
            EqualityGroup equalityGroup = it.next();
            if (isSelection(equalityGroup, addedTables)) {
                continue;
            }
            root = addJoin(dependency, equalityGroup, addedTables, root, treeMap, addOidInequality);
            if (logger.isDebugEnabled()) logger.debug("Adding join for equality group:\n" + equalityGroup + "\nResult:\n" + root);
            it.remove();
        }
        if (!equalityGroupClone.isEmpty()) {
            List<Expression> selections = new ArrayList<Expression>();
            for (EqualityGroup equalityGroup : equalityGroupClone) {
                List<Expression> selectionsForEquality = equalityGroup.getEqualityExpressions();
                selections.addAll(selectionsForEquality);
            }
            Select select = new Select(selections);
            select.addChild(root);
            root = select;
        }
        return root;
    }

    private void sortEqualityGroups(List<EqualityGroup> equalityGroups) {
        if (equalityGroups.isEmpty()) {
            return;
        }
        if (logger.isDebugEnabled()) logger.debug("Sorting equality groups\n" + LunaticUtility.printCollection(equalityGroups));
        List<EqualityGroup> addedGroup = new ArrayList<EqualityGroup>();
        addedGroup.add(equalityGroups.remove(0));
        while (!equalityGroups.isEmpty()) {
            EqualityGroup nextEqualityGroup = findNextGroupInJoin(equalityGroups, addedGroup);
            addedGroup.add(nextEqualityGroup);
            equalityGroups.remove(nextEqualityGroup);
        }
        equalityGroups.addAll(addedGroup);
        if (logger.isDebugEnabled()) logger.debug("Result\n" + LunaticUtility.printCollection(equalityGroups));
    }

    private EqualityGroup findNextGroupInJoin(List<EqualityGroup> equalityGroups, List<EqualityGroup> sortedList) {
        for (EqualityGroup equalityGroup : equalityGroups) {
            if (containsTableAlias(equalityGroup.getLeftTable(), sortedList)
                    || containsTableAlias(equalityGroup.getRightTable(), sortedList)) {
                return equalityGroup;
            }
        }
        throw new IllegalArgumentException("Unable to find a path between equality groups\n" + LunaticUtility.printCollection(equalityGroups) + "\n" + LunaticUtility.printCollection(sortedList));
    }

    private boolean containsTableAlias(TableAlias table, List<EqualityGroup> sortedList) {
        for (EqualityGroup equalityGroup : sortedList) {
            if (equalityGroup.getLeftTable().equals(table)
                    || equalityGroup.getRightTable().equals(table)) {
                return true;
            }
        }
        return false;
    }

    private boolean singleTable(EqualityGroup equalityGroup) {
        return equalityGroup.getLeftTable().equals(equalityGroup.getRightTable());
    }

    private boolean isSelection(EqualityGroup equalityGroup, List<TableAlias> addedTables) {
        return singleTable(equalityGroup)
                || (addedTables.contains(equalityGroup.getLeftTable())
                && addedTables.contains(equalityGroup.getRightTable()));
    }

    private IAlgebraOperator addJoin(Dependency dependency, EqualityGroup equalityGroup, List<TableAlias> addedTables, IAlgebraOperator joinRoot, Map<TableAlias, IAlgebraOperator> treeMap, boolean addOidInequality) {
        if (logger.isDebugEnabled()) logger.debug("-------Adding join for equality: " + equalityGroup);
        // standard case: add table for right attribute
        IAlgebraOperator leftChild = joinRoot;
        if (addedTables.isEmpty()) {
            // initial joins: joinRoot == null
            leftChild = treeMap.get(equalityGroup.getLeftTable());
            AlgebraUtility.addIfNotContained(addedTables, equalityGroup.getLeftTable());
        }
        IAlgebraOperator rightChild = treeMap.get(equalityGroup.getRightTable());
        List<AttributeRef> leftAttributes = equalityGroup.getAttributeRefsForTableAlias(equalityGroup.getLeftTable());
        List<AttributeRef> rightAttributes = equalityGroup.getAttributeRefsForTableAlias(equalityGroup.getRightTable());
        if (addedTables.contains(equalityGroup.getRightTable())) {
            // alternative case: add table for right attribute    
            rightChild = treeMap.get(equalityGroup.getLeftTable());
            leftAttributes = equalityGroup.getAttributeRefsForTableAlias(equalityGroup.getRightTable());
            rightAttributes = equalityGroup.getAttributeRefsForTableAlias(equalityGroup.getLeftTable());
            AlgebraUtility.addIfNotContained(addedTables, equalityGroup.getLeftTable());
        } else {
            AlgebraUtility.addIfNotContained(addedTables, equalityGroup.getRightTable());
        }
        Join join = new Join(leftAttributes, rightAttributes);
        join.addChild(leftChild);
        join.addChild(rightChild);
//        AlgebraUtility.addIfNotContained(addedTables, equalityGroup.leftTable);
//        AlgebraUtility.addIfNotContained(addedTables, equalityGroup.rightTable);
        IAlgebraOperator root = join;
        if (addOidInequality && equalityGroup.getLeftTable().getTableName().equals(equalityGroup.getRightTable().getTableName()) && !dependency.joinGraphIsCyclic()) {
            root = addOidInequality(equalityGroup.getLeftTable(), equalityGroup.getRightTable(), root);
        }
        return root;
    }

    private Select addOidInequality(TableAlias leftTable, TableAlias rightTable, IAlgebraOperator root) {
        String inequalityOperator = "!=";
        Expression oidInequality = new Expression(leftTable.toString() + "." + SpeedyConstants.OID + inequalityOperator + rightTable.toString() + "." + SpeedyConstants.OID);
        oidInequality.changeVariableDescription(leftTable.toString() + "." + SpeedyConstants.OID, new AttributeRef(leftTable, SpeedyConstants.OID));
        oidInequality.changeVariableDescription(rightTable.toString() + "." + SpeedyConstants.OID, new AttributeRef(rightTable, SpeedyConstants.OID));
        Select select = new Select(oidInequality);
        select.addChild(root);
        return select;
    }

    //////////////////////          GLOBAL SELECTIONS
    private IAlgebraOperator addGlobalSelectionsForBuiltins(List<IFormulaAtom> atoms, IAlgebraOperator root) {
        for (IFormulaAtom atom : atoms) {
            BuiltInAtom builtInAtom = (BuiltInAtom) atom;
            Select select = new Select(builtInAtom.getExpression());
            select.addChild(root);
            root = select;
        }
        return root;
    }

    private IAlgebraOperator addGlobalSelectionsForComparisons(List<IFormulaAtom> atoms, IAlgebraOperator root, PositiveFormula positiveFormula, boolean premise) {
        if (logger.isDebugEnabled()) logger.debug("Adding global selections for comparisons " + atoms);
        for (IFormulaAtom atom : atoms) {
            ComparisonAtom comparisonAtom = (ComparisonAtom) atom;
            if (isDifference(comparisonAtom, positiveFormula, premise)) {
                continue;
            }
            Select select = new Select(comparisonAtom.getExpression());
            select.addChild(root);
            root = select;
        }
        return root;
    }

    private boolean isDifference(ComparisonAtom comparisonAtom, PositiveFormula positiveFormula, boolean premise) {
        for (FormulaVariable variable : comparisonAtom.getVariables()) {
            if (findOccurrencesInFormula(variable, positiveFormula, premise).size()
                    != getFormulaVariableOccurrence(variable, premise).size()) {
                return true;
            }
        }
        return false;
    }

    private List<FormulaVariableOccurrence> getFormulaVariableOccurrence(FormulaVariable variable, boolean premise) {
        if (premise) {
            return variable.getPremiseRelationalOccurrences();
        } else {
            return variable.getConclusionRelationalOccurrences();
        }
    }

}
