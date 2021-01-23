package org.sat4j.moco.algorithm;

import java.util.Properties;

import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.core.*;
import org.moeaframework.core.comparator.ChainedComparator;
import org.moeaframework.core.comparator.CrowdingComparator;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.operator.RandomInitialization;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.spi.AlgorithmFactory;
import org.moeaframework.core.spi.AlgorithmProvider;
import org.moeaframework.core.spi.OperatorFactory;
import org.moeaframework.util.TypedProperties;
import org.sat4j.moco.Params;
import org.sat4j.moco.problem.Instance;

public class NSGAIIFactory extends StochasticAlgorithm {

    public NSGAIIFactory(Instance m, boolean b, boolean b2){
        super(m, b, b2);
        alg_name = "NSGAII";
        AlgorithmFactory.getInstance().addProvider(new NSGAIIProvider());
    }

    /**
     * Provider necessary in order to run the MOEA framework
     */
    private class NSGAIIProvider extends AlgorithmProvider {

        /**
         * If the name of the NSGAII algorithm ("NSGAII") is given as input, produces an instance of it for
         * Virtual Machine Consolidation. Otherwise, returns null.
         * @param name The name of the algorithm.
         * @param properties A set of configuration properties, such as population size ("populationSize"),
         * uniform crossover rate ("ux.crossoverRate") and single value uniform mutation rate ("svum.rate").
         * @param problem The problem instance.
         * @return An instance of the NSGAII algorithm if provided with the name "NSGAII", null otherwise.
         */
        @Override
        public Algorithm getAlgorithm(String name, Properties properties, Problem problem) {
            if (name.equals("NSGAII")) {
                TypedProperties typed_props = new TypedProperties(properties);
                int pop_size = typed_props.getInt("populationSize", 100);
                RandomInitialization initialization = new RandomInitialization(problem, pop_size);
                DominanceComparator comparator = new ParetoDominanceComparator();
                NondominatedSortingPopulation population = new NondominatedSortingPopulation(comparator);
                TournamentSelection selection =
                        new TournamentSelection(2, new ChainedComparator(new ParetoDominanceComparator(),
                                new CrowdingComparator()));
                String vars;
                if (typed_props.getBoolean("um", false)){
                    vars = "ux+um+sm";
                }
                else {
                    vars = "ux+spm+sm";
                }
                Variation variation = OperatorFactory.getInstance().getVariation(vars, properties, problem);
                return decorateWithPeriodicActions(new NSGAII(problem, population, null, selection, variation, initialization));
            }
            return null;
        }

    }

    public void setParams(Params params){
        super.setParams(params);
    }

    public void nsgaiiPrintConfiguration(Params params){
        System.out.println("c  ============ NSGAII Configuration ============" +
                "\nc  Crossover rate:                          " + params.getCR() +
                "\nc  Mutation rate:                           " + params.getMR() +
                "\nc  Uniform mutation set to:                 " + params.getUM() +
                "\nc  Unitary propagation set to               " + params.getUnitaryPropagation() +
                "\nc  Population size:                         " + params.getPS() +
                "\nc  Timeout:                                 " + params.getTimeout() +
                "\nc                                           " +
                "\nc  Smart mutation rate:                     " + params.getSMR() +
                "\nc  Smart mutation max conflicts set to:     " + params.getMC() +
                "\nc  Smart improvement relax rate:            " + params.getSIR() +
                "\nc  Smart improvement max conflicts set to:  " + params.getIMC() +
                "\nc  Evolutionary rate set to                 " + params.getEvolutionarySmart() +
                "\nc  Stratification set to:                   " + params.getStratify() +
                "\nc  LWR set to:                              " + params.getLWR() +
                "\nc                                           " +
                "\nc  Structure improvements set to:           " + params.getStructureImprovements() +
                "\nc  =============================================");
    }
}
