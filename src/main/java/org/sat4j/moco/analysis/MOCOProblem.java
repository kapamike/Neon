/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004, 2012 Artois University and CNRS
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU Lesser General Public License Version 2.1 or later (the
 * "LGPL"), in which case the provisions of the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL, and not to allow others to use your version of
 * this file under the terms of the EPL, indicate your decision by deleting
 * the provisions above and replace them with the notice and other provisions
 * required by the LGPL. If you do not delete the provisions above, a recipient
 * may use your version of this file under the terms of the EPL or the LGPL.
 *
 * Contributors:
 *   CRIL - initial API and implementation
 *   Miguel Terra-Neves, Ines Lynce and Vasco Manquinho - MOCO solver
 *******************************************************************************/
package org.sat4j.moco.analysis;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;
import org.sat4j.core.ReadOnlyVecInt;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.moco.algorithm.SmartMutation;
import org.sat4j.moco.pb.*;
import org.sat4j.moco.problem.Instance;
import org.sat4j.moco.problem.Objective;
import org.sat4j.moco.util.Log;
import org.sat4j.moco.util.Real;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import java.util.*;

/**
 * Implementation of the MOCO problem as an {@link AbstractProblem} in the MOEA framework.
 * To be used for performance analysis.
 * @author Miguel Terra-Neves
 */
public class MOCOProblem extends AbstractProblem {

    /**
     * Stores the actual MOCO instance.
     */
    private Instance instance;

    /**
     * Stores the Result instance.
     */
    private Result result;

    /**
     * Stores if structure improvements are to be used
     */
    private boolean structure_improvements = false;


    private int NumberOfVariables;


    private IVecInt removed_constraint_indexes = new VecInt();


    private HashSet<Integer> removed_constraint_indexes_hash = new HashSet<Integer>();


    private HashSet<Integer> removed_variables = new HashSet<Integer>();


    private HashSet<Integer> removed_variables_4_smart = new HashSet<Integer>();

    /**
     * Stores the variables forced to be true by unit propagation
     */
    private HashSet<Integer> true_variables = new HashSet<Integer>();

    /**
     * Stores the variables forced to be false by unit propagation
     */
    private HashSet<Integer> false_variables = new HashSet<Integer>();


    private int[] index_mapping;


    private int[] index_reverse_mapping;

    /**
     * Counters for the final print
     */
    private int successful_mutation = 0;
    private int successful_improvement = 0;
    private int total_mutation = 0;
    private int total_improvement = 0;

    /**
     * Stores the unit propagator
     */
    UnitPropagator unit_solver;

    /**
     * Stores the variables exploited by structure improvements that were not forced by unit propagation
     */
    IVec<IVecInt> free_vars_SI = new Vec();
    IVec<IVecInt> free_vars_SI_4_smart_list = new Vec();

    
    /**
     * Creates an instance of a MOEA framework MOCO problem with a given MOCO instance.
     * @param instance The instance.
     */
    public MOCOProblem(Instance instance) {
        super(instance.nVars(), instance.nObjs(), instance.nConstrs());
        this.instance = instance;
        setNumberOfVariables(instance.nVars());
    }

    /**
     * Creates an instance of a MOEA framework MOCO problem with a given MOCO instance.
     * @param instance The instance.
     */
    public MOCOProblem(Instance instance, boolean si, boolean up, Result result) {
        super(instance.nVars(), instance.nObjs(), instance.nConstrs());
        this.instance = instance;
        this.structure_improvements = si;
        this.result = result;
        setNumberOfVariables(instance.nVars());
        if (up) {
            unitPropagation();
        }
        if(si) {
            int index;
            IVecInt removable_constraint_indexes = findRemovableConstraints(false);
            index = getNewConstraint(removable_constraint_indexes);
            while (index >= 0){
                removed_constraint_indexes.push(index);
                removed_constraint_indexes_hash.add(index);
                removable_constraint_indexes.remove(index);
                index = getNewConstraint(removable_constraint_indexes);
            }

            Log.comment(0, removed_constraint_indexes.size() + " constraints were removed due to the structure improvements");
            Log.comment(0, removed_variables.size() + " variables were removed due to the structure improvements");
            setNumberOfVariables(getNumberOfVariables() - removed_variables.size() + removed_constraint_indexes.size());
            index_mapping = createMapping();
            index_reverse_mapping = createReverseMapping();
        }
    }

    public boolean getSI(){
        return this.structure_improvements;
    }

