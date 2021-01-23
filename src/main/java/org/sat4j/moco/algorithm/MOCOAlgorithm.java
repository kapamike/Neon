package org.sat4j.moco.algorithm;

import org.sat4j.moco.analysis.Result;
import org.sat4j.moco.problem.Instance;

public abstract class MOCOAlgorithm {

    /**
     * Default max time.
     */

    private static final int NO_TIME = 0;

    protected int max_time = NO_TIME;

    /**
     * An instance of a MOCO problem to be solved.
     */
    protected Instance problem;

    /**
     * Stores the result (e.g. nondominated solutions) of the execution of the Pareto-MCS algorithm.
     */
    protected Result result;

    public MOCOAlgorithm(Instance m, boolean b, boolean b2) {
        this.problem = m;
        this.result = new Result(m, b, b2);
    }

    public Instance getProblem(){
        return this.problem;
    }

    /**
     * Retrieves the result of the last call to {@link #solve()}.
     * @return The result.
     */
    public Result getResult(){
        return this.result;
    }

    abstract void solve();

    public void setMaxTime(int time) {
        this.max_time = time;
    }
}
