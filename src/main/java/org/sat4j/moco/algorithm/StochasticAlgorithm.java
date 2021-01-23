package org.sat4j.moco.algorithm;

import org.moeaframework.Executor;
import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.algorithm.PeriodicAction;
import org.moeaframework.core.*;
import org.moeaframework.core.spi.OperatorFactory;
import org.sat4j.moco.Params;
import org.sat4j.moco.problem.Instance;
import org.sat4j.moco.util.Clock;
import org.sat4j.moco.util.Log;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.Arrays;

public class StochasticAlgorithm extends MOCOAlgorithm{


    protected Executor exec;

    protected NondominatedPopulation population = null;

    protected String alg_name;

    public StochasticAlgorithm (Instance m, boolean b, boolean b2){

        super(m, b, b2);
        this.exec = new Executor();
        OperatorFactory.getInstance().addProvider(new SmartMutationProvider());
        OperatorFactory.getInstance().addProvider(new SinglePointMutationProvider());
    }

    /**
     * Sets the uniform crossover rate to be used by the algorithm.
     * @param rate The crossover rate.
     */
    public void setCrossoverRate(double rate) {
        this.exec = this.exec.withProperty("ux.rate", rate);
    }

    /**
     * Sets the single value uniform mutation rate to be used by the algorithm.
     * @param rate The mutation rate.
     */
    public void setMutationRate(double rate) {
        this.exec = this.exec.withProperty("bf.rate", rate);
    }

    /**
     * Sets the single value uniform mutation rate to be used by the algorithm.
     * @param rate The mutation rate.
     */
    public void setSmartMutationRate(double rate) {
        this.exec = this.exec.withProperty("sm.rate", rate);
    }

    /**
     * Sets the single value uniform mutation rate to be used by the algorithm.
     * @param rate The mutation rate.
     */
    public void setSmartImprovementRate(double rate) {
        this.exec = this.exec.withProperty("sm.improvement_rate", rate);
    }

    /**
     * Sets the usage of stratification.
     * @param stratify Boolean indicating is stratification is to be used.
     */
    public void setStratify(boolean stratify) {
        this.exec = this.exec.withProperty("sm.stratify", stratify);
    }

    /**
     * Sets the usage of evolutionary smart operators.
     * @param evo Boolean indicating is evolution rate is to be used.
     */
    public void setEvolutionary(boolean evo) {
        this.exec = this.exec.withProperty("sm.evolutionary", evo);
    }

    /**
     * Sets the literal-weight ratio for stratification.
     * @param lwr The literal-weight ratio.
     */
    public void setLWR(double lwr) {
        this.exec = this.exec.withProperty("sm.lwr", lwr);
    }

    /**
     * Sets the population size to be used by the algorithm.
     * @param size The population size.
     */
    public void setPopSize(int size) {
        this.exec = this.exec.withProperty("populationSize", size);
    }

    /**
     * Sets the maximum of conflicts for each smart mutation call.
     * @param conflicts The maximum number of conflicts for the smart mutation.
     */
    public void setMaxConflicts(int conflicts) {
        this.exec = this.exec.withProperty("sm.max_conflicts", conflicts);
    }

    /**
     * Sets the maximum of conflicts for each smart improvement call.
     * @param conflicts The maximum number of conflicts for the smart improvement.
     */
    public void setImproveMaxConflicts(int conflicts) {
        this.exec = this.exec.withProperty("sm.improve_max_conflicts", conflicts);
    }

    /**
     * Sets the seed for the smart operators
     * @param seed Seed value for the PRNG class.
     */
    public void setSeed(int seed) {
        if (seed > 0){
            PRNG.setSeed(seed);
        }
    }

    /**
     * Sets if uniform mutation should be used instead of single point mutation
     * @param um True if uniform mutation is to be used
     */
    public void setUM(boolean um) { this.exec = this.exec.withProperty("um", um); }

