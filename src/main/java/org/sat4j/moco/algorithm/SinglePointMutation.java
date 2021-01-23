package org.sat4j.moco.algorithm;

import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.operator.real.UM;
import org.moeaframework.core.variable.RealVariable;


public class SinglePointMutation extends UM {

    public SinglePointMutation(double probability) {
        super(probability);
    }

    @Override
    public Solution[] evolve(Solution[] parents) {
        Solution result = parents[0].copy();
        if (PRNG.nextDouble() <= getProbability()) {
            Variable variable = result.getVariable(PRNG.nextInt(result.getNumberOfVariables()));
            if (variable instanceof RealVariable) {
                evolve((RealVariable)variable);
            }
            return new Solution[] { result };
        }
        return parents;
    }

}
