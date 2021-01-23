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
package org.sat4j.moco;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.moco.algorithm.ParetoMCS;
import org.sat4j.moco.analysis.Result;
import org.sat4j.moco.pb.PBExpr;
import org.sat4j.moco.pb.PBFactory;
import org.sat4j.moco.problem.DivObj;
import org.sat4j.moco.problem.LinearObj;
import org.sat4j.moco.problem.Instance;
import org.sat4j.moco.problem.Objective;
import org.sat4j.moco.util.Real;

public class ParetoMCSTest {

    private Instance moco;
    private LinearObj main_obj;
    
    @Before
    public void setUp() {
        this.moco = new Instance();
        this.moco.addConstr(PBFactory.instance().mkGE(new VecInt(new int[] { 1, 2, 3 }), 2));
        this.main_obj = new LinearObj(new VecInt(new int[] { 1, 2 }),
                                      new Vec<Real>(new Real[] { new Real(2), Real.ONE }));
        this.moco.addObj(this.main_obj);
    }
    
    @Test
    public void testSingleObjective() {
        ParetoMCS solver = new ParetoMCS(this.moco);
        solver.solve();
        Result result = solver.getResult();
        assertTrue(result.isParetoFront());
        assertTrue(result.nSolutions() == 1);
        boolean[] solution = result.getAssignment(0);
        assertTrue(solution.length == 3);
        assertFalse(solution[0]);
        assertTrue(solution[1]);
        assertTrue(solution[2]);
        double[] costs = result.getCosts(0);
        assertTrue(costs.length == 1);
        assertTrue(costs[0] == this.main_obj.evaluate(solution).asDouble());
        assertTrue(costs[0] == 1.0);
    }
    
    @Test
    public void testUnsat() {
        this.moco.addConstr(PBFactory.instance().mkLE(new VecInt(new int[] { 1, 2, 3 }), 1));
        ParetoMCS solver = new ParetoMCS(this.moco);
        solver.solve();
        Result result = solver.getResult();
        assertTrue(result.isParetoFront());
        assertTrue(result.nSolutions() == 0);
    }
    
    @Test
    public void testObjectiveWorstCase() {
        this.moco.addConstr(PBFactory.instance().mkClause(new VecInt(new int[] { -3 })));
        ParetoMCS solver = new ParetoMCS(this.moco);
        solver.solve();
        Result result = solver.getResult();
        assertTrue(result.isParetoFront());
        assertTrue(result.nSolutions() == 1);
        boolean[] solution = result.getAssignment(0);
        assertTrue(solution.length == 3);
        assertTrue(solution[0]);
        assertTrue(solution[1]);
        assertFalse(solution[2]);
        double[] costs = result.getCosts(0);
        assertTrue(costs.length == 1);
        assertTrue(costs[0] == this.main_obj.evaluate(solution).asDouble());
        assertTrue(costs[0] == 3.0);
    }
    
    private int getSolMatchIdx(boolean[][] sols, boolean[] sol) {
        int i;
        for (i = 0; i < sols.length; ++i) {
            int j;
            for (j = 0; j < sols[i].length; ++j) {
                if (sols[i][j] != sol[j]) {
                    break;
                }
            }
            if (j == sols[i].length) {
                break;
            }
        }
        return i;
    }
    
    private void validateResult(Result result, Objective[] objs, boolean[][] front_sols, double[][] front_costs) {
        assert(front_costs.length == front_sols.length);
        boolean[] matched = new boolean[front_sols.length];
        for (int i = 0; i < matched.length; ++i) {
            matched[i] = false;
        }
        for (int i = 0; i < result.nSolutions(); ++i) {
            boolean[] solution = result.getAssignment(i);
            assertTrue(solution.length == 3);
            int j = getSolMatchIdx(front_sols, solution);
            assertTrue(j < front_sols.length);
            assertFalse(matched[j]);
            matched[j] = true;
            double[] costs = result.getCosts(i);
            assertTrue(costs.length == objs.length);
            for (int k = 0; k < costs.length; ++k) {
                assertTrue(costs[k] == objs[k].evaluate(solution).asDouble());
                assertTrue(costs[k] == front_costs[j][k]);
            }
        }
    }
    
    @Test
    public void testBiOjective() {
        LinearObj other_obj = new LinearObj(new VecInt(new int[] { -2, 3 }),
                                            new Vec<Real>(new Real[] { new Real(2), new Real(2) }));
        this.moco.addObj(other_obj);
        ParetoMCS solver = new ParetoMCS(this.moco);
        solver.solve();
        Result result = solver.getResult();
        assertTrue(result.isParetoFront());
        assertTrue(result.nSolutions() == 2);
        boolean[][] front_sols = new boolean[][] { new boolean[] { false, true, true },
                                                   new boolean[] { true, true, false } };
        double[][] front_costs = new double[][] { new double[] { 1, 2 }, new double[] { 3, 0 } };
        Objective[] objs = new Objective[] { this.main_obj, other_obj };
        validateResult(result, objs, front_sols, front_costs);
    }
    
