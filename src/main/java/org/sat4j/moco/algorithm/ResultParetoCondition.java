package org.sat4j.moco.algorithm;

import org.moeaframework.core.Algorithm;
import org.moeaframework.core.TerminationCondition;
import org.sat4j.moco.analysis.Result;

public class ResultParetoCondition implements TerminationCondition {
    Result result;
    public ResultParetoCondition(Result r){
        this.result = r;
    }

    @Override
    public void initialize(Algorithm algorithm) {

    }

    @Override
    public boolean shouldTerminate(Algorithm algorithm) {
        return this.result.isParetoFront();
    }
}
