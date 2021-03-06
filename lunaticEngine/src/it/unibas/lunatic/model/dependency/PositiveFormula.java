package it.unibas.lunatic.model.dependency;

import it.unibas.lunatic.model.dependency.operators.CloneFormulaVisitor;
import it.unibas.lunatic.model.dependency.operators.IFormulaVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PositiveFormula implements IFormula {

    private IFormula father;
    private List<IFormulaAtom> atoms = new ArrayList<IFormulaAtom>();
    private List<FormulaVariable> localVariables = new ArrayList<FormulaVariable>();
    private List<VariableEquivalenceClass> localVariableEquivalenceClasses = new ArrayList<VariableEquivalenceClass>();

    public PositiveFormula() {
    }

    public PositiveFormula(IFormula father) {
        this.father = father;
    }

    public IFormula getFather() {
        return this.father;
    }

    public void setFather(IFormula father) {
        this.father = father;
    }

    public PositiveFormula getPositiveFormula() {
        return this;
    }

    public void setPositiveFormula(PositiveFormula formula) {
        throw new IllegalArgumentException("Formula is already positive.");
    }

    @SuppressWarnings("unchecked")
    public List<IFormula> getNegatedSubFormulas() {
        return Collections.EMPTY_LIST;
    }

    public void addNegatedFormula(IFormula formula) {
        throw new IllegalArgumentException("Formula is positive.");
    }

    public List<IFormulaAtom> getAtoms() {
        return atoms;
    }

    public void setAtoms(List<IFormulaAtom> atoms) {
        this.atoms = atoms;
    }

    public void addAtom(IFormulaAtom a) {
        this.atoms.add(a);
    }

    public List<FormulaVariable> getLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(List<FormulaVariable> localVariables) {
        this.localVariables = localVariables;
    }

    public boolean addLocalVariable(FormulaVariable e) {
        return localVariables.add(e);
    }

    public List<FormulaVariable> getAllVariables() {
        List<FormulaVariable> result = new ArrayList<FormulaVariable>(localVariables);
        if (this.father != null) {
            result.addAll(father.getAllVariables());
        }
        return result;
    }

    public List<VariableEquivalenceClass> getLocalVariableEquivalenceClasses() {
        return localVariableEquivalenceClasses;
    }

    public void setLocalVariableEquivalenceClasses(List<VariableEquivalenceClass> variableEquivalenceClasses) {
        this.localVariableEquivalenceClasses = variableEquivalenceClasses;
    }

    public void accept(IFormulaVisitor visitor) {
        visitor.visitPositiveFormula(this);
    }

    public String getId() {
        StringBuilder result = new StringBuilder();
        for (IFormulaAtom atom : atoms) {
            if (atom instanceof RelationalAtom) {
                result.append(((RelationalAtom) atom).getTableName()).append("-");
            } else {
                result.append(atom.toString()).append("-");
            }
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (IFormulaAtom atom : atoms) {
            result.append(atom.toString()).append(", ");
        }
        result.deleteCharAt(result.length() - 1);
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    public String toSaveString() {
        StringBuilder result = new StringBuilder();
        for (IFormulaAtom atom : atoms) {
            result.append(atom.toSaveString()).append(", ");
        }
        result.deleteCharAt(result.length() - 1);
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    public String toCFString() {
        StringBuilder result = new StringBuilder();
        for (IFormulaAtom atom : atoms) {
            result.append(atom.toCFString()).append(", ");
        }
        result.deleteCharAt(result.length() - 1);
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    public String toLongString() {
        StringBuilder result = new StringBuilder();
        result.append(toString());
        result.append("\nAtoms:");
        for (IFormulaAtom atom : atoms) {
            result.append("\n\t").append(atom.toLongString());
        }
        return result.toString();
    }

    @Override
    public PositiveFormula clone() {
        CloneFormulaVisitor cloneFormula = new CloneFormulaVisitor();
        this.accept(cloneFormula);
        return (PositiveFormula) cloneFormula.getResult();
    }

    public PositiveFormula superficialClone() {
        try {
            return (PositiveFormula) super.clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }

}