    @Test
    public void testTriObjective() {
        LinearObj other_obj1 = new LinearObj(new VecInt(new int[] { -2, 3 }),
                                             new Vec<Real>(new Real[] { new Real(2), new Real(2) }));
        LinearObj other_obj2 = new LinearObj(new VecInt(new int[] { 2, 3 }),
                                             new Vec<Real>(new Real[] { Real.ONE.negate(), Real.ONE.negate() }));
        this.moco.addObj(other_obj1);
        this.moco.addObj(other_obj2);
        ParetoMCS solver = new ParetoMCS(this.moco);
        solver.solve();
        Result result = solver.getResult();
        assertTrue(result.isParetoFront());
        assertTrue(result.nSolutions() == 2);
        boolean[][] front_sols = new boolean[][] { new boolean[] { false, true, true },
                                                   new boolean[] { true, true, false } };
        double[][] front_costs = new double[][] { new double[] { 1, 2, -2 }, new double[] { 3, 0, -1 } };
        Objective[] objs = new Objective[] { this.main_obj, other_obj1, other_obj2 };
        validateResult(result, objs, front_sols, front_costs);
    }
    
    @Test
    public void testManyObjective() {
        LinearObj other_obj1 = new LinearObj(new VecInt(new int[] { -2, 3 }),
                                             new Vec<Real>(new Real[] { new Real(2), new Real(2) }));
        LinearObj other_obj2 = new LinearObj(new VecInt(new int[] { 2, 3 }),
                                             new Vec<Real>(new Real[] { Real.ONE.negate(), Real.ONE.negate() }));
        LinearObj other_obj3 = new LinearObj(new VecInt(new int[] { -1, 3 }),
                                             new Vec<Real>(new Real[] { new Real(2), new Real(2) }));
        LinearObj other_obj4 = new LinearObj(new VecInt(new int[] { 1, 2 }),
                                             new Vec<Real>(new Real[] { Real.ONE.negate(), Real.ONE.negate() }));
        this.moco.addObj(other_obj1);
        this.moco.addObj(other_obj2);
        this.moco.addObj(other_obj3);
        this.moco.addObj(other_obj4);
        ParetoMCS solver = new ParetoMCS(this.moco);
        solver.solve();
        Result result = solver.getResult();
        assertTrue(result.isParetoFront());
        assertTrue(result.nSolutions() == 4);
        boolean[][] front_sols = new boolean[][] { new boolean[] { false, true, true },
                                                   new boolean[] { true, false, true },
                                                   new boolean[] { true, true, false },
                                                   new boolean[] { true, true, true } };
        double[][] front_costs = new double[][] { new double[] { 1, 2, -2, 4, -1 },
                                                  new double[] { 2, 4, -1, 2, -1 },
                                                  new double[] { 3, 0, -1, 0, -2 },
                                                  new double[] { 3, 2, -2, 2, -2 } };
        Objective[] objs = new Objective[] { this.main_obj, other_obj1, other_obj2, other_obj3, other_obj4 };
        validateResult(result, objs, front_sols, front_costs);
    }
    
    @Test
    public void testDivReduction() {
        PBExpr num = new PBExpr(new VecInt(new int[] { -2, 3 }),
                                new Vec<Real>(new Real[] { new Real(2), new Real(2) }));
        PBExpr den = new PBExpr(new VecInt(new int[] { 2, 3 }),
                                new Vec<Real>(new Real[] { Real.ONE, Real.ONE }));
        DivObj other_obj = new DivObj(new Vec<PBExpr>(new PBExpr[] { num }),
                                      new Vec<PBExpr>(new PBExpr[] { den }));
        this.moco.addObj(other_obj);
        ParetoMCS solver = new ParetoMCS(this.moco);
        solver.solve();
        Result result = solver.getResult();
        assertTrue(result.isParetoFront());
        assertTrue(result.nSolutions() == 2);
        boolean[][] front_sols = new boolean[][] { new boolean[] { false, true, true },
                                                   new boolean[] { true, true, false } };
        double[][] front_costs = new double[][] { new double[] { 1, 1 }, new double[] { 3, 0 } };
        Objective[] objs = new Objective[] { this.main_obj, other_obj };
        validateResult(result, objs, front_sols, front_costs);
    }
    
    @Test
    public void testSumOfDivReduction() {
        PBExpr num1 = new PBExpr(new VecInt(new int[] { -2, 3 }),
                                 new Vec<Real>(new Real[] { new Real(2), new Real(2) }));
        PBExpr den1 = new PBExpr(new VecInt(new int[] { 2, 3 }),
                                 new Vec<Real>(new Real[] { Real.ONE, Real.ONE }));
        PBExpr num2 = new PBExpr(new VecInt(new int[] { -1, 3 }),
                                 new Vec<Real>(new Real[] { new Real(2), new Real(2) }));
        PBExpr den2 = new PBExpr(new VecInt(new int[] { 1, 2 }),
                                 new Vec<Real>(new Real[] { Real.ONE, Real.ONE }));
        DivObj other_obj = new DivObj(new Vec<PBExpr>(new PBExpr[] { num1, num2 }),
                                      new Vec<PBExpr>(new PBExpr[] { den1, den2 }));
        this.moco.addObj(other_obj);
        ParetoMCS solver = new ParetoMCS(this.moco);
        solver.solve();
        Result result = solver.getResult();
        assertTrue(result.isParetoFront());
        assertTrue(result.nSolutions() == 2);
        boolean[][] front_sols = new boolean[][] { new boolean[] { false, true, true },
                                                   new boolean[] { true, true, false } };
        double[][] front_costs = new double[][] { new double[] { 1, 5 }, new double[] { 3, 0 } };
        Objective[] objs = new Objective[] { this.main_obj, other_obj };
        validateResult(result, objs, front_sols, front_costs);
    }
    
}
