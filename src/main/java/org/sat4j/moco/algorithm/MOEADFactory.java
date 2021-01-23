package org.sat4j.moco.algorithm;

import java.util.Properties;

import org.moeaframework.algorithm.MOEAD;
import org.moeaframework.core.*;
import org.moeaframework.core.operator.RandomInitialization;
import org.moeaframework.core.spi.AlgorithmFactory;
import org.moeaframework.core.spi.AlgorithmProvider;
import org.moeaframework.core.spi.OperatorFactory;
import org.moeaframework.util.TypedProperties;
import org.sat4j.moco.Params;
import org.sat4j.moco.problem.Instance;

public class MOEADFactory extends StochasticAlgorithm{

    public MOEADFactory(Instance m, boolean b, boolean b2){
        super(m, b, b2);
        alg_name = "MOEAD";
        AlgorithmFactory.getInstance().addProvider(new MOEADFactory.MOEADProvider());
    }

    private class MOEADProvider extends AlgorithmProvider {

        /**
         * If the name of the MOEAD algorithm ("MOEAD") is given as input, produces an instance of it for
         * Virtual Machine Consolidation. Otherwise, returns null.
         * @param name The name of the algorithm.
         * @param properties A set of configuration properties, such as population size ("populationSize"),
         * uniform crossover rate ("ux.crossoverRate"), single value uniform mutation rate ("svum.rate"),
         * maximum portion of the population that can be replaced by a new solution ("eta"), neighborhood
         * size ("neighborhoodSize") and probability of performing crossover within the neighborhood of a
         * solution ("delta").
         * @param problem The problem instance.
         * @return An instance of the MOEAD algorithm if provided with the name "MOEAD", null otherwise.
         */
        @Override
        public Algorithm getAlgorithm(String name, Properties properties, Problem problem) {
            if (name.equals("MOEAD")) {
                TypedProperties typed_props = new TypedProperties(properties);
                int pop_size =
                        typed_props.getInt("populationSize", Math.max(100, problem.getNumberOfObjectives()));
                RandomInitialization initialization = new RandomInitialization(problem, pop_size);
                String vars;
                if (typed_props.getBoolean("um", false)){
                    vars = "ux+um+sm";
                }
                else {
                    vars = "ux+spm+sm";
                }
                Variation variation = OperatorFactory.getInstance().getVariation(vars, properties, problem);
                int neighbordhoodSize = (int)(typed_props.getDouble("neighborhoodSize", 0.1) * pop_size);
                neighbordhoodSize = Math.max(2, Math.min(pop_size, neighbordhoodSize));
                int eta = Math.max(2, (int)(typed_props.getDouble("eta", 0.01) * pop_size));
                double delta = typed_props.getDouble("delta", 0.9);
                return decorateWithPeriodicActions(new MOEAD(problem, neighbordhoodSize, initialization, variation, delta, eta));
            }
            return null;
        }

    }

    public void setNeighborhoodSize(double ns) {
        this.exec = this.exec.withProperty("neighbordhoodSize", ns);
    }

    public void setEta(double eta) {
        this.exec = this.exec.withProperty("eta", eta);
    }

    public void setDelta(double delta) {
        this.exec = this.exec.withProperty("delta", delta);
    }

    public void setParams(Params params){
        super.setParams(params);
        setNeighborhoodSize(params.getNS());
        setEta(params.getEta());
        setDelta(params.getDelta());
    }

    public void moeadPrintConfiguration(Params params){
        System.out.println("c  ============ MOEAD Configuration ============" +
                "\nc  Crossover rate:                          " + params.getCR() +
                "\nc  Mutation rate:                           " + params.getMR() +
                "\nc  Uniform mutation set to:                 " + params.getUM() +
                "\nc  Unitary propagation set to               " + params.getUnitaryPropagation() +
                "\nc  Population size:                         " + params.getPS() +
                "\nc  Neighborhood size:                       " + params.getNS() +
                "\nc  ETA:                                     " + params.getEta() +
                "\nc  Delta:                                   " + params.getDelta() +
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