    @Override
    public int getNumberOfVariables(){
        return this.NumberOfVariables;
    }

    public void setNumberOfVariables(int nVars){
        this.NumberOfVariables = nVars;
    }

    public int getInitialNumberOfVariables(){
        return this.instance.nVars();
    }

    /**
     * Applies unit propagation to the formula.
     */
    public void unitPropagation(){
        unit_solver = UnitPropagatorFactory.instance().defaultSolver();
        addConstrToUnitarySolver();
        unit_solver.getOrder().init();
        unit_solver.propagate();
        IVecInt trail = unit_solver.getTrail();
        int head_trail;
        for (int i = 0; i < trail.size(); i++){
            head_trail = trail.get(i);
            if (head_trail % 2 == 0){
                true_variables.add(head_trail / 2);
            }
            else{
                false_variables.add(head_trail / 2);
            }
        }
        Log.comment(0, (true_variables.size() + false_variables.size()) + " variables were removed due to the unitary propagation");

    }

    /**
     * Add the problem's constraints to the unit propagation solver
     */
    public void addConstrToUnitarySolver(){
        for (int i = 0; i < getNumberOfConstraints(); i ++) {
            IVecInt lits = new VecInt();
            IVecInt coeffs = new VecInt();
            
            for (int j = 0; j < this.instance.getConstr(i).getLits().size(); j++){
                lits = lits.push(this.instance.getConstr(i).getLits().get(j));
                coeffs = coeffs.push(this.instance.getConstr(i).getCoeffs().get(j).asInt());
            }
            try {
                if (this.instance.getConstr(i) instanceof EQ) {
                    unit_solver.addExactly(lits, coeffs, this.instance.getConstr(i).getRHS().asInt());
                } else if (this.instance.getConstr(i) instanceof LE) {
                    unit_solver.addAtMost(lits, coeffs, this.instance.getConstr(i).getRHS().asInt());
                } else if (this.instance.getConstr(i) instanceof GE) {
                    unit_solver.addAtLeast(lits, coeffs, this.instance.getConstr(i).getRHS().asInt());
                }
            }
            catch (ContradictionException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Find constraints that can be exploited by the structure improvements
     * @return Indexes of the constraints that can be removed
     */
    public IVecInt findRemovableConstraints(boolean forced_switch){
        IVecInt removable_indexes = new VecInt();
        int count_non_forced_vars;
        int int_to_test;
        outerloop:
        for (int i = 0; i < this.instance.nConstrs(); ++i) {
            count_non_forced_vars = 0;
            PBConstr constr = this.instance.getConstr(i);
            int_to_test = (constr instanceof GE ? -1 : 1);
            if (constr.getRHS().asInt() == int_to_test) {
                for (int j = 0; j < constr.getCoeffs().size(); j++){
                    if (constr.getCoeffs().get(j).asInt() != int_to_test) {
                        continue outerloop;
                    }
                    if (forced_switch || !isForcedVar(constr.getLits().get(j))){
                        count_non_forced_vars ++;
                    }
                }
                if (count_non_forced_vars > 1) {
                    removable_indexes.push(i);
                }
            }
        }
        return removable_indexes;
    }

    /**
     * Selects a constraint to be exploited by the structure improvements
     * @return Index of the constraints to be removed
     */
    public int getNewConstraint(IVecInt removable){
        int biggest_size = -1;
        int size;
        int index = -1;
        VecInt free_vars;
        VecInt biggest_free_vars = new VecInt();
        ReadOnlyVecInt lits;
        Set<Integer> to_remove = new HashSet<Integer>();
        outterloop:
        for (int i = 0; i < removable.size(); i++){
            free_vars = new VecInt();
            lits = this.instance.getConstr(removable.get(i)).getLits();
            size = lits.size();
            if (size > biggest_size){
                for (int j = 0; j < size; j ++){
                    if (this.removed_variables.contains(lits.get(j))){
                        to_remove.add(removable.get(i));
                        continue outterloop;
                    }
                    if (isForcedVar(lits.get(j))){
                        continue;
                    }
                    free_vars.push(lits.get(j));
                }
                biggest_free_vars = free_vars;

                biggest_size = size;
                index = removable.get(i);
            }
        }
        for (Integer i : to_remove){
            removable.remove(i);
        }
        if (index >= 0) {
            for (int j = 0; j < biggest_free_vars.size(); j++) {
                this.removed_variables.add(biggest_free_vars.get(j));
            }
            this.free_vars_SI.push(biggest_free_vars);
        }
        return index;
    }

    /**
     * Find special constraints to be used in the assumptions of smart improvement
     * @return Object containing a set and a list of variables from the special constraints
     */
    public SmartMutation.SpecialConstraints findSpecialConstraints4SmartImprovement(){
        int index;
        IVecInt removable_constraint_indexes = findRemovableConstraints(true);
        index = getNewConstraint4SmartImprovement(removable_constraint_indexes);
        while (index >= 0){
            removable_constraint_indexes.remove(index);
            index = getNewConstraint4SmartImprovement(removable_constraint_indexes);
        }
        return new SmartMutation.SpecialConstraints(free_vars_SI_4_smart_list, removed_variables_4_smart);
    }

    /**
     * Selects a constraint to be used in the assumptions of smart improvement
     * @return Index of the constraints used
     */
    public int getNewConstraint4SmartImprovement(IVecInt removable){
        int biggest_size = -1;
        int size;
        int index = -1;
        VecInt free_vars;
        VecInt biggest_free_vars = new VecInt();
        ReadOnlyVecInt lits;
        Set<Integer> to_remove = new HashSet<Integer>();
        outterloop:
        for (int i = 0; i < removable.size(); i++){
            free_vars = new VecInt();
            lits = this.instance.getConstr(removable.get(i)).getLits();
            size = lits.size();
            if (size > biggest_size){
                for (int j = 0; j < size; j ++){
                    if (this.removed_variables_4_smart.contains(lits.get(j))){
                        to_remove.add(removable.get(i));
                        continue outterloop;
                    }
                    free_vars.push(Math.abs(lits.get(j)));
                }
                biggest_free_vars = free_vars;
                biggest_size = size;
                index = removable.get(i);
            }
        }
        for (Integer i : to_remove){
            removable.remove(i);
        }
        if (index >= 0) {
            for (int j = 0; j < biggest_free_vars.size(); j++) {
                this.removed_variables_4_smart.add(biggest_free_vars.get(j));
            }
            this.free_vars_SI_4_smart_list.push(biggest_free_vars);
        }
        return index;
    }
    
    /**
     * Retrieves an assignment from a given MOEA framework {@link Solution} object.
     * @param sol The solution.
     * @return The assignment.
     */
    public boolean[] getAssignment(Solution sol) {
        boolean[] a = new boolean[getInitialNumberOfVariables()];
        int i = 0;
        if (!getSI()){
            for (int j = 0; j < getNumberOfVariables(); j++) {
                if (true_variables.contains(j+1)){
                    a[j] = true;
                }
                else if (false_variables.contains(j+1)){
                    a[j] = false;
                }
                else {
                    a[j] = (EncodingUtils.getInt(sol.getVariable(i)) == 1);
                    i ++;
                }
            }
        }
        else{
            int number_normal_variables = getNumberOfVariables() - removed_constraint_indexes.size() - true_variables.size() - false_variables.size();
            for (int j = 0; j < number_normal_variables ; j++) {
                a[getMappingIndex(j)] = (EncodingUtils.getInt(sol.getVariable(j)) == 1);
            }
            for (int j : true_variables) {
                a[j-1] = true;
            }
            for (int j : false_variables) {
                a[j-1] = false;
            }
            int value;
            int true_var;
            ReadOnlyVecInt lits;
            for (int j = 0 ; j < removed_constraint_indexes.size(); j++){
                value = EncodingUtils.getInt(sol.getVariable(j + number_normal_variables));
                true_var = decodeIntValue(j, value);
                lits = this.instance.getConstr(removed_constraint_indexes.get(j)).getLits();
                for (i = 0; i < lits.size(); i++){
                    if (isForcedVar(lits.get(i))){
                        continue;
                    }
                    a[lits.get(i) - 1] = (true_var == lits.get(i));
                }
            }
        }
        return a;
    }

    public int decodeIntValue(int index, int value){
        return value == 0 ? 0 : free_vars_SI.get(index).get(value - 1);
    }

    /**
     * Returns the set of variables that are not being exploited by the unit propagation or by structure improvements
     * @return List with variables that are not being exploited
     */
    public IVec<IVecInt> getFreeVars() {
        return this.free_vars_SI;
    }

    /**
     * Creates a mapping that helps in transforming an individual into a purely Boolean assignment
     */
    public int[] createMapping(){
        int n_normal_vars = getNumberOfVariables() - removed_constraint_indexes.size() - true_variables.size() - false_variables.size();
        int[] mapping = new int[n_normal_vars];
        int index = 0;
        for (int i = 0; i < n_normal_vars; i++){
            while (removed_variables.contains(index + 1) || isForcedVar(index + 1)){
                index ++;
            }
            mapping[i] = index;
            index ++;
        }
        return mapping;
    }

    /**
     * Creates a mapping that helps in transforming a purely Boolean assignment into an individual
     */
    public int[] createReverseMapping(){
        int[] mapping = new int[getInitialNumberOfVariables()];
        Arrays.fill(mapping, -1);
        int index = 0;
        for (int i = 0; i < getNumberOfVariables() - removed_constraint_indexes.size() - true_variables.size() - false_variables.size(); i++){
            while (removed_variables.contains(index + 1) || isForcedVar(index + 1)){
                index ++;
            }
            mapping[index] = i;
            index ++;
        }
        return mapping;
    }

    public int getMappingIndex(int j){
        return this.index_mapping[j];
    }

    public int getInverseMappingIndex(int j){
        return this.index_reverse_mapping[j];
    }

    public IVecInt getRemovedConstraints(){
        return this.removed_constraint_indexes;
    }

    public void setVariableValue(Solution solution, int index, int val) {
        EncodingUtils.setInt(solution.getVariable(index), val);
    }

    public boolean isForcedVar (int var){
        return (true_variables.contains(var) || false_variables.contains(var));
    }

    /**
     * Computes the constraint violation and objective function cost values of a given MOEA framework
     * {@link Solution} object.
     * @param sol The solution.
     */
    public void evaluate(Solution sol) {
        boolean[] a = getAssignment(sol);
        for (int i = 0; i < this.instance.nObjs(); ++i) {
            Objective obj = this.instance.getObj(i);
            sol.setObjective(i, obj.evaluate(a).asDouble());
        }
        double viol = 0.0;
        for (int i = 0; i < this.instance.nConstrs(); ++i) {
            if (removed_constraint_indexes_hash.contains(i)){
                continue;
            }
            PBConstr constr = this.instance.getConstr(i);
            Real lhs = constr.getLHS().evaluate(a);
            viol += constr.violatedBy(lhs) ? constr.getRHS().subtract(lhs).abs().asDouble() : 0.0;
        }
        sol.setConstraint(0, viol);
    }

    /**
     * Creates an empty MOEA framework {@link Solution} object.
     * @return The solution object.
     */
    public Solution newSolution() {
        int non_forced_vars = getNumberOfVariables() - true_variables.size() - false_variables.size();
        Solution sol = new Solution(non_forced_vars, getNumberOfObjectives(), getNumberOfConstraints());
        for (int i = 0; i < non_forced_vars - removed_constraint_indexes.size(); ++i) {
            sol.setVariable(i, EncodingUtils.newInt(0, 1));
        }
        for (int i = 0; i < removed_constraint_indexes.size(); ++i) {
            sol.setVariable(i + non_forced_vars - removed_constraint_indexes.size(),
                    EncodingUtils.newInt((this.instance.getConstr(removed_constraint_indexes.get(i)) instanceof LE ||
                                          this.instance.getConstr(removed_constraint_indexes.get(i)) instanceof GE)? 0 : 1,
                                         free_vars_SI.get(i).size()));
        }
        return sol;
    }
    
    /**
     * Creates an empty MOEA framework {@link Solution} object purely for storing the objective function
     * cost values of some assignment in a memory efficient manner.
     * @return The solution object.
     */
    Solution newCostVec() { return new Solution(0, getNumberOfObjectives(), 0); }


    public PBConstr getConstr(int i){
        return this.instance.getConstr(i);
    }

    public Objective getObj(int i){
        return this.instance.getObj(i);
    }


    public int getSuccessfulMutation() {
        return successful_mutation;
    }

    public void increaseSuccessfulMutation() {
        this.successful_mutation += 1;
    }

    public int getSuccessfulImprovement() {
        return successful_improvement;
    }

    public void increaseSuccessfulImprovement() {
        this.successful_improvement += 1;
    }

    public int getTotalMutation() {
        return total_mutation;
    }

    public void increaseTotalMutation() {
        this.total_mutation += 1;
    }

    public int getTotalImprovement() {
        return total_improvement;
    }

    public void increaseTotalImprovement() {
        this.total_improvement += 1;
    }

    public Result getResult() { return this.result; }
}
