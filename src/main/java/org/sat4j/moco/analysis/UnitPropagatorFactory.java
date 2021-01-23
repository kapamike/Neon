package org.sat4j.moco.analysis;

import org.sat4j.core.ASolverFactory;
import org.sat4j.minisat.learning.MiniSATLearning;
import org.sat4j.minisat.orders.RSATPhaseSelectionStrategy;
import org.sat4j.minisat.restarts.ArminRestarts;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.constraints.CompetResolutionPBLongMixedWLClauseCardConstrDataStructure;
import org.sat4j.pb.core.PBDataStructureFactory;
import org.sat4j.pb.orders.VarOrderHeapObjective;

public class UnitPropagatorFactory extends ASolverFactory<IPBSolver> {

    /**
     * The single instance of the unitary propagator factory.
     */
    private static final UnitPropagatorFactory instance = new UnitPropagatorFactory();

    /**
     * Retrieves the singleton unitary propagator factory instance.
     * @return The unitary propagator factory.
     */
    public static UnitPropagatorFactory instance() { return instance; }

    private UnitPropagatorFactory() {}

    @Override
    public UnitPropagator defaultSolver() {
        MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning();
        UnitPropagator solver = new UnitPropagator(learning, new CompetResolutionPBLongMixedWLClauseCardConstrDataStructure(), new VarOrderHeapObjective(new RSATPhaseSelectionStrategy()), new ArminRestarts());
        learning.setDataStructureFactory(solver.getDSFactory());
        learning.setVarActivityListener(solver);
        solver.setSimplifier(solver.EXPENSIVE_SIMPLIFICATION);
        return solver;
    }

    @Override
    public IPBSolver lightSolver() {
        return null;
    }
}
