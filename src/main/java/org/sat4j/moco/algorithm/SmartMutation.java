package org.sat4j.moco.algorithm;

import org.moeaframework.core.*;
import org.sat4j.core.ReadOnlyVec;
import org.sat4j.core.ReadOnlyVecInt;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.moco.analysis.MOCOProblem;
import org.sat4j.moco.mcs.IModelListener;
import org.sat4j.moco.mcs.MCSExtractor;
import org.sat4j.moco.pb.*;
import org.sat4j.moco.problem.Objective;
import org.sat4j.moco.util.Clock;
import org.sat4j.moco.util.Log;
import org.sat4j.moco.util.Real;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SmartMutation implements Variation {

    public static int MAX_EVO_COUNT = 1000;

    private double mutation_probability;

    private double improvement_relax;

    /**
     * Boolean indicating if stratification is to be used.
     */
    private boolean stratify;

    private boolean evolutionary;

    private double evo_counter = 0;
    /**
     * Stratification parameter that controls the literal-weight ratio used in the objective literals
     * partitioning process.
     */
    private double lwr;

    private int max_conflicts = 0;

    private int improve_max_conflicts = 0;

    private MOCOProblem problem;

    /**
     * Stores the PB solver to be used by the Pareto-MCS algorithm.
     */
    private PBSolver solver = null;

    /**
     * Stores the MCS extractor to be used by the Pareto-MCS algorithm.
     */
    private MCSExtractor extractor = null;

    /**
     * Stores each objective's individual literal partition sequence.
     */
    private IVec<IVec<IVecInt>> undef_parts = null;

    private SpecialConstraints special_constraints;

    private int[] improved_model = null;

    private Solution improved_solution = null;


    public SmartMutation (double mutation_probability, boolean evolutionary, Problem problem){
        this.problem = (MOCOProblem) problem;
        special_constraints = this.problem.findSpecialConstraints4SmartImprovement();
        setVars(mutation_probability, evolutionary);
        try {
            this.solver = buildSolver();
        }
        catch (ContradictionException e) {
            Log.comment(3, "Contradiction in SmartMutation");
            return;
        }
        this.extractor = new MCSExtractor(this.solver);
        initUndefFmls();
        this.extractor.setModelListener(new IModelListener() {
            public void onModel(PBSolver s) {
                saveModel(s);
            }
        });
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    public Solution[] evolve(Solution[] solutions) {
        if (PRNG.nextDouble() > this.mutation_probability || (Clock.instance().timedOut())) {
            return solutions;
        }
        //System.out.println(this.mutation_probability);
        //System.out.println("counter = " + this.evo_counter);
        Solution[] new_sols = new Solution[] { solutions[0] };
        boolean[] assignment = this.problem.getAssignment(new_sols[0]);
        Set<Integer> violating_indexes = GetViolatingVariables(assignment);
        if(!violating_indexes.isEmpty()){
            new_sols[0] = ApplySmartMutation(new_sols[0], violating_indexes, assignment);
        }
        else{
            new_sols[0] = ApplySmartImprovement(new_sols[0], assignment);
        }
        Log.comment(3, "Out SmartMutation.evolve");
        return new_sols;
    }

    private void setVars(double prob, boolean evo) {
        this.mutation_probability = prob;
        this.evolutionary = evo;
    }

    /**
     * Increases the probability of applying a smart operator
     */
    public void IncreaseEvoCounter() {
        double MAX_UB = 0.2;
        if(this.evolutionary){
            this.evo_counter ++;
            if (this.evo_counter == MAX_EVO_COUNT){
                this.mutation_probability = Math.min(MAX_UB, this.mutation_probability * 1.05);
                this.evo_counter = 0;
            }
        }
    }

    /**
     * Decreases the probability of applying a smart operator
     */
    public void DecreaseEvoCounter() {
        double MAX_LB = 0.0001;
        if (this.evolutionary) {
            this.evo_counter--;
            if (this.evo_counter == -MAX_EVO_COUNT) {
                this.mutation_probability = Math.max(MAX_LB, this.mutation_probability * 0.95);
                this.evo_counter = 0;
            }
        }
    }

    /**
     * Creates a pseudo-Boolean solver to be used by smart mutation
     * @return Pseudo-Boolean solver
     */
    private PBSolver buildSolver() throws ContradictionException {
        Log.comment(3, "in SmartMutation.buildSolver");
        PBSolver solver = new PBSolver();
        solver.newVars(this.problem.getInitialNumberOfVariables());
        for (int i = 0; i < this.problem.getNumberOfConstraints(); ++i) {
            solver.addConstr(this.problem.getConstr(i));
        }
        Log.comment(3, "out SmartMutation.buildSolver");
        return solver;
    }


    /**------------------------------------------------Smart operators------------------------------------------------*/

    /**
     * Applies smart mutation to an individual
     * @param result The individual to be fixed
     * @param violating_indexes The indexes of the variables that violate the constraint
     * @param assignment The purely Boolean assignment of the individual
     */
    public Solution ApplySmartMutation(Solution result, Set<Integer> violating_indexes, boolean[] assignment){
        if (Clock.instance().timedOut()) { return result; }
        Log.comment(3, "in SmartMutation.ApplySmartMutation");
        double initial_time = Clock.instance().getElapsed();
        int initial_conflits = this.solver.getConflicts();
        if (this.max_conflicts > 0) {
            setMax_conflicts(this.max_conflicts);
            this.solver.setMaxConflicts(this.max_conflicts);
        }
        this.problem.increaseTotalMutation();
        IVecInt assumptions = getMutationAssumptions(violating_indexes, assignment);
        Log.comment(1, "assumptions size on smart mutation: " + assumptions.size());
        Solution new_result = result.copy();
        Log.comment(2, "looking for a model");
        this.solver.check(assumptions);
        int initial_assumptions_size;
        while(this.solver.isSolved() && this.solver.isUnsat() && assumptions.size() != 0) {
            Log.comment(1, "finding an explanation");
            IVecInt expl = this.solver.unsatExplanation();
            if (expl == null || expl.size() == 0) {
                Log.comment(1, "no explanation found on smart mutation");
                Log.comment(3, "out SmartMutation.ApplySmartMutation");
                return result;
            }
            initial_assumptions_size = assumptions.size();
            RemoveCoreFromAssumptions(assumptions, expl);
            Log.comment(1, "assumptions new size: " + assumptions.size());
            if (initial_assumptions_size == assumptions.size()){
                Log.comment(1, "unsat core does not change assumptions");
                Log.comment(3, "out SmartMutation.ApplySmartMutation");
                return result;
            }
            Log.comment(2, "looking for a model");
            this.solver.check(assumptions);
        }

        if ((this.max_conflicts > 0 && this.solver.getConflicts() >= this.max_conflicts + initial_conflits)){
            DecreaseEvoCounter();
            double end_time = Clock.instance().getElapsed();
            Log.comment(1, "smart mutation reached timeout in: " + (end_time - initial_time) + " with " + (this.solver.getConflicts() - initial_conflits) + " conflicts");
            Log.comment(3, "out SmartMutation.ApplySmartMutation");
            return result;
        }

        if (this.solver.isSolved() && this.solver.isUnsat()){
            Log.comment(0, "Problem is UNSAT. Ending Execution on smart mutation.");
            this.problem.getResult().setParetoFrontFound();
            return result;
        }

        if (this.solver.isUnsat()){
            Log.comment(0, "Smart mutation could not solve the problem");
            return result;
        }

        int[] model = null;
        try {
            model = this.solver.model();
        }
        catch(UnsupportedOperationException e){
            Log.comment(0,"UnsupportedOperationException found while getting model on smart mutation");
            System.exit(1);
        }
        try {
            AddBlockClause(model);
        }
        catch(ContradictionException ce){
            Log.comment(0,"ContradictionException found while blocking solution on smart mutation");
            System.exit(1);
        }
        new_result = setNewResult(new_result, model);
        this.problem.evaluate(new_result);
        IncreaseEvoCounter();
        this.problem.increaseSuccessfulMutation();
        double end_time = Clock.instance().getElapsed();
        Log.comment(1, "smart mutation time: " + (end_time - initial_time));
        Log.comment(3, "out SmartMutation.ApplySmartMutation");
        this.problem.getResult().addSolution(new_result);
        return new_result;
    }

    /**
     * Applies smart improvement to an individual
     * @param result The individual to be fixed
     * @param assignment The purely Boolean assignment of the individual
     */
    public Solution ApplySmartImprovement(Solution result, boolean[] assignment){
        Log.comment(3, "in SmartMutation.ApplySmartImprovement");
        double initial_time = Clock.instance().getElapsed();
        if (Clock.instance().timedOut()) { return result; }
        improved_model = null;
        improved_solution = null;
        this.problem.increaseTotalImprovement();
        IVecInt assumptions = getImprovementAssumptions(assignment);
        Log.comment(1, "assumptions size in smart improvement: " + assumptions.size());

        extractor.setImproveMaxConfl(this.improve_max_conflicts);
        IVec<IVecInt> undef_fmls = buildUndefFmls();
        extractor.extract(undef_fmls, assumptions);
        if (improved_model == null){
            DecreaseEvoCounter();
            Log.comment(1, "no individual found");
            this.solver.check();
            if (this.solver.isSolved() && this.solver.isUnsat() && !this.problem.getResult().isParetoFront()) {
                Log.comment(0, "Problem is UNSAT. Ending Execution on smart improvement.");
                this.problem.getResult().setParetoFrontFound();
            }
            Log.comment(3, "out SmartMutation.ApplySmartImprovement");
            return result;
        }
        try {
            solver.addConstr(PBFactory.instance().mkClause(extractor.getMCS()));
        }
        catch (ContradictionException e){
            Log.comment(3, "contradiction adding MCS to formula");
        }
        double end_time = Clock.instance().getElapsed();
        Log.comment(1, "smart Improvement time: " + (end_time - initial_time));
        if (!isWeaklyDominated(improved_solution, result)){
            this.problem.increaseSuccessfulImprovement();
            IncreaseEvoCounter();
            Log.comment(1, "better individual found");
            Log.comment(3, "out SmartMutation.ApplySmartImprovement");
            this.problem.getResult().addSolution(improved_solution);
            return improved_solution;
        }
        DecreaseEvoCounter();
        Log.comment(1, "better individual not found");
        Log.comment(3, "out SmartMutation.ApplySmartImprovement");
        return result;
    }


    /**------------------------------------------------Get Assumptions------------------------------------------------*/

    /**
     * Gets the set of assumptions to be used in smart mutation
     * @param violating_indexes The indexes of the variables that violate the constraint
     * @param assignment The purely Boolean assignment of the individual
     */
    public IVecInt getMutationAssumptions(Set<Integer> violating_indexes, boolean[] assignment){
        Log.comment(2, "looking for assumptions");
        IVecInt assumptions = new VecInt();
        HashSet<Integer> level0 = new HashSet<Integer>();

        for (int i = 1; i <= assignment.length; i++){
            if (!violating_indexes.contains(i)){
                assumptions.push(assignment[i - 1] ? i : -i);
            }
            else if (this.problem.getSI()){
                level0.add(i);
            }
        }
        if (this.problem.getSI()) {
            IVec<IVecInt> freeVars = new Vec<IVecInt>();
            this.problem.getFreeVars().copyTo(freeVars);

            for (int j = 0; j < freeVars.size(); j++) {
                IVecInt freeVarsJ = freeVars.get(j);
                for (int k = 0; k < freeVarsJ.size(); k++) {
                    if (level0.contains(Math.abs(freeVarsJ.get(k)))) {
                        for (int l = 0; l < freeVarsJ.size(); l++) {
                            if (assumptions.contains(freeVarsJ.get(l))) {
                                assumptions.remove(freeVarsJ.get(l));
                            }
                            else if (assumptions.contains(-freeVarsJ.get(l))) {
                                assumptions.remove(-freeVarsJ.get(l));
                            }
                        }
                        break;
                    }
                }
                if (assumptions.size() == 0){
                    break;
                }
            }
        }
        return assumptions;
    }

    /**
     * Applies smart improvement to an individual
     * @param assignment The purely Boolean assignment of the individual
     */
    public IVecInt getImprovementAssumptions(boolean[] assignment){
        Log.comment(2, "looking for assumptions");
        IVecInt assumptions = new VecInt();
        HashSet<Integer> level0 = new HashSet<Integer>();
        IVec<IVecInt> var_list = special_constraints.getVariablesList();

        for (int i = 0; i < var_list.size(); i++){
            if (PRNG.nextDouble() > this.improvement_relax){
                IVecInt lits = var_list.get(i);
                for (int j = 0; j < lits.size(); j++){
                    assumptions.push(assignment[Math.abs(lits.get(j)) - 1] ? Math.abs(lits.get(j)) : -Math.abs(lits.get(j)));
                }
            }
        }
        HashSet<Integer> var_hash = special_constraints.getVariablesHash();
        for (int i = 1; i <= assignment.length; i++){
            if (var_hash.contains(i)){
                continue;
            }
            if (PRNG.nextDouble() > this.improvement_relax){
                assumptions.push(assignment[i - 1] ? i : -i);
            }
            else if (this.problem.getSI()){
                level0.add(i);
            }
        }
        if (this.problem.getSI()) {
            IVec<IVecInt> freeVars = new Vec<IVecInt>();
            this.problem.getFreeVars().copyTo(freeVars);
            for (int j = 0; j < freeVars.size(); j++) {
                IVecInt freeVarsJ = freeVars.get(j);
                for (int k = 0; k < freeVarsJ.size(); k++) {
                    if (level0.contains(Math.abs(freeVarsJ.get(k)))) {
                        for (int l = 0; l < freeVarsJ.size(); l++) {
                            if (assumptions.contains(freeVarsJ.get(l))) {
                                assumptions.remove(freeVarsJ.get(l));
                            }
                            else if (assumptions.contains(-freeVarsJ.get(l))) {
                                assumptions.remove(-freeVarsJ.get(l));
                            }
                        }
                        break;
                    }
                }
                if (assumptions.size() == 0){
                    break;
                }
            }
        }
        return assumptions;
    }

    public static class SpecialConstraints {
        public  IVec<IVecInt> variables_list;
        public HashSet<Integer> variables_hash;

        public SpecialConstraints(IVec<IVecInt> name, HashSet<Integer> object) {
            this.variables_list = name;
            this.variables_hash = object;
        }

        public IVec<IVecInt> getVariablesList(){ return this.variables_list; }

        public HashSet<Integer> getVariablesHash() { return this.variables_hash; }
    }


    /**------------------------------------------------Set result------------------------------------------------*/


    /**
     * Sets the new assignment to an individual
     * @param sol The new individual
     * @param model The purely Boolean assignment found by the solver
     * @return The new individual with the assignment found
     */
    public Solution setNewResult(Solution sol, int[] model){
        Log.comment(3, "in SmartMutation.setNewResult");
        if (this.problem.getSI()){
            setNewResultWithStructureImprov(sol, model);
            Log.comment(3, "out SmartMutation.setNewResult");
            return sol;
        }
        int i = 0;
        for (int index = 0; index < this.problem.getNumberOfVariables(); index++) {
            if (!this.problem.isForcedVar(Math.abs(model[index]))) {
                this.problem.setVariableValue(sol, i, (model[index] > 0 ? 1 : 0));
                i++;
            }
        }
        Log.comment(3, "out SmartMutation.setNewResult");
        return sol;
    }

    /**
     * Sets the new assignment to an individual while using structure improvements
     * @param sol The new individual
     * @param model The purely Boolean assignment found by the solver
     */
    private void setNewResultWithStructureImprov(Solution sol, int[] model){
        int index;
        int normal_variables = sol.getNumberOfVariables() - this.problem.getRemovedConstraints().size();
        for (index = 0; index < this.problem.getRemovedConstraints().size(); index ++) {
                if (this.problem.getConstr(this.problem.getRemovedConstraints().get(index)) instanceof LE ||
                        this.problem.getConstr(this.problem.getRemovedConstraints().get(index)) instanceof GE) {
                    this.problem.setVariableValue(sol, normal_variables + index, 0);
                }
        }
        IVec<IVecInt> freeVars = new Vec<IVecInt>();
        this.problem.getFreeVars().copyTo(freeVars);
        for (index = 0; index < this.problem.getInitialNumberOfVariables(); index ++) {
            int model_value = model[index];
            if ((Math.abs(model_value) - 1) >=  this.problem.getInitialNumberOfVariables()){
                Log.comment(0, "model did not have all the variables");
                break;
            }
            if (this.problem.isForcedVar(Math.abs(model_value))){
                continue;
            }
            int inverse_mapping_index = this.problem.getInverseMappingIndex(Math.abs(model_value) - 1);
            if (inverse_mapping_index != -1) {
                this.problem.setVariableValue(sol, inverse_mapping_index, (model_value > 0 ? 1 : 0));
            }
            else if (model_value > 0){
                outside:
                for (int j = 0; j < freeVars.size(); j++){
                    IVecInt freeVarsJ = freeVars.get(j);
                    for (int i = 0; i < freeVarsJ.size(); i++){
                        if (freeVarsJ.get(i) == model_value) {
                            this.problem.setVariableValue(sol, normal_variables + j,
                                    i + 1);
                            freeVars.set(j, new VecInt(0));
                            break outside;
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds a block clause to the problem
     * @param model The model that will be blocked
     */
    private void AddBlockClause(int[] model) throws ContradictionException {
        IVecInt block_lits = new VecInt();
        //System.out.println("model = ");
        for (int i = 0; i < this.problem.getInitialNumberOfVariables(); i++){
            //System.out.print(model[i] + " ");
            block_lits.push(-model[i]);
        }
        PBConstr block_constr = PBFactory.instance().mkClause(block_lits);
        solver.addConstr(block_constr);
    }

    /**
     * Removes the explanation to why the solver cannot find a feasible assignment from the set of assumptions
     * @param as The set of assumptions
     * @param expl The explanation to why the solver cannot find a feasible assignment
     */
    private void RemoveCoreFromAssumptions(IVecInt as, IVecInt expl) {
        Set<Integer> to_remove = new HashSet<Integer>();
        for (int i = 0; i < expl.size(); i++) {
            to_remove.add(expl.get(i));
        }
        for (int j = 0; j < this.problem.getNumberOfConstraints(); j++){
            for (int k = 0; k < this.problem.getConstr(j).getLits().size(); k++){
                if (expl.contains(Math.abs(this.problem.getConstr(j).getLits().get(k)))){
                    for (int l = 0; l < this.problem.getConstr(j).getLits().size(); l++){
                        to_remove.add(Math.abs(this.problem.getConstr(j).getLits().get(l)));
                    }
                    break;
                }
            }
        }
        for (int i : to_remove) {
            if (as.indexOf(i) != -1){
                as.remove(i);
            }
            else if (as.indexOf(-i) != -1){
                as.remove(-i);
            }
        }
    }

    private boolean isWeaklyDominated(Solution sol, Solution other_sol) {
        for (int i = 0; i < other_sol.getNumberOfObjectives(); ++i) {
            if (sol.getObjective(i) < other_sol.getObjective(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds with there are any variables from the assignment that violate the constraints
     * @return Set of variables that violate the constraints
     */
    public Set<Integer> GetViolatingVariables(boolean[] assignment){
        Log.comment(2, "looking for violating variables");
        Set<Integer> violating_indexes = new HashSet<Integer>();
        ReadOnlyVecInt literals;

        for (int i = 0; i < this.problem.getNumberOfConstraints(); ++i) {
            PBConstr constr = this.problem.getConstr(i);
            Real lhs = constr.getLHS().evaluate(assignment);
            if (constr.violatedBy(lhs)){
                literals = this.problem.getConstr(i).getLHS().getLits();
                for (int j = 0; j < literals.size(); j++){
                    violating_indexes.add(Math.abs(literals.get(j)));
                }
            }
        }
        return violating_indexes;
    }

    /**
     * Saves the model found on smart improvement
     * @param solver the solver that extracts the model
     */
    public void saveModel(PBSolver solver) {
        Solution sol = this.problem.newSolution();
        improved_model = solver.model();
        sol = setNewResult(sol, improved_model);
        this.problem.evaluate(sol);
        if (improved_solution == null){
            improved_solution = sol;
        }
        else {
            if (!isWeaklyDominated(sol, improved_solution)) {
                improved_solution = sol;
            }
        }
    }


    /**
     * Initializes the objective literal partition sequences.
     * If stratification is disabled, a single partition is created with all objective literals for all
     * objective functions.
     * If stratification is enabled, an individual partition sequence is built for each objective function,
     * to later be mixed in during the search process.
     * @see #buildUndefFmls()
     */
    public void initUndefFmls() {
        Log.comment(3, "in SmartMutation.initUndefFmls");
        this.undef_parts = new Vec<IVec<IVecInt>>();
        for (int i = 0; i < this.problem.getNumberOfObjectives(); ++i) {
            Objective o = this.problem.getObj(i);
            if (this.stratify) {
                this.undef_parts.push(partition(o));
            }
            if (this.undef_parts.isEmpty()) {
                this.undef_parts.push(new Vec<IVecInt>());
                this.undef_parts.get(0).push(singlePartition(o));
            }
            else {
                singlePartition(o).copyTo(this.undef_parts.get(0).get(0));
            }
        }
        logPartitions();
        Log.comment(3, "out SmartMutation.initUndefFmls");
    }

    /**
     * Logs the number of partitions for each objective function and partition sizes.
     */
    private void logPartitions() {
        for (int i = 0; i < this.undef_parts.size(); ++i) {
            IVec<IVecInt> obj_parts = this.undef_parts.get(i);
            Log.comment(1, ":obj-idx " + i + " :partitions " + obj_parts.size());
            for (int j = 0; j < obj_parts.size(); ++j) {
                Log.comment(1, ":part-idx " + j + " :part-size " + obj_parts.get(j).size());
            }
        }
    }
    /**
     * Builds a literal partition sequence for a given objective.
     * @param o The objective.
     * @return A partition sequence for objective {@code o}.
     * */

    private IVec<IVecInt> partition(Objective o) {
        IVec<IVecInt> parts = new Vec<IVecInt>();
        IVec<SmartMutation.WeightedLit> w_lits = getWeightedLits(o);
        SmartMutation.WeightedLit[] w_lits_array = new SmartMutation.WeightedLit[w_lits.size()];
        w_lits.copyTo(w_lits_array);
        Arrays.sort(w_lits_array);
        IVecInt part = new VecInt();
        int w_count = 0;
        for (int i = w_lits_array.length-1; i >= 0; --i) {
            if (    i < w_lits_array.length-1 &&
                    !w_lits_array[i].getWeight().equals(w_lits_array[i+1].getWeight()) &&
                    (double)part.size() / w_count > this.lwr) {
                parts.push(part);
                part = new VecInt();
                w_count = 0;
            }
            part.push(-w_lits_array[i].getLit());
            if (w_count == 0 || !w_lits_array[i].getWeight().equals(w_lits_array[i+1].getWeight())) {
                w_count++;
            }
        }
        assert(!part.isEmpty());
        parts.push(part);
        return parts;
    }

    /**
     * Builds a single partition for a given objective containing all of the objective's literals.
     * @param o The objective.
     * @return A partition with all of objective {@code o}'s literals.
     */
    private IVecInt singlePartition(Objective o) {
        IVec<SmartMutation.WeightedLit> w_lits = getWeightedLits(o);
        IVecInt part = new VecInt(w_lits.size());
        for (int i = 0; i < w_lits.size(); ++i) {
            part.unsafePush(-w_lits.get(i).getLit());
        }
        return part;
    }

    /**
     * Retrieves the literals and respective coefficients in an objective function as a vector of weighted
     * literals.
     * @param o The objective.
     * @return The objective's literals and coefficients as weighted literals.
     */
    private IVec<SmartMutation.WeightedLit> getWeightedLits(Objective o) {
        IVec<SmartMutation.WeightedLit> w_lits = new Vec<SmartMutation.WeightedLit>();
        for (int i = 0; i < o.nSubObj(); ++i) {
            ReadOnlyVecInt lits = o.getSubObjLits(i);
            ReadOnlyVec<Real> coeffs = o.getSubObjCoeffs(i);
            for (int j = 0; j < lits.size(); ++j) {
                int lit = lits.get(j);
                Real coeff = coeffs.get(j);
                if (coeff.isPositive()) {
                    w_lits.push(new WeightedLit(lit, coeff));
                }
                else if (coeff.isNegative()) {
                    w_lits.push(new WeightedLit(-lit, coeff.negate()));
                }
                else {
                    Log.comment(2, "0 coefficient ignored");
                }
            }
        }
        return w_lits;
    }

    /**
     * Builds a partition sequence of the literals in the objective functions to be used for stratified
     * MCS extraction.
     * If stratification is disabled, a single partition is returned.
     * @return The objective literals partition sequence.
     */
    public IVec<IVecInt> buildUndefFmls() {
        Log.comment(3, "in SmartMutation.buildUndefFmls");
        IVec<IVecInt> fmls = new Vec<IVecInt>();
        IVec<IVec<IVecInt>> p_stacks = new Vec<IVec<IVecInt>>(this.undef_parts.size());
        for (int i = 0; i < this.undef_parts.size(); ++i) {
            IVec<IVecInt> parts = this.undef_parts.get(i);
            IVec<IVecInt> p_stack = new Vec<IVecInt>(parts.size());
            for (int j = parts.size()-1; j >= 0; --j) {
                p_stack.unsafePush(parts.get(j));
            }
            p_stacks.unsafePush(p_stack);
        }
        while (!p_stacks.isEmpty()) {
            int rand_i = PRNG.nextInt(p_stacks.size());
            IVec<IVecInt> rand_stack = p_stacks.get(rand_i);
            fmls.push(new ReadOnlyVecInt(rand_stack.last()));
            rand_stack.pop();
            if (rand_stack.isEmpty()) {
                p_stacks.set(rand_i, p_stacks.last());
                p_stacks.pop();
            }
        }
        Log.comment(3, "out SmartMutation.buildUndefFmls");
        return fmls;
    }

    /**
     * Representation of weighted literals.
     * Used to store and sort literals based on their coefficients in some objective function.
     * @author Miguel Terra-Neves
     */
    private static class WeightedLit implements Comparable<WeightedLit> {

        /**
         * Stores the literal.
         */
        private int lit;

        /**
         * Stores the weight.
         */
        private Real weight;

        /**
         * Creates an instance of a weighted literal with a given weight.
         * @param l The literal.
         * @param w The weight.
         */
        WeightedLit(int l, Real w) {
            this.lit = l;
            this.weight = w;
        }

        /**
         * Retrieves the literal part of the weighted literal.
         * @return The literal.
         */
        int getLit() { return this.lit; }

        /**
         * Retrieves the weight part of the weighted literal.
         * @return The weight.
         */
        Real getWeight() { return this.weight; }

        /**
         * Compares the weighted literal to another weighted literal.
         * The weighted literal order is entailed by their weights.
         * @param other The other weighted literal.
         * @return An integer smaller than 0 if this literal's weight is smaller than {@code other}'s, 0 if
         * the weight are equal, an integer greater than 0 if this literal's weight is larger than
         * {@code other}'s.
         */
        public int compareTo(WeightedLit other) {
            return getWeight().compareTo(other.getWeight());
        }

    }

    public void setMax_conflicts(int conflicts){
        this.max_conflicts = conflicts;
    }

    public void setImprove_max_conflicts(int conflicts){
        this.improve_max_conflicts = conflicts;
    }

    public void setLWR(double lwr){
        this.lwr = lwr;
    }

    public void setStratify(boolean stratify){
        this.stratify = stratify;
    }

    public void setImprovement_relax(double relax) { this.improvement_relax = relax; }

}
