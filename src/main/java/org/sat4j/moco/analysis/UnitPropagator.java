package org.sat4j.moco.analysis;

import org.sat4j.minisat.core.IOrder;
import org.sat4j.minisat.core.LearningStrategy;
import org.sat4j.minisat.core.RestartStrategy;
import org.sat4j.pb.core.PBDataStructureFactory;
import org.sat4j.pb.core.PBSolverResolution;
import org.sat4j.specs.IVecInt;

public class UnitPropagator extends PBSolverResolution {
    public UnitPropagator(LearningStrategy<PBDataStructureFactory> learner, PBDataStructureFactory dsf, IOrder order, RestartStrategy restarter) {
        super(learner, dsf, order, restarter);
    }

    public IVecInt getTrail() {
        return this.trail;
    }


}