    public void setParams(Params params){
        setCrossoverRate(params.getCR());
        setMutationRate(params.getMR());
        setMaxTime(params.getTimeout());
        setPopSize(params.getPS());
        setSmartMutationRate(params.getSMR());
        setSmartImprovementRate(params.getSIR());
        setStratify(params.getStratify());
        setLWR(params.getLWR());
        setMaxConflicts(params.getMC());
        setImproveMaxConflicts(params.getIMC());
        setEvolutionary(params.getEvolutionarySmart());
        setSeed(params.getSeed());
        setUM(params.getUM());
    }

    @Override
    public void solve() {
        Log.comment(0, "applying evolutionary optimization");
        exec.withTerminationCondition(this.result.getTc()).withAlgorithm(alg_name).withProblem(this.getResult().getProblem()).withMaxTime(Clock.instance().getRemaining()*1000L).run();
    }

    public void addSolutionToResult(Solution s){
        this.getResult().addSolution(s);
    }

    protected NondominatedPopulation cleanUpPopulation(NondominatedPopulation pop) {
        NondominatedPopulation new_pop = new NondominatedPopulation();
        for (int i = 0; i < pop.size(); ++i) {
            Solution sol = pop.get(i);
            if (!sol.violatesConstraints()) {
                new_pop.add(sol);
            }
        }
        return new_pop;
    }


    /**
     * Periodic action that collects the current set of feasible solutions in some evolutionary algorithm's
     * population and, optionally, logs them into a file.
     * @author Miguel Terra-Neves
     */
    private class SolutionCollector extends PeriodicAction {

        /**
         * Creates an instance of a solution collector.
         * @param algorithm The evolutionary algorithm to be instrumented with the collector.
         * @param frequency The frequency of solution collection measured in iterations of the evolutionary
         * algorithm.
         */
        public SolutionCollector(Algorithm algorithm, int frequency) {
            super(algorithm, frequency, FrequencyType.STEPS);
        }

        @Override
        public void doAction() {
            NondominatedPopulation pop = getResult();
            pop = cleanUpPopulation(pop);
            //removeSolutions();
            for (Solution s : pop){
                //System.out.println("collector " + s.toString() + " with values " + Arrays.toString(s.getObjectives()));
                addSolutionToResult(s);
            }
            //System.out.println("-------------------------------------------------");
        }

    }

    /**
     * Periodic action that logs the number of feasible solutions in some evolutionary algorithm's
     * population.
     * @author Miguel Terra-Neves
     */
    private static class FeasibleCountLogger extends PeriodicAction {

        /**
         * Stores the number of feasible solutions counted in the last execution of {@link #doAction()}.
         */
        int last_count = -1;

        double last_clock = 0;

        /**
         * Creates an instance of the feasible solution count logger.
         * @param algorithm The evolutionary algorithm to be instrumented with the collector.
         * @param frequency The frequency of solution collection measured in iterations of the evolutionary
         * algorithm.
         */
        public FeasibleCountLogger(Algorithm algorithm, int frequency) {
            super(algorithm, frequency, FrequencyType.STEPS);
        }

        @Override
        public void doAction() {
            NondominatedPopulation pop = getResult();
            int feasible = 0;
            double current_clock = Clock.instance().getElapsed();
            for (int i = 0; i < pop.size(); ++i) {
                if (!pop.get(i).violatesConstraints()) {
                    ++feasible;
                }
            }
            if (last_count != feasible || this.iteration % 250 == 0) {     // prevent excessive logging
                System.out.println("c :iteration " + this.iteration +
                    " :elapsed-time " + current_clock +
                    " :nfeasible " + feasible +
                    " :iteration time " + (current_clock - last_clock));
            }
            this.last_count = feasible;
            last_clock = current_clock;
        }

    }

    public Algorithm decorateWithPeriodicActions(Algorithm alg) {
        return new FeasibleCountLogger(new SolutionCollector(alg, 1), 10);
    }
}
